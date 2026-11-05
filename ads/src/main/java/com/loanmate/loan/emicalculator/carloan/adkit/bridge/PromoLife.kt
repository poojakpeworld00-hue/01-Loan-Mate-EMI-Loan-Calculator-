package com.loanmate.loan.emicalculator.carloan.adkit.bridge

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.loanmate.loan.emicalculator.carloan.adkit.draw.fullscreen.NavLayer

/**
 * LoanTwo-wide hooks the engine needs (port of QR ScannerApplication.onActivityResumed):
 * when the user comes back from sponsor links (webview / Chrome tab), resume the flow that opened them
 * (link-first follow-up ads, then the screen's own action — e.g. the next onboarding step).
 */
object PromoLife : Application.ActivityLifecycleCallbacks {
    private var registered = false

    fun register(app: Application) {
        if (registered) return
        registered = true
        app.registerActivityLifecycleCallbacks(this)
    }

    override fun onActivityResumed(activity: Activity) {
        NavLayer.onResultFromSettings(activity)
        keepInlineWarm(activity)
    }

    /** Previous version kept a native ready in the background so screens find one at once (shared load, no spam). */
    private fun keepInlineWarm(activity: Activity) {
        if (activity is com.loanmate.loan.emicalculator.carloan.adkit.draw.SponsorBrowserActivity) return
        val vault = com.loanmate.loan.emicalculator.carloan.adkit.rules.PromoSettingsBox.getInstance(activity)
        if (!vault.getBoolean("IsAdsON") || !vault.getBoolean("NativeAd")) return
        if (com.loanmate.loan.emicalculator.carloan.adkit.draw.InlinePromoRenderer.holdsCachedPromo()) return
        runCatching { com.loanmate.loan.emicalculator.carloan.adkit.draw.InlinePromoRenderer().obtainInlinePromos(activity) }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
    override fun onActivityStarted(activity: Activity) = Unit
    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivityStopped(activity: Activity) = NavLayer.onLoanCovered()
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) = Unit
}
