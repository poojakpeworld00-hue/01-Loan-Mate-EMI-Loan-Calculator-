package com.loanmate.loan.emicalculator.carloan.homelloan.screens.nudges

import com.loanmate.loan.emicalculator.carloan.homelloan.CODE_PACKAGE

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.loanmate.loan.emicalculator.carloan.homelloan.screens.intro.StartActivity
import java.util.WeakHashMap
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.edit
import com.loanmate.loan.emicalculator.carloan.adkit.ExitPromoRunner
import com.loanmate.loan.emicalculator.carloan.adkit.metrics.MetricsTwo
import com.loanmate.loan.emicalculator.carloan.homelloan.R
import com.loanmate.loan.emicalculator.carloan.homelloan.store.cloud.flags.BaseConfig
import com.loanmate.loan.emicalculator.carloan.homelloan.store.cloud.flags.Asks
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
class LoanUpdateQuery(private val activity: ComponentActivity) {
    // Lazy: this is created as an activity field, before the activity has a context.
    private val manager by lazy { AppUpdateManagerFactory.create(activity) }
    private var force = false
    private val launcher = activity.registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        MetricsTwo.step(
            MetricsTwo.APP_UPDATE,
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
                    MetricsTwo.step(MetricsTwo.APP_UPDATE, "installed")
                    manager.completeUpdate()
                }
                info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS -> start(info, AppUpdateType.IMMEDIATE)
                info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE && (force || !LoanUpdateQueries.offered) -> {
                    LoanUpdateQueries.offered = true
                    start(info, if (force) AppUpdateType.IMMEDIATE else AppUpdateType.FLEXIBLE)
                }
            }
        }
    }

    private fun start(info: AppUpdateInfo, type: Int) {
        if (!info.isUpdateTypeAllowed(type)) return
        runCatching { manager.startUpdateFlowForResult(info, launcher, AppUpdateOptions.newBuilder(type).build()) }
            .onSuccess { MetricsTwo.step(MetricsTwo.APP_UPDATE, "shown", if (type == AppUpdateType.IMMEDIATE) "force" else "flexible") }
            .onFailure { MetricsTwo.step(MetricsTwo.APP_UPDATE, "failed") }
    }
}

/**
 * Gives every app screen its [LoanUpdateQuery] (the reference app checks on its single screen's every resume). Created as
 * each screen is created, so its activity-result launcher registers in time; the splash and ad screens are left out.
 */
object LoanUpdateQueries : Application.ActivityLifecycleCallbacks {
    private val prompts = WeakHashMap<Activity, LoanUpdateQuery>()

    /** A flexible update was offered in this app process: not again on the next screen (as the reference's single screen). */
    var offered = false

    fun install(app: Application, base: () -> BaseConfig) {
        this.base = base
        app.registerActivityLifecycleCallbacks(this)
    }

    private var base: () -> BaseConfig = { BaseConfig() }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        if (activity is ComponentActivity && activity !is StartActivity && activity.javaClass.name.startsWith(CODE_PACKAGE)) {
            prompts[activity] = LoanUpdateQuery(activity)
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
object RateQuery {
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
        if (askedThisRun || ExitPromoRunner.busy() || activity.isFinishing) return
        val p = prefs(activity)
        if (!kitDue(p.getInt(KEY_OPENS, 0), p.getLong(KEY_ASKED_AT, 0L), nowMs, prompts)) return
        askedThisRun = true
        p.edit { putLong(KEY_ASKED_AT, nowMs) }
        val manager = ReviewManagerFactory.create(activity)
        MetricsTwo.step(MetricsTwo.RATE_US, "request")
        manager.requestReviewFlow()
            .addOnSuccessListener { info ->
                if (!activity.isFinishing && !ExitPromoRunner.busy()) {
                    manager.launchReviewFlow(activity, info).addOnCompleteListener { MetricsTwo.step(MetricsTwo.RATE_US, "done") }
                }
            }
            .addOnFailureListener { MetricsTwo.step(MetricsTwo.RATE_US, "failed") }
    }

    /** Pure rule: enough opens, and never asked or asked at least `retry_after_hours` ago. */
    internal fun kitDue(opens: Int, askedAtMs: Long, nowMs: Long, prompts: Asks): Boolean =
        opens >= prompts.rateAfterOpens && (askedAtMs <= 0L || nowMs - askedAtMs >= prompts.rateRetryAfterHours * 3_600_000L)

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
