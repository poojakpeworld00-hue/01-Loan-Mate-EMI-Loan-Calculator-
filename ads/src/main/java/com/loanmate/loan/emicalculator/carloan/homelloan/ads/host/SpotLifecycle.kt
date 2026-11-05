package com.loanmate.loan.emicalculator.carloan.homelloan.ads.host

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.loanmate.loan.emicalculator.carloan.homelloan.ads.render.interstitial.NavigationOverlay

/**
 * Tool2-wide hooks the engine needs (port of QR ScannerApplication.onActivityResumed):
 * when the user comes back from sponsor links (webview / Chrome tab), resume the flow that opened them
 * (link-first follow-up ads, then the screen's own action — e.g. the next onboarding step).
 */
object SpotLifecycle : Application.ActivityLifecycleCallbacks {
    private var registered = false

    fun register(app: Application) {
        if (registered) return
        registered = true
        app.registerActivityLifecycleCallbacks(this)
    }

    override fun onActivityResumed(activity: Activity) {
        NavigationOverlay.onReturnFromOptions(activity)
        keepEmbeddedWarm(activity)
    }

    /** Previous version kept a native ready in the background so screens find one at once (shared load, no spam). */
    private fun keepEmbeddedWarm(activity: Activity) {
        if (activity is com.loanmate.loan.emicalculator.carloan.homelloan.ads.render.PatronWebActivity) return
        val vault = com.loanmate.loan.emicalculator.carloan.homelloan.ads.policy.SpotOptionsChest.getInstance(activity)
        if (!vault.getBoolean("IsAdsON") || !vault.getBoolean("NativeAd")) return
        if (com.loanmate.loan.emicalculator.carloan.homelloan.ads.render.EmbeddedSpotPainter.holdsCachedSpot()) return
        runCatching { com.loanmate.loan.emicalculator.carloan.homelloan.ads.render.EmbeddedSpotPainter().obtainEmbeddedSpots(activity) }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
    override fun onActivityStarted(activity: Activity) = Unit
    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivityStopped(activity: Activity) = NavigationOverlay.onToolCovered()
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) = Unit
}
