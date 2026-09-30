package net.typeblog.shelter.util

import android.content.Context
import android.util.Log

/**
 * VPN detection completely disabled.
 */
object VpnTunnelDetector {
    private const val TAG = "VpnTunnelDetector"

    fun isVpnActive(context: Context): Boolean = false

    fun logDiagnostics(context: Context) {
        Log.i(TAG, "VPN detection is completely disabled.")
    }
}
