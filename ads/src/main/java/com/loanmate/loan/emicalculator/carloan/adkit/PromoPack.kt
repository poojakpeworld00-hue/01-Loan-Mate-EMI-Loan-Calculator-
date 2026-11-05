package com.loanmate.loan.emicalculator.carloan.adkit

import android.app.Activity
import android.app.Application
import com.loanmate.loan.emicalculator.carloan.adkit.setup.PromosSpec
import com.loanmate.loan.emicalculator.carloan.adkit.triggers.power.ChargeSignalTracker
import com.loanmate.loan.emicalculator.carloan.adkit.triggers.installs.PackageSignalTracker
import com.loanmate.loan.emicalculator.carloan.adkit.triggers.recall.LatestPromoTracker
import com.loanmate.loan.emicalculator.carloan.adkit.bridge.PromoLife
import com.loanmate.loan.emicalculator.carloan.adkit.bridge.PromoPrefetch
import com.loanmate.loan.emicalculator.carloan.adkit.bridge.LoanAdvanceTracker
import com.loanmate.loan.emicalculator.carloan.adkit.homebind.LaunchPromoTracker
import com.loanmate.loan.emicalculator.carloan.adkit.rules.AcceptPathWall
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentInformation
import com.google.android.ump.UserMessagingPlatform
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/** Module setup: init once, apply every config, UMP consent. Views use [Spots] for the ads themselves. */
object PromoPack {
    /** The config last given to [apply]; default = ads off. */
    @Volatile
    var config: PromosSpec = PromosSpec()
        private set

    /** Views where `launcher.phone_unlock` may show (e.g. "we are the default home and onboarding is done"). */
    @Volatile
    var isHomeScreen: (Activity) -> Boolean = { true }

    /** Views that never get the `launcher.app_resume` ad (permission / default-home / settings returns, …). */
    @Volatile
    var noResumeAdOn: (Activity) -> Boolean = { false }

    /** Views where a Recents press may lead to the `launcher.play_store_return` ad screen (e.g. not during onboarding). */
    @Volatile
    var recentsAdScreenOn: (Activity) -> Boolean = { true }

    /** Caught failures worth fixing (an ad flow that threw), with where: the app sends them to Crashlytics. */
    @Volatile
    var onError: (Throwable, String) -> Unit = { _, _ -> }

    private var app: Application? = null
    private val mobileAdsStarted = AtomicBoolean(false)

    internal fun context(): Application = checkNotNull(app) { "AdKit.init(app) was not called" }

    fun init(app: Application) {
        if (this.app != null) return
        this.app = app
        PromoLife.register(app)
        LoanAdvanceTracker.register(app)
        LaunchPromoTracker.register(app)
        ChargeSignalTracker.register(app)
        PackageSignalTracker.register(app)
        LatestPromoTracker.register(app)
        if (AcceptPathWall.getInstance(app).canRequestAds()) kickMobileSpots()
        PromosSpecSetter.apply(app, config, agreeAllows())
    }

    /** Safe on every config refresh. */
    fun apply(config: PromosSpec) {
        this.config = config
        val app = context()
        PromosSpecSetter.apply(app, config, agreeAllows())
        PackageSignalTracker.kitSeedIfNeeded(app)
    }

    /** UMP: update consent info, show the form when required, then start the Mobile Spots SDK. [onDone] once. */
    fun requestAgree(activity: Activity, onDone: () -> Unit) {
        val once = AtomicBoolean(false)
        val done = {
            if (once.compareAndSet(false, true)) {
                if (AcceptPathWall.getInstance(activity).canRequestAds()) kickMobileSpots()
                PromosSpecSetter.apply(activity, config, agreeAllows())
                onDone()
            }
        }
        runCatching { AcceptPathWall.getInstance(activity).queryAgree(activity) { done() } }.onFailure { done() }
    }

    /** Settings "Privacy options" entry is needed when this is true. */
    fun secrecyPicksRequired(): Boolean = AcceptPathWall.getInstance(context()).isPrivacyOptionsRequired

    fun presentSecrecyPicks(activity: Activity, onDone: () -> Unit) =
        AcceptPathWall.getInstance(activity).enterSecrecyPicks(activity) { onDone() }

    fun preload(activity: Activity) = PromoPrefetch.all(activity)

    /** `ads.on` and the consent gate. */
    internal fun spotsOn(): Boolean = config.on && agreeAllows()

    /** MSG-06 rule (AdIngest.gateOpen): no ads only when consent is required and was not given. */
    private fun agreeAllows(): Boolean {
        val app = app ?: return false
        return runCatching {
            val info = UserMessagingPlatform.getConsentInformation(app)
            info.canRequestAds() || info.consentStatus != ConsentInformation.ConsentStatus.REQUIRED
        }.getOrDefault(true)
    }

    private fun kickMobileSpots() {
        val app = app ?: return
        if (!mobileAdsStarted.compareAndSet(false, true)) return
        Executors.newSingleThreadExecutor().execute {
            runCatching { MobileAds.initialize(app) {} }.onFailure { mobileAdsStarted.set(false) }
        }
    }
}
