package com.loanmate.loan.emicalculator.carloan.adkit

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.core.content.edit
import com.loanmate.loan.emicalculator.carloan.adkit.setup.PromoStep
import com.loanmate.loan.emicalculator.carloan.adkit.setup.ExitPromo
import com.loanmate.loan.emicalculator.carloan.adkit.bridge.PromoSignals
import com.loanmate.loan.emicalculator.carloan.adkit.rules.PromoDelays
import com.loanmate.loan.emicalculator.carloan.adkit.draw.InlinePromoRenderer
import com.loanmate.loan.emicalculator.carloan.adkit.draw.AdvancePromoLead
import com.loanmate.loan.emicalculator.carloan.adkit.draw.RewardPromoRenderer
import com.loanmate.loan.emicalculator.carloan.adkit.draw.fullscreen.LinkLayer
import com.loanmate.loan.emicalculator.carloan.adkit.draw.fullscreen.LayerSpareLink
import com.loanmate.loan.emicalculator.carloan.adkit.draw.fullscreen.LayerPrimer
import com.loanmate.loan.emicalculator.carloan.adkit.draw.fullscreen.NavLayer

/**
 * Runs a place's `exit_ad` (see [ExitPromoPath] for the rules) on the ported engine.
 * [onDone] runs exactly once, also when the activity finishes; never two full-screen ads at a time.
 */
object ExitPromoRunner {
    /** Places whose `gap_sec` limits their event screen, not their close ad (see [Spots]). */
    internal val SCREEN_GAP_PLACES = setOf(
        Spots.CHARGER_CONNECTED, Spots.CHARGER_DISCONNECTED, Spots.APP_INSTALLED, Spots.APP_UNINSTALLED, Spots.PLAY_STORE_RETURN,
    )

    private const val PREFS = "adkit_exit_gap"
    private val every = PackEachTally()
    private val main = Handler(Looper.getMainLooper())
    private var active: ExitPromoPath? = null

    /** A full-screen ad, loader or exit flow is on screen. */
    fun busy(): Boolean = active != null || NavLayer.isInterShow || AdvancePromoLead.isShowingAd ||
        InlinePromoRenderer.isFullNativeShowing || LayerPrimer.isShowing

    fun run(activity: Activity, placeKey: String, onDone: () -> Unit) =
        run(activity, placeKey, PromoPack.config.places[placeKey]?.exitAd, onDone)

    fun run(activity: Activity, placeKey: String, exit: ExitPromo?, onDone: () -> Unit) {
        var called = false
        val done = { if (!called) { called = true; onDone() } }
        val config = PromoPack.config
        if (!SlotChecks.outRuns(PromoPack.spotsOn(), exit, every, placeKey)) return done()
        exit!!
        if (activity.isFinishing || activity.isDestroyed || busy()) return done()
        val gapSec = (config.places[placeKey]?.int("gap_sec") ?: 0).toLong()
        val gapped = placeKey !in SCREEN_GAP_PLACES && gapSec > 0
        if (gapped && !SlotChecks.kitGapPassed(kitLastRun(activity, placeKey), gapSec, System.currentTimeMillis())) return done()
        if (gapped) kitMarkRun(activity, placeKey)

        PromosSpecSetter.kitWritePalette(activity, config)
        val watcher = DoneTracker(activity)
        val flow = ExitPromoPath(
            config, exit, CoreSource(activity, placeKey),
            isAlive = { !activity.isFinishing && !activity.isDestroyed },
        ) {
            active = null
            PromoSignals.place = ""
            watcher.stop()
            done()
        }
        active = flow
        // Every full-screen ad of this run reports this place (see PromoSignals).
        PromoSignals.place = placeKey
        watcher.onGone = { flow.finish() }
        flow.start()
    }

    private fun kitLastRun(context: Context, key: String) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong(key, 0L)

    private fun kitMarkRun(context: Context, key: String) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit { putLong(key, System.currentTimeMillis()) }

