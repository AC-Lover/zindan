package net.typeblog.shelter.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log

/**
 * Background VPN watcher — completely disabled.
 */
class AntiSpyVpnWatchService : Service() {
    override fun onCreate() {
        super.onCreate()
        stopSelf()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        stopSelf()
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "AntiSpyVpnWatch"

        fun syncState(context: Context, allowBackgroundRetry: Boolean = true) {
            try {
                val app = context.applicationContext
                val intent = Intent(app, AntiSpyVpnWatchService::class.java)
                app.stopService(intent)
            } catch (e: Exception) {
                Log.d(TAG, "VPN watch disabled")
            }
        }
    }
}
