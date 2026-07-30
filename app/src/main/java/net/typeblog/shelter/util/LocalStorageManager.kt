package net.typeblog.shelter.util

import android.content.Context
import android.content.SharedPreferences
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

class LocalStorageManager private constructor(context: Context) {
    private val appContext: Context = context.applicationContext
    private var prefs: SharedPreferences = prefs()

    @Suppress("DEPRECATION")
    private fun prefs(): SharedPreferences =
        appContext.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE or Context.MODE_MULTI_PROCESS,
        )

    fun remove(pref: String) {
        prefs.edit().remove(pref).apply()
    }

    fun contains(pref: String): Boolean = prefs.contains(pref)

    fun getBoolean(pref: String): Boolean = prefs.getBoolean(pref, false)

    fun getBoolean(pref: String, defaultValue: Boolean): Boolean =
        prefs.getBoolean(pref, defaultValue)

    fun setBoolean(pref: String, value: Boolean) {
        prefs.edit().putBoolean(pref, value).apply()
    }

    fun getInt(pref: String): Int = prefs.getInt(pref, Int.MIN_VALUE)

    fun setInt(pref: String, value: Int) {
        prefs.edit().putInt(pref, value).apply()
    }

    fun getLong(pref: String, defaultValue: Long = 0L): Long =
        prefs.getLong(pref, defaultValue)

    fun setLong(pref: String, value: Long) {
        prefs.edit().putLong(pref, value).apply()
    }

    fun getString(pref: String): String? = prefs.getString(pref, null)

    fun setString(pref: String, value: String) {
        prefs.edit().putString(pref, value).apply()
    }

    fun getStringList(pref: String): Array<String> = if (isProtectedAutoFreezeList(pref)) {
        readProtectedAutoFreezeList(prefs, repair = true)
    } else {
        decodeStringList(prefs.getString(pref, ""))
    }

    /** Re-read from disk (needed for the {@code :vpnwatch} process). */
    fun getStringListFresh(pref: String): Array<String> {
        prefs = prefs()
        return if (isProtectedAutoFreezeList(pref)) {
            readProtectedAutoFreezeList(prefs, repair = true)
        } else {
            decodeStringList(prefs.getString(pref, ""))
        }
    }

    fun getBooleanFresh(pref: String, defaultValue: Boolean): Boolean =
        prefs().getBoolean(pref, defaultValue)

    fun setStringList(pref: String, list: Array<String>) {
        val value = Utility.stringJoin(LIST_DIVIDER, list)
        if (isProtectedAutoFreezeList(pref)) {
            writeProtectedAutoFreezeList(prefs, value)
            prefs = prefs()
            return
        }
        val editor = prefs.edit().putString(pref, value)
        editor.commit()
        prefs = prefs()
    }

    fun stringListContains(pref: String, item: String): Boolean =
        getStringList(pref).indexOf(item) >= 0

    fun appendStringList(pref: String, newItem: String) {
        if (isProtectedAutoFreezeList(pref)) {
            val list = ArrayList(getStringList(pref).toList())
            if (!list.contains(newItem)) list.add(newItem)
            setStringList(pref, list.toTypedArray())
            return
        }
        var str = prefs.getString(pref, null)
        str = if (str == null) {
            newItem
        } else {
            str + LIST_DIVIDER + newItem
        }
        prefs.edit().putString(pref, str).commit()
        prefs = prefs()
    }

    fun backupAutoFreezeListIfPresent() {
        val list = getStringList(PREF_AUTO_FREEZE_LIST_WORK_PROFILE)
        if (list.isNotEmpty()) {
            setStringList(PREF_AUTO_FREEZE_LIST_WORK_PROFILE, list)
        }
    }

    fun removeFromStringList(pref: String, item: String) {
        val list = ArrayList(getStringList(pref).toList())
        list.removeIf { it == item }
        setStringList(pref, list.toTypedArray())
    }

    companion object {
        const val PREF_IS_SETTING_UP = "is_setting_up"
        const val PREF_HAS_SETUP = "has_setup"
        const val PREF_AUTO_FREEZE_LIST_WORK_PROFILE = "auto_freeze_list_work_profile"
        const val PREF_AUTO_FREEZE_LIST_WORK_PROFILE_BACKUP =
            "auto_freeze_list_work_profile_backup"
        private const val PREF_AUTO_FREEZE_LIST_WORK_PROFILE_GENERATION =
            "auto_freeze_list_work_profile_generation"
        private const val PREF_AUTO_FREEZE_LIST_WORK_PROFILE_CHECKSUM =
            "auto_freeze_list_work_profile_checksum"
        private const val PREF_AUTO_FREEZE_LIST_WORK_PROFILE_BACKUP_GENERATION =
            "auto_freeze_list_work_profile_backup_generation"
        private const val PREF_AUTO_FREEZE_LIST_WORK_PROFILE_BACKUP_CHECKSUM =
            "auto_freeze_list_work_profile_backup_checksum"
        private const val PREF_AUTO_FREEZE_LIST_FORMAT = "auto_freeze_list_format"
        private const val AUTO_FREEZE_LIST_FORMAT_VERSION = 1
        const val PREF_CROSS_PROFILE_FILE_CHOOSER = "cross_profile_file_chooser"
        const val PREF_AUTH_KEY = "auth_key"
        const val PREF_AUTO_FREEZE_SERVICE = "auto_freeze_service"
        const val PREF_DONT_FREEZE_FOREGROUND = "dont_freeze_foreground"
        const val PREF_AUTO_FREEZE_DELAY = "auto_freeze_delay"
        const val PREF_BLOCK_CONTACTS_SEARCHING = "block_contacts_searching"
        const val PREF_PAYMENT_STUB = "payment_stub"
        const val PREF_ANTI_SPY_BOOT_FREEZE_PENDING = "anti_spy_boot_freeze_pending"
        const val PREF_ANTI_SPY_LAUNCH_VERSION_CODE = "anti_spy_launch_version_code"
        const val PREF_UNFREEZE_SHORTCUT_REGISTRY = "unfreeze_shortcut_registry"
        const val PREF_LEGACY_FROZEN_MIGRATION_DONE = "legacy_frozen_migration_done"
        /** Last seen work-profile package set; used to detect store installs between sessions. */
        const val PREF_KNOWN_WORK_PROFILE_PACKAGES = "known_work_profile_packages"
        /** User removed auto-freeze; do not re-assign until they enable it in the menu. */
        const val PREF_AUTO_FREEZE_OPT_OUT_WORK_PROFILE = "auto_freeze_opt_out_work_profile"
        /** Store installs waiting for cross-profile write to the auto-freeze list. */
        const val PREF_PENDING_STORE_AUTO_FREEZE = "pending_store_auto_freeze"
        /** Last batch-freeze summary (VPN or manual) for diagnostics. */
        const val PREF_LAST_BATCH_FREEZE_AT = "last_batch_freeze_at"
        const val PREF_LAST_BATCH_FREEZE_NEWLY = "last_batch_freeze_newly"
        const val PREF_LAST_BATCH_FREEZE_STILL_VISIBLE = "last_batch_freeze_still_visible"
        const val PREF_LAST_BATCH_FREEZE_STILL_VISIBLE_PKGS = "last_batch_freeze_still_visible_pkgs"
        /** `:vpnwatch` poll heartbeat (main profile, written in main user). */
        const val PREF_VPN_WATCH_HEARTBEAT_MAIN = "vpn_watch_heartbeat_main"
        const val PREF_VPN_WATCH_VPN_MAIN = "vpn_watch_vpn_main"
        /** Work `:vpnwatch` heartbeat mirrored to main profile for Settings. */
        const val PREF_VPN_WATCH_HEARTBEAT_WORK_MIRROR = "vpn_watch_heartbeat_work_mirror"
        const val PREF_VPN_WATCH_VPN_WORK_MIRROR = "vpn_watch_vpn_work_mirror"
        /** Written in work profile user (local diagnostics). */
        const val PREF_VPN_WATCH_HEARTBEAT_WORK = "vpn_watch_heartbeat_work"
        const val PREF_VPN_WATCH_VPN_WORK = "vpn_watch_vpn_work"

        private const val LIST_DIVIDER = ","
        private const val PREFS_NAME = "prefs"

        private var instance: LocalStorageManager? = null

        fun initialize(context: Context) {
            instance = LocalStorageManager(context)
        }

        fun getInstance(): LocalStorageManager {
            return instance
                ?: throw IllegalStateException("LocalStorageManager must be initialized at start-up")
        }

        @Suppress("DEPRECATION")
        fun readStringListFresh(context: Context, pref: String): Array<String> {
            val sharedPreferences = context.applicationContext.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE or Context.MODE_MULTI_PROCESS,
            )
            return if (isProtectedAutoFreezeList(pref)) {
                readProtectedAutoFreezeList(sharedPreferences, repair = true)
            } else {
                decodeStringList(sharedPreferences.getString(pref, ""))
            }
        }

        fun readBooleanFresh(context: Context, pref: String, defaultValue: Boolean): Boolean =
            context.applicationContext
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(pref, defaultValue)

        private data class AutoFreezeSnapshot(
            val value: String,
            val generation: Long,
        )

        private fun isProtectedAutoFreezeList(pref: String): Boolean =
            pref == PREF_AUTO_FREEZE_LIST_WORK_PROFILE

        private fun decodeStringList(value: String?): Array<String> =
            value.orEmpty()
                .split(LIST_DIVIDER)
                .filter { it.isNotEmpty() }
                .toTypedArray()

        private fun readProtectedAutoFreezeList(
            sharedPreferences: SharedPreferences,
            repair: Boolean,
        ): Array<String> {
            if (sharedPreferences.getInt(PREF_AUTO_FREEZE_LIST_FORMAT, 0) !=
                AUTO_FREEZE_LIST_FORMAT_VERSION
            ) {
                val legacyValue = sharedPreferences.getString(
                    PREF_AUTO_FREEZE_LIST_WORK_PROFILE,
                    "",
                ).orEmpty()
                writeProtectedAutoFreezeList(sharedPreferences, legacyValue)
                return decodeStringList(legacyValue)
            }

            val primary = readSnapshot(
                sharedPreferences,
                PREF_AUTO_FREEZE_LIST_WORK_PROFILE,
                PREF_AUTO_FREEZE_LIST_WORK_PROFILE_GENERATION,
                PREF_AUTO_FREEZE_LIST_WORK_PROFILE_CHECKSUM,
            )
            val backup = readSnapshot(
                sharedPreferences,
                PREF_AUTO_FREEZE_LIST_WORK_PROFILE_BACKUP,
                PREF_AUTO_FREEZE_LIST_WORK_PROFILE_BACKUP_GENERATION,
                PREF_AUTO_FREEZE_LIST_WORK_PROFILE_BACKUP_CHECKSUM,
            )
            val selected = when {
                primary == null -> backup
                backup == null -> primary
                backup.generation > primary.generation -> backup
                else -> primary
            } ?: AutoFreezeSnapshot("", 0L)

            if (repair && primary != selected) {
                sharedPreferences.edit()
                    .putString(PREF_AUTO_FREEZE_LIST_WORK_PROFILE, selected.value)
                    .putLong(PREF_AUTO_FREEZE_LIST_WORK_PROFILE_GENERATION, selected.generation)
                    .putString(
                        PREF_AUTO_FREEZE_LIST_WORK_PROFILE_CHECKSUM,
                        checksum(selected.value),
                    )
                    .commit()
            }
            return decodeStringList(selected.value)
        }

        private fun readSnapshot(
            sharedPreferences: SharedPreferences,
            valueKey: String,
            generationKey: String,
            checksumKey: String,
        ): AutoFreezeSnapshot? {
            val value = sharedPreferences.getString(valueKey, null) ?: return null
            val generation = sharedPreferences.getLong(generationKey, Long.MIN_VALUE)
            if (generation == Long.MIN_VALUE) return null
            val storedChecksum = sharedPreferences.getString(checksumKey, null) ?: return null
            if (!MessageDigest.isEqual(
                    checksum(value).toByteArray(StandardCharsets.US_ASCII),
                    storedChecksum.toByteArray(StandardCharsets.US_ASCII),
                )
            ) {
                return null
            }
            return AutoFreezeSnapshot(value, generation)
        }

        private fun writeProtectedAutoFreezeList(
            sharedPreferences: SharedPreferences,
            value: String,
        ) {
            val generation = maxOf(
                sharedPreferences.getLong(PREF_AUTO_FREEZE_LIST_WORK_PROFILE_GENERATION, 0L),
                sharedPreferences.getLong(
                    PREF_AUTO_FREEZE_LIST_WORK_PROFILE_BACKUP_GENERATION,
                    0L,
                ),
            ) + 1L
            val valueChecksum = checksum(value)
            val editor = sharedPreferences.edit()
                .putInt(PREF_AUTO_FREEZE_LIST_FORMAT, AUTO_FREEZE_LIST_FORMAT_VERSION)
                .putString(PREF_AUTO_FREEZE_LIST_WORK_PROFILE, value)
                .putLong(PREF_AUTO_FREEZE_LIST_WORK_PROFILE_GENERATION, generation)
                .putString(PREF_AUTO_FREEZE_LIST_WORK_PROFILE_CHECKSUM, valueChecksum)
            if (value.isNotEmpty()) {
                editor.putString(PREF_AUTO_FREEZE_LIST_WORK_PROFILE_BACKUP, value)
                    .putLong(PREF_AUTO_FREEZE_LIST_WORK_PROFILE_BACKUP_GENERATION, generation)
                    .putString(PREF_AUTO_FREEZE_LIST_WORK_PROFILE_BACKUP_CHECKSUM, valueChecksum)
            }
            editor.commit()
        }

        private fun checksum(value: String): String =
            MessageDigest.getInstance("SHA-256")
                .digest(value.toByteArray(StandardCharsets.UTF_8))
                .joinToString("") { "%02x".format(it) }
    }
}
