package net.typeblog.shelter.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import net.typeblog.shelter.R
import net.typeblog.shelter.receivers.AntiSpyVpnWatchWatchdogReceiver

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

    fun scheduleWatchdog(context: Context) {
        cancelWatchdog(context)
    }

    fun cancelWatchdog(context: Context) {
        try {
            val app = context.applicationContext
            val am = app.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(app, AntiSpyVpnWatchWatchdogReceiver::class.java).apply {
                action = ACTION_WATCHDOG
            }
            val pi = PendingIntent.getBroadcast(
                app,
                0xE49E8,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            am.cancel(pi)
            pi.cancel()
        } catch (_: Exception) {}
    }

    fun scheduleFgsRetry(context: Context) {}

    fun runWatchdog(context: Context) {}

    fun restartMonitoring(context: Context) {
        cancelWatchdog(context)
    }
}
