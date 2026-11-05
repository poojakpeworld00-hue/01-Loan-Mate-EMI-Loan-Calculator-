package com.loanmate.loan.emicalculator.carloan.homelloan.ads

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.core.content.edit
import com.loanmate.loan.emicalculator.carloan.homelloan.ads.config.SpotPhase
import com.loanmate.loan.emicalculator.carloan.homelloan.ads.config.OutSpot
import com.loanmate.loan.emicalculator.carloan.homelloan.ads.host.SpotCues
import com.loanmate.loan.emicalculator.carloan.homelloan.ads.policy.SpotWaits
import com.loanmate.loan.emicalculator.carloan.homelloan.ads.render.EmbeddedSpotPainter
import com.loanmate.loan.emicalculator.carloan.homelloan.ads.render.ProceedSpotLeader
import com.loanmate.loan.emicalculator.carloan.homelloan.ads.render.PrizeSpotPainter
import com.loanmate.loan.emicalculator.carloan.homelloan.ads.render.interstitial.ChainOverlay
import com.loanmate.loan.emicalculator.carloan.homelloan.ads.render.interstitial.OverlayBackupChain
import com.loanmate.loan.emicalculator.carloan.homelloan.ads.render.interstitial.OverlayWarmer
import com.loanmate.loan.emicalculator.carloan.homelloan.ads.render.interstitial.NavigationOverlay

/**
 * Runs a place's `exit_ad` (see [OutSpotRoute] for the rules) on the ported engine.
 * [onDone] runs exactly once, also when the activity finishes; never two full-screen ads at a time.
 */
object OutSpotDriver {
    /** Places whose `gap_sec` limits their event screen, not their close ad (see [Spots]). */
    internal val SCREEN_GAP_PLACES = setOf(
        Spots.CHARGER_CONNECTED, Spots.CHARGER_DISCONNECTED, Spots.APP_INSTALLED, Spots.APP_UNINSTALLED, Spots.PLAY_STORE_RETURN,
    )

    private const val PREFS = "adkit_exit_gap"
    private val every = KitEveryCounter()
    private val main = Handler(Looper.getMainLooper())
    private var active: OutSpotRoute? = null

    /** A full-screen ad, loader or exit flow is on screen. */
    fun busy(): Boolean = active != null || NavigationOverlay.isInterShow || ProceedSpotLeader.isShowingAd ||
        EmbeddedSpotPainter.isFullNativeShowing || OverlayWarmer.isShowing

    fun run(activity: Activity, placeKey: String, onDone: () -> Unit) =
        run(activity, placeKey, SpotKit.config.places[placeKey]?.exitAd, onDone)

    fun run(activity: Activity, placeKey: String, exit: OutSpot?, onDone: () -> Unit) {
        var called = false
        val done = { if (!called) { called = true; onDone() } }
        val config = SpotKit.config
        if (!PlaceGuards.outRuns(SpotKit.spotsOn(), exit, every, placeKey)) return done()
        exit!!
        if (activity.isFinishing || activity.isDestroyed || busy()) return done()
        val gapSec = (config.places[placeKey]?.int("gap_sec") ?: 0).toLong()
        val gapped = placeKey !in SCREEN_GAP_PLACES && gapSec > 0
        if (gapped && !PlaceGuards.kitGapPassed(kitLastRun(activity, placeKey), gapSec, System.currentTimeMillis())) return done()
        if (gapped) kitMarkRun(activity, placeKey)

        SpotsProfileApplier.kitWritePalette(activity, config)
        val watcher = FinishMonitor(activity)
        val flow = OutSpotRoute(
            config, exit, EngineFeed(activity, placeKey),
            isAlive = { !activity.isFinishing && !activity.isDestroyed },
        ) {
            active = null
            SpotCues.place = ""
            watcher.stop()
            done()
        }
        active = flow
        // Every full-screen ad of this run reports this place (see SpotCues).
        SpotCues.place = placeKey
        watcher.onGone = { flow.finish() }
        flow.start()
    }

    private fun kitLastRun(context: Context, key: String) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong(key, 0L)

    private fun kitMarkRun(context: Context, key: String) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit { putLong(key, System.currentTimeMillis()) }

    /** Ends the flow when its activity is destroyed (onDone still runs once). */
    private class FinishMonitor(private val activity: Activity) : Application.ActivityLifecycleCallbacks {
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
    private class EngineFeed(private val activity: Activity, private val placeKey: String) : OutSpotFeed {
        /** Splash: no step starts later than loader_wait_sec after the first one, so the user never waits longer. */
        private var splashDeadline = 0L

        override fun show(step: SpotPhase, onShown: () -> Unit, onNotReady: () -> Unit) {
            if (activity.isFinishing || activity.isDestroyed) return onNotReady()
            if (placeKey == Spots.SPLASH) {
                val now = SystemClock.elapsedRealtime()
                if (splashDeadline == 0L) splashDeadline = now + SpotWaits.waitMs(activity)
                else if (now >= splashDeadline) {
                    SpotCues.send(SpotCues.formatOf(step), "timeout", placeKey)
                    return onNotReady()
                }
            }
            val units = SpotKit.config.units
            // ads.loader_on_miss false: an ad that is not cached yet is skipped instead of loaded behind "Please wait".
            // The splash keeps loading: it is a waiting screen already. A place's own `loader_on_miss` wins over the global.
            val loadIfMissing = placeKey == Spots.SPLASH ||
                (SpotKit.config.places[placeKey]?.options?.get("loader_on_miss") as? Boolean ?: SpotKit.config.loaderOnMiss)
            when (step) {
                SpotPhase.INTER -> {
                    val unit = when (placeKey) {
                        Spots.SPLASH -> units.interForBoot()
                        Spots.BACK_BUTTON -> units.interForPrior()
                        else -> units.inter
                    }
                    ChainOverlay.show(activity, unit, placeKey, onMiss = onNotReady, loadIfMissing = loadIfMissing) { onShown() }
                }
                SpotPhase.REWARDED -> PrizeSpotPainter.presentOrPull(activity, onShown, onNotReady, loadIfMissing = loadIfMissing)
                SpotPhase.APP_OPEN -> ProceedSpotLeader.presentLoaded(activity, onShown, onNotReady, loadIfMissing = loadIfMissing)
                SpotPhase.FULL_NATIVE ->
                    OverlayBackupChain.fullEmbedded(activity, placeKey, next = onNotReady, done = onShown, loadIfMissing = loadIfMissing)
                SpotPhase.CUSTOM -> OverlayBackupChain.custom(activity, next = onNotReady, done = onShown)
                SpotPhase.LINK -> onNotReady() // never asked: OutSpotRoute opens links itself
            }
        }

        override fun openLinks(urls: List<String>, onBack: () -> Unit) {
            // Our own return: the app-open / resume ad must not fire on it (cleared after the return settles).
            ProceedSpotLeader.skipNextAppOpenAd = true
            SpotCues.send(SpotCues.CUSTOM, "link_open", placeKey)
            NavigationOverlay.enterPatronLink(activity, placeKey, urls) {
                main.postDelayed({ ProceedSpotLeader.skipNextAppOpenAd = false }, 1_000L)
                onBack()
            }
        }
    }
}
