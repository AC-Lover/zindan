package net.typeblog.shelter.util

import android.content.Context

/**
 * Anti Spy VPN prompt manager — completely disabled.
 */
object AntiSpyVpnPromptManager {
    const val EXTRA_MODE = "anti_spy_vpn_prompt_mode"
    const val MODE_FREEZE_PROMPT = 1
    const val MODE_VPN_PERMISSION = 2
    const val MODE_DISPLACEMENT_FAILED = 3

    fun isPromptActive(): Boolean = false

    fun isDeclinedForCurrentVpnSession(): Boolean = false

    fun onVpnSessionEnded() {}

    fun showPrompt(context: Context) {}

    fun showVpnPermissionNeeded(context: Context) {}

    fun showDisplacementFailed(context: Context) {}

    fun dismiss(context: Context) {}

    fun onUserDeclined(context: Context) {}

    fun onUserConfirmedFreeze(context: Context) {}
}
