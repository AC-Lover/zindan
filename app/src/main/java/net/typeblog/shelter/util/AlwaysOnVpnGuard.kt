package net.typeblog.shelter.util

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import androidx.appcompat.app.AlertDialog
import net.typeblog.shelter.R
import net.typeblog.shelter.receivers.ShelterDeviceAdminReceiver

/**
 * Detects Always-on / lockdown VPN using public APIs only.
 *
 * Android only exposes the configured package to device/profile owners. In the personal profile
 * Zindan usually is not an owner, so we fall back to a user-facing manual check instead of using
 * hidden SettingsProvider keys or shell-only APIs.
 */
object AlwaysOnVpnGuard {
    private const val TAG = "AlwaysOnVpnGuard"
    private const val PROMPT_COOLDOWN_MS = 30 * 60 * 1000L

    enum class Action {
        LAUNCH_APP,
        INSTALL_APK,
        VPN_PERMISSION,
        VPN_MONITOR,
        STATE_CHANGED,
    }

    enum class State {
        DISABLED,
        ENABLED,
        UNKNOWN,
    }

    data class Status(
        val state: State,
        val packageName: String? = null,
        val lockdown: Boolean = false,
        val reason: String? = null,
    ) {
        val signature: String =
            "${state.name}:${packageName.orEmpty()}:$lockdown:${reason.orEmpty()}"
    }

    fun inspect(context: Context): Status {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return Status(State.UNKNOWN, reason = "api_below_24")
        }
        val app = context.applicationContext
        val dpm = app.getSystemService(DevicePolicyManager::class.java)
            ?: return Status(State.UNKNOWN, reason = "no_dpm")
        val admin = ComponentName(app, ShelterDeviceAdminReceiver::class.java)
        val isOwner = try {
            dpm.isProfileOwnerApp(app.packageName) || dpm.isDeviceOwnerApp(app.packageName)
        } catch (e: Exception) {
            Log.w(TAG, "owner check failed", e)
            false
        }
        if (!isOwner) {
            return Status(State.UNKNOWN, reason = "not_profile_or_device_owner")
        }

        return try {
            val packageName = dpm.getAlwaysOnVpnPackage(admin)
            if (packageName.isNullOrEmpty()) {
                Status(State.DISABLED)
            } else {
                val lockdown = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    dpm.isAlwaysOnVpnLockdownEnabled(admin)
                } else {
                    false
                }
                Status(State.ENABLED, packageName = packageName, lockdown = lockdown)
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "always-on VPN query denied", e)
            Status(State.UNKNOWN, reason = "security_exception")
        } catch (e: Exception) {
            Log.w(TAG, "always-on VPN query failed", e)
            Status(State.UNKNOWN, reason = "query_failed")
        }
    }

    fun runOrWarn(
        activity: Activity,
        action: Action,
        onProceed: Runnable,
        onCancel: Runnable? = null,
    ) {
        val status = inspect(activity)
        if (status.state == State.DISABLED) {
            rememberState(status)
            onProceed.run()
            return
        }
        if (!shouldPrompt(status, forceForAction = true)) {
            if (status.state == State.UNKNOWN) {
                onProceed.run()
            } else {
                onCancel?.run()
            }
            return
        }
        showWarning(activity, status, action, onCancel)
    }

    fun maybeWarnStateChanged(activity: Activity) {
        val storage = LocalStorageManager.getInstance()
        if (!storage.getBoolean(LocalStorageManager.PREF_HAS_SETUP)) {
            return
        }
        val status = inspect(activity)
        val previous = storage.getString(LocalStorageManager.PREF_ALWAYS_ON_VPN_LAST_SIGNATURE)
        rememberState(status)
        if (status.state == State.DISABLED || previous == status.signature) {
            return
        }
        if (shouldPrompt(status, forceForAction = false)) {
            showWarning(activity, status, Action.STATE_CHANGED, null)
        }
    }

    private fun shouldPrompt(status: Status, forceForAction: Boolean): Boolean {
        if (status.state == State.DISABLED) {
            return false
        }
        val storage = LocalStorageManager.getInstance()
        val now = SystemClock.elapsedRealtime()
        val lastPromptAt = storage.getLong(LocalStorageManager.PREF_ALWAYS_ON_VPN_LAST_PROMPT_AT)
        val lastPromptedSignature =
            storage.getString(LocalStorageManager.PREF_ALWAYS_ON_VPN_LAST_PROMPT_SIGNATURE)
        val stateChanged = lastPromptedSignature != status.signature
        if (stateChanged) {
            return true
        }
        if (!forceForAction) {
            return false
        }
        return now - lastPromptAt >= PROMPT_COOLDOWN_MS
    }

    private fun rememberPrompt(status: Status) {
        val storage = LocalStorageManager.getInstance()
        storage.setString(LocalStorageManager.PREF_ALWAYS_ON_VPN_LAST_PROMPT_SIGNATURE, status.signature)
        storage.setLong(LocalStorageManager.PREF_ALWAYS_ON_VPN_LAST_PROMPT_AT, SystemClock.elapsedRealtime())
        rememberState(status)
    }

    private fun rememberState(status: Status) {
        LocalStorageManager.getInstance()
            .setString(LocalStorageManager.PREF_ALWAYS_ON_VPN_LAST_SIGNATURE, status.signature)
    }

    private fun showWarning(
        activity: Activity,
        status: Status,
        action: Action,
        onCancel: Runnable?,
    ) {
        if (activity.isFinishing) {
            onCancel?.run()
            return
        }
        rememberPrompt(status)
        val message = when (status.state) {
            State.ENABLED -> activity.getString(
                R.string.always_on_vpn_warning_text,
                status.packageName ?: activity.getString(R.string.always_on_vpn_unknown_package),
                if (status.lockdown) {
                    activity.getString(R.string.always_on_vpn_lockdown_on)
                } else {
                    activity.getString(R.string.always_on_vpn_lockdown_off)
                },
            )
            State.UNKNOWN -> activity.getString(R.string.always_on_vpn_manual_check_text)
            State.DISABLED -> return
        }
        Log.i(TAG, "show warning action=$action status=$status")
        AlertDialog.Builder(activity, R.style.AlertDialogTheme)
            .setTitle(R.string.always_on_vpn_warning_title)
            .setMessage(message)
            .setPositiveButton(R.string.always_on_vpn_open_settings) { _, _ ->
                openVpnSettings(activity)
                onCancel?.run()
            }
            .setNegativeButton(android.R.string.no) { _, _ ->
                onCancel?.run()
            }
            .show()
    }

    fun openVpnSettings(context: Context) {
        val app = context.applicationContext
        val vpnIntent = Intent(Settings.ACTION_VPN_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(vpnIntent)
            return
        } catch (e: ActivityNotFoundException) {
            Log.w(TAG, "VPN settings activity not found", e)
        } catch (e: Exception) {
            Log.w(TAG, "VPN settings launch failed", e)
        }
        try {
            app.startActivity(Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: Exception) {
            Log.w(TAG, "settings launch failed", e)
        }
    }
}
