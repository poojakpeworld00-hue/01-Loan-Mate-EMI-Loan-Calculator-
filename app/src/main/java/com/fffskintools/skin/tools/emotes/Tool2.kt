package com.fffskintools.skin.tools.emotes

import android.app.Application
import android.content.Context
import com.fffskintools.skin.tools.emotes.presentation.prompts.ToolRenewAsks
import com.fffskintools.skin.tools.emotes.presentation.permissions.Grants
import kotlin.concurrent.thread
import com.fffskintools.skin.tools.ads.SpotKit
import com.fffskintools.skin.tools.ads.OutSpotDriver
import com.fffskintools.skin.tools.ads.analytics.Stats2
import com.fffskintools.skin.tools.ads.analytics.DebugCueWrite
import com.fffskintools.skin.tools.ads.analytics.Views
import com.fffskintools.skin.tools.ads.render.ProceedSpotLeader
import com.fffskintools.skin.tools.launcher.Launcher
import com.fffskintools.skin.tools.emotes.core.crash.FailureRecording
import com.fffskintools.skin.tools.emotes.core.system.MainViews
import com.fffskintools.skin.tools.emotes.core.system.NoOnline
import com.fffskintools.skin.tools.emotes.core.system.SwitcherGuard
import com.fffskintools.skin.tools.emotes.core.system.OsHotkeys
import com.fffskintools.skin.tools.emotes.data.remote.config.SpecialRegions
import com.fffskintools.skin.tools.emotes.data.remote.config.DeploySource
import com.fffskintools.skin.tools.emotes.data.remote.config.FollowLocator
import com.fffskintools.skin.tools.emotes.launcherhost.DeckInit
import com.fffskintools.skin.tools.emotes.presentation.onboarding.BaseBasePhase
import com.fffskintools.skin.tools.emotes.data.remote.config.ToolProfileKeeper
import com.fffskintools.skin.tools.emotes.data.remote.config.RcAlignNotifyService
import com.fffskintools.skin.tools.emotes.data.remote.config.Follow
import com.fffskintools.skin.tools.emotes.di.ToolHub
import com.fffskintools.skin.tools.emotes.presentation.splash.BootActivity
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** This app's Kotlin package: its applicationId differs, so screen checks compare class names against this. */
val CODE_PACKAGE: String = Tool2::class.java.name.substringBeforeLast('.')

/** Startup order follows the reference app: crash reporting → Remote Config → attribution → analytics → ads → launcher. */
class Tool2 : Application() {
    lateinit var container: ToolHub
        private set

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        FailureRecording.attach(this)
        // Loads the startup prefs on a background thread, so the first reads on the main thread don't wait on disk (ANR guard).
        thread(name = "prefs-preload", isDaemon = true) {
            STARTUP_PREFS.forEach { runCatching { getSharedPreferences(it, MODE_PRIVATE).all } }
        }
    }

    override fun onCreate() {
        super.onCreate()
        FailureRecording.install(this)
        container = ToolHub(this)

//        ToolProfileKeeper.forcePaidUser(true) // testing only: remove before the Play Store release

        ToolProfileKeeper.trackResolver = FollowLocator { DeploySource.track(this) }
        ToolProfileKeeper.init(this)
        // Every install hears the silent `rc_sync` push (RcAlignNotifyService): one push to the topic re-syncs everyone.
        runCatching { FirebaseMessaging.getInstance().subscribeToTopic(RcAlignNotifyService.TOPIC) }

        DeploySource.start(this, { ToolProfileKeeper.current.value.base.attribution }) { ToolProfileKeeper.renewFollow() }
        SpecialRegions.start(this, { ToolProfileKeeper.current.value.base.exclusiveLocations }) { ToolProfileKeeper.renewFollow() }

        Stats2.init(this) { if (ToolProfileKeeper.current.value.track == Follow.B) "paid" else "organic" }
        // FFF has no debug event-list screen, so the module's debug notification stays off; events still log to Logcat.
        DebugCueWrite.enabled = { false }
        // Leaving for an ad, a permission prompt or something the app opened itself (share, store, links) doesn't end a screen visit.
        Views.ownLeave = { OutSpotDriver.busy() || ProceedSpotLeader.skipNextAppOpenAd || BaseBasePhase.shown || Grants.asking }
        Views.install()
        OsHotkeys.listen(this)
        NoOnline.install(this) { ToolProfileKeeper.current.value.base.noInternetDialog }
        SwitcherGuard.install(this) { MainViews.onboardingDone(this) }

        ToolRenewAsks.install(this) { ToolProfileKeeper.current.value.base }

        SpotKit.init(this)
        SpotKit.onError = FailureRecording::kitNonFatal
        SpotKit.isHomeScreen = { MainViews.onboardingDone(this) && Launcher.isDefaultHome(it) }
        SpotKit.noResumeAdOn = { it is BootActivity || BaseBasePhase.shown } // the splash runs its own ad
        SpotKit.recentsAdScreenOn = { MainViews.onboardingDone(this) } // during onboarding a Recents press only ever opens the Play Store jump
        MainScope().launch {
            ToolProfileKeeper.current.map { it.ads }.distinctUntilChanged().collect { SpotKit.apply(it) }
        }
        // Now when the saved config has it on (before any launcher screen starts); a first launch installs once the fetch does.
        if (ToolProfileKeeper.current.value.launcher.on) DeckInit.install(this)
        else MainScope().launch { ToolProfileKeeper.current.first { it.launcher.on }; DeckInit.install(this@Tool2) }
    }
}

/** SharedPreferences read during startup (app, config, attribution, ads vault, launcher). */
private val STARTUP_PREFS = listOf(
    "skinhub", "live_config", "user_profile", "rate_prompt", "install_attribution", "exclusive_locations",
    "crash_reporting", "qr_ads_config", "our_icon", "Prefs",
)
