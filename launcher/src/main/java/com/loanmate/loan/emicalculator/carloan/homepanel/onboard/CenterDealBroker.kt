package com.loanmate.loan.emicalculator.carloan.homepanel.onboard

import android.app.Activity
import android.util.Log
import com.loanmate.loan.emicalculator.carloan.homepanel.Launcher
import com.loanmate.loan.emicalculator.carloan.homepanel.gateway.StarterTray

/**
 * The ad / analytics moments of MSG-05's launcher, handed to [com.loanmate.loan.emicalculator.carloan.homepanel.gateway.StarterSignals].
 * [action] runs once, when the host calls `proceed` (or at once if the host throws).
 */
object CenterDealBroker {
    private const val TAG = "LAUNCHER_PROMO"

    /** What the next launcher resume returns from (MSG-05 LauncherAds.onAppLaunched / onLobbyResume). */
    enum class Away { NONE, APP, SPONSORED }

    var away = Away.NONE

    fun run(activity: Activity?, gesture: String, packageName: String = "", action: () -> Unit) {
        var done = false
        val once = {
            if (!done) {
                done = true
                action()
            }
        }
        if (activity == null) return once()
        val events = Launcher.events
        runCatching {
            when (gesture) {
                CenterAdsToggles.LEFT_SWIPE -> events.onPanelOpened(activity, StarterTray.SUGGESTED, once)
                CenterAdsToggles.RIGHT_SWIPE -> events.onPanelOpened(activity, StarterTray.HOST_APP, once)
                CenterAdsToggles.DRAWER_OPEN -> events.onDrawerOpened(activity, once)
                CenterAdsToggles.APP_LAUNCH -> events.onAppIconTapped(activity, packageName, once)
                else -> once()
            }
        }.onFailure {
            Log.w(TAG, "$gesture: host event failed - running the action", it)
            once()
        }
    }

    /** Launcher home came back to the front: tell the host where the user returns from. */
    fun onLobbyResume(activity: Activity) {
        val from = away
        away = Away.NONE
        when (from) {
            Away.APP -> Launcher.events.onReturnedFromApp(activity)
            Away.SPONSORED -> Launcher.events.onReturnedFromSponsored(activity)
            Away.NONE -> Unit
        }
    }
}
