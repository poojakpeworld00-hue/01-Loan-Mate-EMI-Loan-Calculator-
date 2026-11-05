package com.loanmate.loan.emicalculator.carloan.adkit.triggers.recall

import android.app.Activity
import android.app.ActivityManager
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import androidx.core.content.ContextCompat
import com.loanmate.loan.emicalculator.carloan.adkit.PromoPack
import com.loanmate.loan.emicalculator.carloan.adkit.Spots
import com.loanmate.loan.emicalculator.carloan.adkit.BuildConfig
import com.loanmate.loan.emicalculator.carloan.adkit.SlotChecks
import com.loanmate.loan.emicalculator.carloan.adkit.triggers.power.CellStateActivity
import com.loanmate.loan.emicalculator.carloan.adkit.triggers.installs.PackageResultActivity
import com.loanmate.loan.emicalculator.carloan.adkit.rules.PromoSettingsBox
import java.lang.ref.WeakReference

/**
 * Recents key on one of our screens, RC `launcher.play_store_return`:
 * `on` + inside `window_sec` after install → the first press opens our Play Store page (once);
 * otherwise `show_ad_screen` → [LatestPromoActivity] waits for the user when they come back (`gap_sec` apart).
 */
object LatestPromoTracker {

    private const val TAG = "RecentAd"

    private const val GATE_KEY = "__recent_ad_last_shown"

    private const val ARM_DEBOUNCE_MS = 700L

    private var top = WeakReference<Activity>(null)

    private var armed = WeakReference<Activity>(null)

    private var lastArmUptime = 0L
    private var registered = false

