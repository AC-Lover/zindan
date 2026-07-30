package net.typeblog.shelter.util

import android.content.Intent
import java.nio.charset.StandardCharsets
import java.security.InvalidKeyException
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

// Opening access to actions across the profile boundary poses a security risk
// The risk is that other applications might also be able to start our activities
// through system's IntentForwarderActivity
// That activity runs in the system process, thus normal limitations like "permissions"
// and "exported" will not work.
// This class appends a timestamp, one-time nonce and a signature bound to the action
// and application extras. The component is intentionally excluded because Android's
// cross-profile forwarder rewrites it while routing the Intent.
// to our own Intents sent through the boundary, ensuring that only Shelter can invoke
// its high-privilege functions across that boundary, assuming that no other application
// would be able to access Shelter's internal storage to gain access to the private key.
// The private key is generated the first time this class is used, and then shared
// across the profile boundary. Shelter will always trust the first key it receives.
object AuthenticationUtility {
    private const val AUTH_PROTOCOL_VERSION = 2
    private const val MAX_INTENT_AGE_MS = 30_000L
    private const val MAX_FUTURE_SKEW_MS = 5_000L
    private const val NONCE_BYTES = 16
    private const val MAX_REMEMBERED_NONCES = 128

    private const val EXTRA_AUTH_KEY = "auth_key"
    private const val EXTRA_AUTH_VERSION = "auth_version"
    private const val EXTRA_AUTH_TIMESTAMP = "timestamp"
    private const val EXTRA_AUTH_NONCE = "auth_nonce"
    private const val EXTRA_AUTH_SIGNATURE = "signature"

    private val secureRandom = SecureRandom()
    private val acceptedNonces = LinkedHashMap<String, Long>()

    fun getOrCreateKey(): String {
        var key = LocalStorageManager.getInstance().getString(LocalStorageManager.PREF_AUTH_KEY)
        if (key == null || !isValidKey(key)) {
            key = generateKey()
            LocalStorageManager.getInstance().setString(LocalStorageManager.PREF_AUTH_KEY, key)
        }
        return key
    }

    fun replaceKey(key: String) {
        if (!isValidKey(key)) return
        LocalStorageManager.getInstance().setString(LocalStorageManager.PREF_AUTH_KEY, key)
    }

    fun signIntent(intent: Intent) {
        val key = LocalStorageManager.getInstance().getString(LocalStorageManager.PREF_AUTH_KEY)
        if (key == null) {
            intent.putExtra(EXTRA_AUTH_KEY, getOrCreateKey())
        } else {
            val timestamp = System.currentTimeMillis()
            val nonce = ByteArray(NONCE_BYTES).also(secureRandom::nextBytes).let(::bytesToHex)
            intent.putExtra(EXTRA_AUTH_VERSION, AUTH_PROTOCOL_VERSION)
            intent.putExtra(EXTRA_AUTH_TIMESTAMP, timestamp)
            intent.putExtra(EXTRA_AUTH_NONCE, nonce)
            intent.putExtra(
                EXTRA_AUTH_SIGNATURE,
                sign(key, signaturePayload(intent, timestamp, nonce))
            )
        }
    }

    fun checkIntent(intent: Intent): Boolean {
        val key = LocalStorageManager.getInstance().getString(LocalStorageManager.PREF_AUTH_KEY)
        if (key == null) {
            return if (intent.hasExtra(EXTRA_AUTH_KEY)) {
                val authKey = intent.getStringExtra(EXTRA_AUTH_KEY) ?: return false
                if (!isValidKey(authKey)) return false
                LocalStorageManager.getInstance().setString(
                    LocalStorageManager.PREF_AUTH_KEY,
                    authKey
                )
                true
            } else {
                false
            }
        } else {
            if (intent.getIntExtra(EXTRA_AUTH_VERSION, 0) != AUTH_PROTOCOL_VERSION) return false

            val now = System.currentTimeMillis()
            val timestamp = intent.getLongExtra(EXTRA_AUTH_TIMESTAMP, 0L)
            val age = now - timestamp
            if (age !in -MAX_FUTURE_SKEW_MS..MAX_INTENT_AGE_MS) return false

            val nonce = intent.getStringExtra(EXTRA_AUTH_NONCE) ?: return false
            if (!isValidNonce(nonce) || wasNonceAccepted(nonce, now)) return false

            val suppliedSignature = intent.getStringExtra(EXTRA_AUTH_SIGNATURE) ?: return false
            val expectedSignature = sign(key, signaturePayload(intent, timestamp, nonce))
            if (!constantTimeEquals(expectedSignature, suppliedSignature)) return false

            rememberNonce(nonce, now)
            return true
        }
    }

    fun reset() {
        LocalStorageManager.getInstance().remove(LocalStorageManager.PREF_AUTH_KEY)
    }

