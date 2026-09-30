package net.typeblog.shelter.services

import android.content.Intent
import android.net.VpnService
import android.os.IBinder

class AntiSpyDummyVpnService : VpnService() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        stopSelf()
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_ESTABLISH = "net.typeblog.shelter.action.ANTI_SPY_DUMMY_VPN_ESTABLISH"
        const val ACTION_DISCONNECT = "net.typeblog.shelter.action.ANTI_SPY_DUMMY_VPN_DISCONNECT"
        const val BROADCAST_ESTABLISHED = "net.typeblog.shelter.broadcast.ANTI_SPY_DUMMY_VPN_ESTABLISHED"
        const val BROADCAST_FAILED = "net.typeblog.shelter.broadcast.ANTI_SPY_DUMMY_VPN_FAILED"
        const val BROADCAST_PERMISSION_REQUIRED =
            "net.typeblog.shelter.broadcast.ANTI_SPY_DUMMY_VPN_PERMISSION_REQUIRED"
        const val BROADCAST_DISCONNECTED =
            "net.typeblog.shelter.broadcast.ANTI_SPY_DUMMY_VPN_DISCONNECTED"

        fun isTunnelActive(): Boolean = false
    }
}