    fun register(app: Application) {
        if (registered) return
        registered = true

        app.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityResumed(activity: Activity) {
                top = WeakReference(activity)
                topResumed = true
            }

            override fun onActivityPaused(activity: Activity) {
                if (top.get() === activity) topResumed = false
                if (armed.get() === activity) {
                    armed.clear()
                    if (armedForPlayStore) {
                        armedForPlayStore = false
                        launchPlayHolderFrom(activity)
                    } else {
                        launchPromoLeaf(activity)
                    }
                }
            }

            override fun onActivityDestroyed(activity: Activity) {
                if (top.get() === activity) top.clear()
                if (armed.get() === activity) armed.clear()
            }

            override fun onActivityCreated(activity: Activity, state: Bundle?) = Unit
            override fun onActivityStarted(activity: Activity) = Unit
            override fun onActivityStopped(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, state: Bundle) = Unit
        })

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action != Intent.ACTION_CLOSE_SYSTEM_DIALOGS) return
                when (intent.getStringExtra("reason")) {
                    "recentapps", "recent_apps" -> Spots.runHolderResult(context)
                    "homekey" -> kitDisarm()
                }
            }
        }
        ContextCompat.registerReceiver(
            app,
            receiver,
            IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS),
            ContextCompat.RECEIVER_EXPORTED,
        )
    }

    /** A Recents press (see the class doc). */
    internal fun onToggle(context: Context) {
        if (!armPlayHolderIfLaunchSession(context)) kitArmIfAllowed(context)
    }

    private fun kitArmIfAllowed(context: Context) {
        val now = SystemClock.uptimeMillis()
        if (now - lastArmUptime < ARM_DEBOUNCE_MS) return
        lastArmUptime = now

        val activity = top.get() ?: return
        if (activity.isFinishing || activity.isDestroyed) return
        if (activity is LatestPromoActivity || activity is PackageResultActivity || activity is CellStateActivity) return
        if (!PromoPack.recentsAdScreenOn(activity)) return

        if (!PromoPack.spotsOn()) return
        val place = PromoPack.config.places[Spots.PLAY_STORE_RETURN] ?: return
        if (!place.bool("show_ad_screen")) return

        val vault = PromoSettingsBox.getInstance(context)
        val gapSec = place.int("gap_sec").toLong()
        if (!SlotChecks.kitGapPassed(vault.getLong(GATE_KEY, 0L), gapSec, System.currentTimeMillis())) {
            log("throttled — gap_sec $gapSec")
            return
        }

        armed = WeakReference(activity)
        log("armed ${activity::class.java.simpleName}")
    }

    private var topResumed = false

    private var armedForPlayStore = false

    private val main = android.os.Handler(android.os.Looper.getMainLooper())

    private const val PLAY_STORE_FIRED_KEY = "__recent_playstore_fired"

    /** Some launchers (Pixel) keep the screen live behind Recents, so it never pauses: the jump then starts after this. */
    private const val NO_PAUSE_MS = 250L

    /** A press this soon after the jump started still belongs to it. */
    private const val CLAIM_MS = 2_000L

    private var firedAt = 0L

    /**
     * Whether a Recents press right now is the Play Store jump's (due, waiting for the screen to pause, or just started):
     * the app's own Recents handling then stands down.
     */
    fun kitClaimsPress(context: Context): Boolean =
        armedForPlayStore || SystemClock.uptimeMillis() - firedAt < CLAIM_MS || runHolderDue(context)

    /** `on`, not fired yet and inside `window_sec` after install. */
    fun runHolderDue(context: Context): Boolean = runCatching {
        if (!Spots.kitSlotOn(Spots.PLAY_STORE_RETURN)) return false
        val vault = PromoSettingsBox.getInstance(context)
        if (vault.getBoolean(PLAY_STORE_FIRED_KEY)) return false
        val windowSec = (PromoPack.config.places[Spots.PLAY_STORE_RETURN]?.int("window_sec", 180) ?: 180).toLong()
        val installedAt = context.packageManager.getPackageInfo(context.packageName, 0).firstInstallTime
        SlotChecks.kitWithinWindow(System.currentTimeMillis(), installedAt, windowSec)
    }.getOrDefault(false)

    /** Home was pressed: a Play Store jump armed by an earlier Recents press must not fire. */
    fun kitDisarm() {
        armed.clear()
        armedForPlayStore = false
    }

    private fun armPlayHolderIfLaunchSession(context: Context): Boolean {
        return runCatching {
            if (!runHolderDue(context)) return false
            val activity = top.get() ?: return false
            if (activity.isFinishing || activity.isDestroyed || armedForPlayStore) return false

            if (!topResumed) {
                log("play store: top already paused — starting now")
                launchPlayHolderFrom(activity)
                return true
            }
            armedForPlayStore = true
            armed = WeakReference(activity)
            log("play store: armed on ${activity::class.java.simpleName}")
            main.postDelayed({
                if (armedForPlayStore) {
                    armedForPlayStore = false
                    armed.clear()
                    if (!activity.isFinishing && !activity.isDestroyed) {
                        log("play store: no pause (screen live behind Recents) — starting now")
                        launchPlayHolderFrom(activity)
                    }
                }
            }, NO_PAUSE_MS)
            true
        }.getOrElse { Log.w(TAG, "play_store_return failed", it); PromoPack.onError(it, "recent.play_store_return"); false }
    }

    private fun launchPlayHolderFrom(activity: Activity) {
        runCatching {
            // Play Store's home page, never our own listing: from our page, uninstalling us is one tap away.
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setPackage("com.android.vending")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            Spots.kitMarkOwnLaunch()
            // Recents put the home screen in front of our task; with Play Store on top it would sit behind home, and Android
            // trims a hidden (excludeFromRecents) task there: the screen the user comes back to would be gone. Ours goes
            // back in front first, so the order is Play Store, us, home.
            runCatching { activity.getSystemService(ActivityManager::class.java)?.moveTaskToFront(activity.taskId, 0) }
            runCatching { activity.startActivity(intent) }.getOrElse {
                activity.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
            PromoSettingsBox.getInstance(activity).putBoolean(PLAY_STORE_FIRED_KEY, true)
            firedAt = SystemClock.uptimeMillis()
            log("play store: fired (one-shot spent)")
        }.onFailure {
            Spots.restoreOwnLaunch()
            Log.w(TAG, "launchPlayStoreFrom failed", it)
            PromoPack.onError(it, "recent.play_store")
        }
    }

    private fun launchPromoLeaf(from: Activity) {
        runCatching {
            PromoSettingsBox.getInstance(from).putLong(GATE_KEY, System.currentTimeMillis())
            from.startActivity(
                Intent(from, LatestPromoActivity::class.java).addFlags(
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                )
            )
            log("launched the recent page")
        }.onFailure { Log.e(TAG, "could not launch the recent page", it); PromoPack.onError(it, "recent.page") }
    }

    private fun log(message: String) {
        if (BuildConfig.DEBUG) Log.d(TAG, message)
    }
}
