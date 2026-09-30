package net.typeblog.shelter.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * VPN batch freeze receiver — disabled.
 */
class AntiSpyVpnFreezeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Disabled: VPN checking and VPN-triggered freeze are removed.
    }

    companion object {
        const val ACTION = "net.typeblog.shelter.action.VPN_BATCH_FREEZE"
    }
}
