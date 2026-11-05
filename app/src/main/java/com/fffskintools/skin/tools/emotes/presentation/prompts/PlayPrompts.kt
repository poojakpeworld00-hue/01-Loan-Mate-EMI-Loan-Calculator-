package com.fffskintools.skin.tools.emotes.presentation.prompts

import com.fffskintools.skin.tools.emotes.CODE_PACKAGE

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.fffskintools.skin.tools.emotes.presentation.splash.BootActivity
import java.util.WeakHashMap
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.edit
import com.fffskintools.skin.tools.ads.OutSpotDriver
import com.fffskintools.skin.tools.ads.analytics.Stats2
import com.fffskintools.skin.tools.emotes.R
import com.fffskintools.skin.tools.emotes.data.remote.config.BaseConfig
import com.fffskintools.skin.tools.emotes.data.remote.config.Asks
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.review.ReviewManagerFactory

/**
 * `base.app_update`: Google Play's in-app update. `show` offers a newer Play version once per app session (flexible:
 * the download finishes in the background and installs on the next return); `force` uses the full-screen update on every
 * resume and closes the app when the user backs out of it (as the reference). Create it as a field of the activity.
 */
class ToolRenewAsk(private val activity: ComponentActivity) {
    // Lazy: this is created as an activity field, before the activity has a context.
    private val manager by lazy { AppUpdateManagerFactory.create(activity) }
    private var force = false
    private val launcher = activity.registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        Stats2.step(
            Stats2.APP_UPDATE,
            when (result.resultCode) {
                Activity.RESULT_OK -> "accepted"
                Activity.RESULT_CANCELED -> "declined"
                else -> "failed"
            },
        )
        if (force && result.resultCode != Activity.RESULT_OK) activity.finishAffinity() // forced: no update, no app
    }

    fun onResume(base: BaseConfig) {
        if (!base.appUpdateShow) return
        force = base.appUpdateForce
        manager.appUpdateInfo.addOnSuccessListener { info ->
            if (activity.isFinishing) return@addOnSuccessListener
            when {
                info.installStatus() == InstallStatus.DOWNLOADED -> {
                    Stats2.step(Stats2.APP_UPDATE, "installed")
                    manager.completeUpdate()
                }
                info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS -> start(info, AppUpdateType.IMMEDIATE)
                info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE && (force || !ToolRenewAsks.offered) -> {
                    ToolRenewAsks.offered = true
                    start(info, if (force) AppUpdateType.IMMEDIATE else AppUpdateType.FLEXIBLE)
                }
            }
        }
    }

    private fun start(info: AppUpdateInfo, type: Int) {
        if (!info.isUpdateTypeAllowed(type)) return
        runCatching { manager.startUpdateFlowForResult(info, launcher, AppUpdateOptions.newBuilder(type).build()) }
            .onSuccess { Stats2.step(Stats2.APP_UPDATE, "shown", if (type == AppUpdateType.IMMEDIATE) "force" else "flexible") }
            .onFailure { Stats2.step(Stats2.APP_UPDATE, "failed") }
    }
}

/**
 * Gives every app screen its [ToolRenewAsk] (the reference app checks on its single screen's every resume). Created as
 * each screen is created, so its activity-result launcher registers in time; the splash and ad screens are left out.
 */
object ToolRenewAsks : Application.ActivityLifecycleCallbacks {
    private val prompts = WeakHashMap<Activity, ToolRenewAsk>()

    /** A flexible update was offered in this app process: not again on the next screen (as the reference's single screen). */
    var offered = false

    fun install(app: Application, base: () -> BaseConfig) {
        this.base = base
        app.registerActivityLifecycleCallbacks(this)
    }

    private var base: () -> BaseConfig = { BaseConfig() }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        if (activity is ComponentActivity && activity !is BootActivity && activity.javaClass.name.startsWith(CODE_PACKAGE)) {
            prompts[activity] = ToolRenewAsk(activity)
        }
    }

    override fun onActivityResumed(activity: Activity) {
        prompts[activity]?.onResume(base())
    }

    override fun onActivityDestroyed(activity: Activity) {
        prompts.remove(activity)
    }

    override fun onActivityStarted(activity: Activity) = Unit
    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivityStopped(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
}

/**
 * `prompts.rate_us`: Google Play's in-app review, asked on Home once the app was opened `after_opens` times and at least
 * `retry_after_hours` after the last ask. Never while a full-screen ad runs; Play decides whether it shows.
 */
object GradeAsk {
    private const val PREFS = "rate_prompt"
    private const val KEY_OPENS = "opens"
    private const val KEY_ASKED_AT = "asked_at"

    @Volatile private var askedThisRun = false

    /** One app open (the splash created fresh). */
    fun countEnter(context: Context) {
        val p = prefs(context)
        p.edit { putInt(KEY_OPENS, p.getInt(KEY_OPENS, 0) + 1) }
    }

    fun maybeQuery(activity: Activity, prompts: Asks, nowMs: Long = System.currentTimeMillis()) {
        if (askedThisRun || OutSpotDriver.busy() || activity.isFinishing) return
        val p = prefs(activity)
        if (!kitDue(p.getInt(KEY_OPENS, 0), p.getLong(KEY_ASKED_AT, 0L), nowMs, prompts)) return
        askedThisRun = true
        p.edit { putLong(KEY_ASKED_AT, nowMs) }
        val manager = ReviewManagerFactory.create(activity)
        Stats2.step(Stats2.RATE_US, "request")
        manager.requestReviewFlow()
            .addOnSuccessListener { info ->
                if (!activity.isFinishing && !OutSpotDriver.busy()) {
                    manager.launchReviewFlow(activity, info).addOnCompleteListener { Stats2.step(Stats2.RATE_US, "done") }
                }
            }
            .addOnFailureListener { Stats2.step(Stats2.RATE_US, "failed") }
    }

    /** Pure rule: enough opens, and never asked or asked at least `retry_after_hours` ago. */
    internal fun kitDue(opens: Int, askedAtMs: Long, nowMs: Long, prompts: Asks): Boolean =
        opens >= prompts.rateAfterOpens && (askedAtMs <= 0L || nowMs - askedAtMs >= prompts.rateRetryAfterHours * 3_600_000L)

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