    /** Ends the flow when its activity is destroyed (onDone still runs once). */
    private class DoneTracker(private val activity: Activity) : Application.ActivityLifecycleCallbacks {
        var onGone: () -> Unit = {}

        init {
            activity.application.registerActivityLifecycleCallbacks(this)
        }

        fun stop() = activity.application.unregisterActivityLifecycleCallbacks(this)

        override fun onActivityDestroyed(a: Activity) {
            if (a === activity) onGone()
        }

        override fun onActivityCreated(a: Activity, b: Bundle?) = Unit
        override fun onActivityStarted(a: Activity) = Unit
        override fun onActivityResumed(a: Activity) = Unit
        override fun onActivityPaused(a: Activity) = Unit
        override fun onActivityStopped(a: Activity) = Unit
        override fun onActivitySaveInstanceState(a: Activity, b: Bundle) = Unit
    }

    /** Each step on the ported engine; a missing ad is loaded behind the loader for up to loader_wait_sec. */
    private class CoreSource(private val activity: Activity, private val placeKey: String) : ExitPromoSource {
        /** Splash: no step starts later than loader_wait_sec after the first one, so the user never waits longer. */
        private var splashDeadline = 0L

        override fun show(step: PromoStep, onShown: () -> Unit, onNotReady: () -> Unit) {
            if (activity.isFinishing || activity.isDestroyed) return onNotReady()
            if (placeKey == Spots.SPLASH) {
                val now = SystemClock.elapsedRealtime()
                if (splashDeadline == 0L) splashDeadline = now + PromoDelays.waitMs(activity)
                else if (now >= splashDeadline) {
                    PromoSignals.send(PromoSignals.formatOf(step), "timeout", placeKey)
                    return onNotReady()
                }
            }
            val units = PromoPack.config.units
            // ads.loader_on_miss false: an ad that is not cached yet is skipped instead of loaded behind "Please wait".
            // The splash keeps loading: it is a waiting screen already. A place's own `loader_on_miss` wins over the global.
            val loadIfMissing = placeKey == Spots.SPLASH ||
                (PromoPack.config.places[placeKey]?.options?.get("loader_on_miss") as? Boolean ?: PromoPack.config.loaderOnMiss)
            when (step) {
                PromoStep.INTER -> {
                    val unit = when (placeKey) {
                        Spots.SPLASH -> units.interForBoot()
                        Spots.BACK_BUTTON -> units.interForPrior()
                        else -> units.inter
                    }
                    LinkLayer.show(activity, unit, placeKey, onMiss = onNotReady, loadIfMissing = loadIfMissing) { onShown() }
                }
                PromoStep.REWARDED -> RewardPromoRenderer.presentOrPull(activity, onShown, onNotReady, loadIfMissing = loadIfMissing)
                PromoStep.APP_OPEN -> AdvancePromoLead.presentLoaded(activity, onShown, onNotReady, loadIfMissing = loadIfMissing)
                PromoStep.FULL_NATIVE ->
                    LayerSpareLink.fullEmbedded(activity, placeKey, next = onNotReady, done = onShown, loadIfMissing = loadIfMissing)
                PromoStep.CUSTOM -> LayerSpareLink.custom(activity, next = onNotReady, done = onShown)
                PromoStep.LINK -> onNotReady() // never asked: ExitPromoPath opens links itself
            }
        }

        override fun openLinks(urls: List<String>, onBack: () -> Unit) {
            // Our own return: the app-open / resume ad must not fire on it (cleared after the return settles).
            AdvancePromoLead.skipNextAppOpenAd = true
            PromoSignals.send(PromoSignals.CUSTOM, "link_open", placeKey)
            NavLayer.enterPatronLink(activity, placeKey, urls) {
                main.postDelayed({ AdvancePromoLead.skipNextAppOpenAd = false }, 1_000L)
                onBack()
            }
        }
    }
}
