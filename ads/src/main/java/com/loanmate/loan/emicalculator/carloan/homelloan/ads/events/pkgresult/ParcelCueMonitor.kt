package com.loanmate.loan.emicalculator.carloan.homelloan.ads.events.pkgresult

import com.loanmate.loan.emicalculator.carloan.homelloan.ads.SpotKit
import com.loanmate.loan.emicalculator.carloan.homelloan.ads.Spots
import com.loanmate.loan.emicalculator.carloan.homelloan.ads.PlaceGuards
import android.app.Activity
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import java.lang.ref.WeakReference
import kotlin.concurrent.thread

object ParcelCueMonitor {

    private const val TAG = "PackageEvent"

    private const val PENDING_FILE = "package_event_pending"
    private const val KEY_INSTALLED = "pending_installed"
    private const val KEY_PACKAGE = "pending_package"
    private const val KEY_TIME = "pending_time"
    private const val KEY_LAST_SHOWN = "last_shown_at"

    private const val PENDING_TTL_MS = 10 * 60 * 1000L

    private var registered = false

    private var resumed = WeakReference<Activity>(null)

    fun register(app: Application) {
        if (registered) return
        registered = true
        app.registerActivityLifecycleCallbacks(KitCallbacks)

        ContextCompat.registerReceiver(
            app,
            Receiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_PACKAGE_ADDED)
                addAction(Intent.ACTION_PACKAGE_REMOVED)
                addDataScheme("package")
            },
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )

    }

    private var seeded = false

    /** Caches labels / icons of installed apps for the uninstall screen, only while `app_uninstalled` is on. */
    internal fun kitSeedIfNeeded(app: Context) {
        if (seeded || !Spots.kitPlaceOn(Spots.APP_UNINSTALLED)) return
        seeded = true
        thread { ParcelMetadataCache.kitSeedAll(app) }
    }

    private object Receiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val pkg = intent.data?.schemeSpecificPart
            val app = context.applicationContext

            if (intent.action == Intent.ACTION_PACKAGE_ADDED && !pkg.isNullOrBlank()) {
                thread { ParcelMetadataCache.remember(app, pkg) }
            }

            if (intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) return
            if (pkg.isNullOrBlank() || pkg == app.packageName) return

            val installed = when (intent.action) {
                Intent.ACTION_PACKAGE_ADDED -> true
                Intent.ACTION_PACKAGE_REMOVED -> false
                else -> return
            }
            Spots.parcelChanged(app, installed, pkg)
        }
    }

    internal fun onEvent(context: Context, installed: Boolean, pkg: String) {
        runCatching {
            if (!kitGatesPass(context, installed)) return
            val activity = resumed.get()
            if (activity != null && !activity.isFinishing && !activity.isDestroyed) {
                fire(activity, installed, pkg)
            } else {
                enqueue(context, installed, pkg)
                Log.d(TAG, "queued $pkg (installed=$installed) — no foreground screen")
            }
        }.onFailure { Log.w(TAG, "onEvent($pkg) failed", it); SpotKit.onError(it, "package.event") }
    }

    private fun fire(activity: Activity, installed: Boolean, pkg: String) {
        prefs(activity).edit { putLong(KEY_LAST_SHOWN, System.currentTimeMillis()) }
        activity.startActivity(
            Intent(activity, ParcelYieldActivity::class.java)
                .putExtra(ParcelYieldActivity.EXTRA_INSTALLED, installed)
                .putExtra(ParcelYieldActivity.EXTRA_PACKAGE, pkg)
        )
        Log.d(TAG, "showed $pkg (installed=$installed) from ${activity.javaClass.simpleName}")

        if (!installed) {
            val app = activity.applicationContext
            activity.window.decorView.postDelayed({
                thread { ParcelMetadataCache.kitForget(app, pkg) }
            }, FORGET_DELAY_MS)
        }
    }

    private fun kitGatesPass(context: Context, installed: Boolean): Boolean {
        val placeKey = if (installed) Spots.APP_INSTALLED else Spots.APP_UNINSTALLED
        if (!Spots.kitPlaceOn(placeKey)) return false
        val gapSec = (SpotKit.config.places[placeKey]?.int("gap_sec") ?: 0).toLong()
        val passed = PlaceGuards.kitGapPassed(prefs(context).getLong(KEY_LAST_SHOWN, 0L), gapSec, System.currentTimeMillis())
        if (!passed) Log.d(TAG, "throttled (gap_sec $gapSec)")
        return passed
    }

    private fun enqueue(context: Context, installed: Boolean, pkg: String) {
        prefs(context).edit {
            putBoolean(KEY_INSTALLED, installed)
            putString(KEY_PACKAGE, pkg)
            putLong(KEY_TIME, System.currentTimeMillis())
        }
    }

    private fun takePending(context: Context): Pair<Boolean, String>? {
        val store = prefs(context)
        val pkg = store.getString(KEY_PACKAGE, null) ?: return null
        val installed = store.getBoolean(KEY_INSTALLED, true)
        val at = store.getLong(KEY_TIME, 0L)
        store.edit { remove(KEY_INSTALLED); remove(KEY_PACKAGE); remove(KEY_TIME) }
        if (System.currentTimeMillis() - at > PENDING_TTL_MS) return null
        return installed to pkg
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PENDING_FILE, Context.MODE_PRIVATE)

    private object KitCallbacks : Application.ActivityLifecycleCallbacks {
        override fun onActivityResumed(activity: Activity) {
            if (activity is ParcelYieldActivity) return
            resumed = WeakReference(activity)

            val (installed, pkg) = takePending(activity.applicationContext) ?: return
            runCatching {
                if (kitGatesPass(activity.applicationContext, installed)) fire(activity, installed, pkg)
            }.onFailure { Log.w(TAG, "drain($pkg) failed", it); SpotKit.onError(it, "package.drain") }
        }

        override fun onActivityPaused(activity: Activity) {
            if (resumed.get() === activity) resumed.clear()
        }

        override fun onActivityDestroyed(activity: Activity) {
            if (resumed.get() === activity) resumed.clear()
        }

        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
        override fun onActivityStarted(activity: Activity) = Unit
        override fun onActivityStopped(activity: Activity) = Unit
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    }

    private const val FORGET_DELAY_MS = 5_000L
}
