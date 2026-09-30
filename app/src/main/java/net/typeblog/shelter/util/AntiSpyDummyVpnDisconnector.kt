package net.typeblog.shelter.util

import android.content.Context

/**
 * Displaces an active VPN using dummy VPN — completely disabled.
 */
object AntiSpyDummyVpnDisconnector {
    const val RESULT_CLEARED = 0
    const val RESULT_VPN_STILL_ACTIVE = 1
    const val RESULT_VPN_PERMISSION_REQUIRED = 2
    const val RESULT_FAILED = 3

    fun isSuppressingVpnReactions(): Boolean = false

    fun interface Callback {
        fun onResult(result: Int)
    }

    fun tryClearVpnAsync(context: Context, callback: Callback) {
        callback.onResult(RESULT_CLEARED)
    }
}
