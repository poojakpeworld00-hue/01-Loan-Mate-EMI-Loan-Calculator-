package com.loanmate.loan.emicalculator.carloan.homelloan

import android.app.Application
import android.content.Context
import com.loanmate.loan.emicalculator.carloan.homelloan.screens.nudges.LoanUpdateQueries
import com.loanmate.loan.emicalculator.carloan.homelloan.screens.access.Grants
import kotlin.concurrent.thread
import com.loanmate.loan.emicalculator.carloan.adkit.PromoPack
import com.loanmate.loan.emicalculator.carloan.adkit.ExitPromoRunner
import com.loanmate.loan.emicalculator.carloan.adkit.metrics.MetricsTwo
import com.loanmate.loan.emicalculator.carloan.adkit.metrics.TraceSignalPut
import com.loanmate.loan.emicalculator.carloan.adkit.metrics.Views
import com.loanmate.loan.emicalculator.carloan.adkit.draw.AdvancePromoLead
import com.loanmate.loan.emicalculator.carloan.homepanel.Launcher
import com.loanmate.loan.emicalculator.carloan.homelloan.kit.faults.FaultLog
import com.loanmate.loan.emicalculator.carloan.homelloan.kit.device.PrimeScreens
import com.loanmate.loan.emicalculator.carloan.homelloan.kit.device.NoConnected
import com.loanmate.loan.emicalculator.carloan.homelloan.kit.device.ToggleShield
import com.loanmate.loan.emicalculator.carloan.homelloan.kit.device.SysShortcuts
import com.loanmate.loan.emicalculator.carloan.homelloan.store.cloud.flags.UniqueAreas
import com.loanmate.loan.emicalculator.carloan.homelloan.store.cloud.flags.ShipOrigin
import com.loanmate.loan.emicalculator.carloan.homelloan.store.cloud.flags.TrailFinder
import com.loanmate.loan.emicalculator.carloan.homelloan.homebridge.PackBoot
import com.loanmate.loan.emicalculator.carloan.homelloan.screens.welcome.RootRootStep
import com.loanmate.loan.emicalculator.carloan.homelloan.store.cloud.flags.LoanSpecHolder
import com.loanmate.loan.emicalculator.carloan.homelloan.store.cloud.flags.RemoteSyncAlertService
import com.loanmate.loan.emicalculator.carloan.homelloan.store.cloud.flags.Follow
import com.loanmate.loan.emicalculator.carloan.homelloan.inject.LoanCenter
import com.loanmate.loan.emicalculator.carloan.homelloan.screens.intro.StartActivity
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** This app's Kotlin package: its applicationId differs, so screen checks compare class names against this. */
val CODE_PACKAGE: String = LoanTwo::class.java.name.substringBeforeLast('.')

/** Startup order follows the reference app: crash reporting → Remote Config → attribution → analytics → ads → launcher. */
class LoanTwo : Application() {
    lateinit var container: LoanCenter
        private set

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        FaultLog.attach(this)
        // Loads the startup prefs on a background thread, so the first reads on the main thread don't wait on disk (ANR guard).
        thread(name = "prefs-preload", isDaemon = true) {
            STARTUP_PREFS.forEach { runCatching { getSharedPreferences(it, MODE_PRIVATE).all } }
        }
    }

    override fun onCreate() {
        super.onCreate()
        FaultLog.install(this)
        container = LoanCenter(this)

//        LoanSpecHolder.forcePaidUser(true) // testing only: remove before the Play Store release

        LoanSpecHolder.trackResolver = TrailFinder { ShipOrigin.track(this) }
        LoanSpecHolder.init(this)
        // Every install hears the silent `rc_sync` push (RemoteSyncAlertService): one push to the topic re-syncs everyone.
        runCatching { FirebaseMessaging.getInstance().subscribeToTopic(RemoteSyncAlertService.TOPIC) }

        ShipOrigin.start(this, { LoanSpecHolder.current.value.base.attribution }) { LoanSpecHolder.renewTrail() }
        UniqueAreas.start(this, { LoanSpecHolder.current.value.base.exclusiveLocations }) { LoanSpecHolder.renewTrail() }

        MetricsTwo.init(this) { if (LoanSpecHolder.current.value.track == Follow.B) "paid" else "organic" }
        // FFF has no debug event-list screen, so the module's debug notification stays off; events still log to Logcat.
        TraceSignalPut.enabled = { false }
        // Leaving for an ad, a permission prompt or something the app opened itself (share, store, links) doesn't end a screen visit.
        Views.ownLeave = { ExitPromoRunner.busy() || AdvancePromoLead.skipNextAppOpenAd || RootRootStep.shown || Grants.asking }
        Views.install()
        SysShortcuts.listen(this)
        NoConnected.install(this) { LoanSpecHolder.current.value.base.noInternetDialog }
        ToggleShield.install(this) { PrimeScreens.onboardingDone(this) }

        LoanUpdateQueries.install(this) { LoanSpecHolder.current.value.base }

        PromoPack.init(this)
        PromoPack.onError = FaultLog::kitNonFatal
        PromoPack.isHomeScreen = { PrimeScreens.onboardingDone(this) && Launcher.isDefaultHome(it) }
        PromoPack.noResumeAdOn = { it is StartActivity || RootRootStep.shown } // the splash runs its own ad
        PromoPack.recentsAdScreenOn = { PrimeScreens.onboardingDone(this) } // during onboarding a Recents press only ever opens the Play Store jump
        MainScope().launch {
            LoanSpecHolder.current.map { it.ads }.distinctUntilChanged().collect { PromoPack.apply(it) }
        }
        // Now when the saved config has it on (before any launcher screen starts); a first launch installs once the fetch does.
        if (LoanSpecHolder.current.value.launcher.on) PackBoot.install(this)
        else MainScope().launch { LoanSpecHolder.current.first { it.launcher.on }; PackBoot.install(this@LoanTwo) }
    }
}

/** SharedPreferences read during startup (app, config, attribution, ads vault, launcher). */
private val STARTUP_PREFS = listOf(
    "skinhub", "live_config", "user_profile", "rate_prompt", "install_attribution", "exclusive_locations",
    "crash_reporting", "qr_ads_config", "our_icon", "Prefs",
)
