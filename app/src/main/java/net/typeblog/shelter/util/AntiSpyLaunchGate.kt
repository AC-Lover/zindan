package net.typeblog.shelter.util

import android.content.Context

/**
 * Anti Spy launch path: VPN gating completely disabled.
 */
object AntiSpyLaunchGate {
    const val BROADCAST_LAUNCH_BLOCKED_VPN =
        "net.typeblog.shelter.broadcast.ANTI_SPY_LAUNCH_BLOCKED_VPN"
    const val EXTRA_BLOCK_REASON = "block_reason"
    const val EXTRA_PACKAGE_NAME = "package_name"

    const val REASON_VPN_STILL_ACTIVE = 1
    const val REASON_VPN_PERMISSION_REQUIRED = 2
    const val REASON_FAILED = 3

    enum class VpnGateMode {
        CLEAR_THEN_PROCEED,
        BLOCK_IF_ACTIVE,
    }

    fun interface BlockedCallback {
        fun onBlocked(reason: Int)
    }

    fun needsVpnClear(context: Context, @Suppress("UNUSED_PARAMETER") storage: LocalStorageManager): Boolean = false

    fun shouldApplyVpnGate(packageName: String?, forceGate: Boolean = false): Boolean = false

    fun runBeforeAutoFreezeAccess(
        context: Context,
        storage: LocalStorageManager,
        packageName: String?,
        forceGate: Boolean,
        onProceed: Runnable,
        onBlocked: BlockedCallback?,
        mode: VpnGateMode = VpnGateMode.CLEAR_THEN_PROCEED,
    ) {
        onProceed.run()
    }

    fun runBeforeLaunch(
        context: Context,
        storage: LocalStorageManager,
        packageName: String,
        onProceed: Runnable,
        onBlocked: BlockedCallback?,
    ) {
        onProceed.run()
    }

    fun notifyLaunchBlocked(
        context: Context,
        reason: Int,
        packageName: String?,
        installContext: Boolean = false,
    ) {
        // No-op: VPN gating disabled
    }
}
