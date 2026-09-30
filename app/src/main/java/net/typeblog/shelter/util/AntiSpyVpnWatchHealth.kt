package net.typeblog.shelter.util

import android.content.Context
import net.typeblog.shelter.R

/**
 * Heartbeat + watchdog for :vpnwatch — completely disabled.
 */
object AntiSpyVpnWatchHealth {
    const val MAIN_HEARTBEAT_STALE_MS = 12_000L
    const val WORK_HEARTBEAT_STALE_MS = 90_000L
    const val WATCHDOG_INTERVAL_MS = 30 * 60 * 1000L
    const val FGS_RETRY_DELAY_MS = 5_000L

    const val ACTION_WATCHDOG = "net.typeblog.shelter.action.VPN_WATCH_WATCHDOG"
    const val ACTION_FGS_RETRY = "net.typeblog.shelter.action.VPN_WATCH_FGS_RETRY"
    const val ACTION_HEARTBEAT = "net.typeblog.shelter.action.VPN_WATCH_HEARTBEAT"

    const val EXTRA_VPN_ACTIVE = "vpn_active"
    const val EXTRA_AT = "at"

    fun recordHeartbeat(context: Context, vpnActive: Boolean) {}

    fun recordWorkHeartbeatOnMain(at: Long, vpnActive: Boolean) {}

    fun isMainWatcherAlive(context: Context): Boolean = false

    fun isWorkWatcherAlive(context: Context): Boolean = false

    fun formatStatusLine(context: Context, mainProfile: Boolean): String =
        context.getString(R.string.settings_vpn_watch_status_never)

    fun scheduleWatchdog(context: Context) {}

    fun cancelWatchdog(context: Context) {}

    fun scheduleFgsRetry(context: Context) {}

    fun runWatchdog(context: Context) {}

    fun restartMonitoring(context: Context) {}
}