    private fun signaturePayload(intent: Intent, timestamp: Long, nonce: String): ByteArray {
        val action = intent.action.orEmpty()
        val extras = canonicalExtras(intent)
        return listOf(
            AUTH_PROTOCOL_VERSION.toString(),
            action.length.toString(),
            action,
            extras.length.toString(),
            extras,
            timestamp.toString(),
            nonce,
        ).joinToString("\n").toByteArray(StandardCharsets.UTF_8)
    }

    private fun canonicalExtras(intent: Intent): String {
        val extras = intent.extras ?: return ""
        return extras.keySet()
            .asSequence()
            .filterNot { it in AUTH_EXTRA_NAMES }
            .sorted()
            .map { key ->
                val value = canonicalValue(extras.get(key))
                "${key.length}:$key:${value.length}:$value"
            }
            .joinToString("|")
    }

    private fun canonicalValue(value: Any?): String = when (value) {
        null -> "null:"
        is String -> "string:${value.length}:$value"
        is CharSequence -> value.toString().let { "chars:${it.length}:$it" }
        is Boolean -> "boolean:$value"
        is Byte -> "byte:$value"
        is Short -> "short:$value"
        is Int -> "int:$value"
        is Long -> "long:$value"
        is Float -> "float:${value.toRawBits()}"
        is Double -> "double:${value.toRawBits()}"
        is Char -> "char:${value.code}"
        is ByteArray -> "bytes:${bytesToHex(value)}"
        is IntArray -> "ints:${value.joinToString(",")}"
        is LongArray -> "longs:${value.joinToString(",")}"
        is BooleanArray -> "booleans:${value.joinToString(",")}"
        is Array<*> -> "array:${value.joinToString("|") { canonicalValue(it) }}"
        is ArrayList<*> -> "list:${value.joinToString("|") { canonicalValue(it) }}"
        // Binder-bearing bundles cannot be serialized reproducibly across profiles.
        else -> "opaque:${value.javaClass.name}"
    }

    private fun sign(hexKey: String, payload: ByteArray): String {
        try {
            val keySpec = SecretKeySpec(hexStringToByteArray(hexKey), "HmacSHA256")
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(keySpec)
            return bytesToHex(mac.doFinal(payload))
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException("WTF?")
        } catch (e: InvalidKeyException) {
            throw RuntimeException("WTF?")
        }
    }

    private fun generateKey(): String {
        return try {
            val keyGen = KeyGenerator.getInstance("HmacSHA256")
            keyGen.init(256)
            bytesToHex(keyGen.generateKey().encoded)
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException("WTF?")
        }
    }

    private fun isValidKey(value: String): Boolean =
        value.length == 64 && value.all { Character.digit(it, 16) >= 0 }

    private fun isValidNonce(value: String): Boolean =
        value.length == NONCE_BYTES * 2 && value.all { Character.digit(it, 16) >= 0 }

    private fun constantTimeEquals(expectedHex: String, suppliedHex: String): Boolean {
        val expected = hexStringToByteArray(expectedHex)
        val supplied = hexStringToByteArrayOrNull(suppliedHex) ?: return false
        return MessageDigest.isEqual(expected, supplied)
    }

    @Synchronized
    private fun wasNonceAccepted(nonce: String, now: Long): Boolean {
        pruneNonces(now)
        return acceptedNonces.containsKey(nonce)
    }

    @Synchronized
    private fun rememberNonce(nonce: String, now: Long) {
        pruneNonces(now)
        acceptedNonces[nonce] = now
        while (acceptedNonces.size > MAX_REMEMBERED_NONCES) {
            acceptedNonces.remove(acceptedNonces.keys.first())
        }
    }

    private fun pruneNonces(now: Long) {
        val iterator = acceptedNonces.entries.iterator()
        while (iterator.hasNext()) {
            if (now - iterator.next().value > MAX_INTENT_AGE_MS) iterator.remove()
        }
    }

    private val AUTH_EXTRA_NAMES = setOf(
        EXTRA_AUTH_KEY,
        EXTRA_AUTH_VERSION,
        EXTRA_AUTH_TIMESTAMP,
        EXTRA_AUTH_NONCE,
        EXTRA_AUTH_SIGNATURE,
    )

    private val hexArray = "0123456789ABCDEF".toCharArray()

    private fun bytesToHex(bytes: ByteArray): String {
        val hexChars = CharArray(bytes.size * 2)
        for (j in bytes.indices) {
            val v = bytes[j].toInt() and 0xFF
            hexChars[j * 2] = hexArray[v ushr 4]
            hexChars[j * 2 + 1] = hexArray[v and 0x0F]
        }
        return String(hexChars)
    }

    private fun hexStringToByteArray(s: String): ByteArray =
        hexStringToByteArrayOrNull(s) ?: throw IllegalArgumentException("Invalid hexadecimal value")

    private fun hexStringToByteArrayOrNull(s: String): ByteArray? {
        if (s.isEmpty() || s.length % 2 != 0) return null
        return ByteArray(s.length / 2) { i ->
            val high = Character.digit(s[i * 2], 16)
            val low = Character.digit(s[i * 2 + 1], 16)
            if (high < 0 || low < 0) return null
            ((high shl 4) + low).toByte()
        }
    }
}
