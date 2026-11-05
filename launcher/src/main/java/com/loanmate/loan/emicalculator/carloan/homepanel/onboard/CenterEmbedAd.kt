package com.loanmate.loan.emicalculator.carloan.homepanel.onboard

import android.app.Activity
import android.util.Log
import android.widget.FrameLayout
import androidx.core.view.ViewCompat
import com.loanmate.loan.emicalculator.carloan.homepanel.Launcher
import com.loanmate.loan.emicalculator.carloan.homepanel.gateway.StarterBay
import com.loanmate.loan.emicalculator.carloan.homepanel.helpers.reviveNavStrip

object CenterEmbedAd {
    private const val TAG = "LAUNCHER_AD"

    fun wireOnReveal(activity: Activity?, key: String, container: FrameLayout?) {
        if (activity == null || container == null) return
        val slot = when (key) {
            CenterAdsToggles.KEY_PANEL_BOTTOM -> StarterBay.SUGGESTED_BOTTOM
            CenterAdsToggles.KEY_PANEL_MIDDLE -> StarterBay.SUGGESTED_LIST
            CenterAdsToggles.KEY_APP_DRAWER -> StarterBay.DRAWER
            else -> return
        }
        runCatching {
            Launcher.events.bindSlot(activity, slot, container)
            ViewCompat.requestApplyInsets(container)
            activity.reviveNavStrip()
        }.onFailure { Log.w(TAG, "could not render $key", it) }
    }
}
