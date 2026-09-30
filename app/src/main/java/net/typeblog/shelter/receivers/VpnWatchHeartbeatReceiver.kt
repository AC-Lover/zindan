package net.typeblog.shelter.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * VPN watch heartbeat receiver — disabled.
 */
class VpnWatchHeartbeatReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Disabled: VPN watch service is removed.
    }
}
