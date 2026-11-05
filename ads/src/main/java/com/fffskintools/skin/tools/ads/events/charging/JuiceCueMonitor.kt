package com.fffskintools.skin.tools.ads.events.charging

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
import com.fffskintools.skin.tools.ads.SpotKit
import com.fffskintools.skin.tools.ads.Spots
import com.fffskintools.skin.tools.ads.PlaceGuards
import java.lang.ref.WeakReference

/**
 * Charger plugged / unplugged → [BatteryStatusActivity] over the screen in front (RC `launcher.charger_connected`
 * / `charger_disconnected`: on, gap_sec). With no screen of ours in front the event waits up to 10 min for one.
 */
object JuiceCueMonitor {

    private const val TAG = "ChargeEvent"

    private const val PREFS = "charge_event_watcher"
    private const val KEY_PENDING = "pending_trigger"
    private const val KEY_PENDING_AT = "pending_at"

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
                addAction(Intent.ACTION_POWER_CONNECTED)
                addAction(Intent.ACTION_POWER_DISCONNECTED)
            },
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }

    private object Receiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_POWER_CONNECTED -> Spots.kitChargerChanged(context, connected = true)
                Intent.ACTION_POWER_DISCONNECTED -> Spots.kitChargerChanged(context, connected = false)
            }
        }
    }

    /** [placeKey] = [Spots.CHARGER_CONNECTED] or [Spots.CHARGER_DISCONNECTED]. */
    internal fun onEvent(context: Context, placeKey: String) {
        runCatching {
            if (!kitGatesPass(context, placeKey)) return
            val activity = resumed.get()
            if (activity != null && !activity.isFinishing && !activity.isDestroyed) {
                fire(activity, placeKey)
            } else {
                prefs(context).edit {
                    putString(KEY_PENDING, placeKey)
                    putLong(KEY_PENDING_AT, System.currentTimeMillis())
                }
                Log.d(TAG, "'$placeKey' queued — no foreground screen")
            }
        }.onFailure { Log.w(TAG, "onEvent('$placeKey') failed", it); SpotKit.onError(it, "charging.event") }
    }

    private fun fire(activity: Activity, placeKey: String) {
        prefs(activity).edit { putLong(lastKey(placeKey), System.currentTimeMillis()) }
        activity.startActivity(
            Intent(activity, BatteryStatusActivity::class.java)
                .putExtra(BatteryStatusActivity.EXTRA_PLACE, placeKey)
        )
        Log.d(TAG, "showed '$placeKey' from ${activity.javaClass.simpleName}")
    }

    private fun kitGatesPass(context: Context, placeKey: String): Boolean {
        if (!Spots.kitPlaceOn(placeKey)) return false
        val gapSec = (SpotKit.config.places[placeKey]?.int("gap_sec") ?: 0).toLong()
        val passed = PlaceGuards.kitGapPassed(prefs(context).getLong(lastKey(placeKey), 0L), gapSec, System.currentTimeMillis())
        if (!passed) Log.d(TAG, "'$placeKey' throttled (gap_sec $gapSec)")
        return passed
    }

    private fun takePending(context: Context): String? {
        val store = prefs(context)
        val placeKey = store.getString(KEY_PENDING, null) ?: return null
        val at = store.getLong(KEY_PENDING_AT, 0L)
        store.edit { remove(KEY_PENDING); remove(KEY_PENDING_AT) }
        return placeKey.takeIf { System.currentTimeMillis() - at <= PENDING_TTL_MS }
    }

    private fun lastKey(placeKey: String) = "last_$placeKey"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private object KitCallbacks : Application.ActivityLifecycleCallbacks {
        override fun onActivityResumed(activity: Activity) {
            if (activity is BatteryStatusActivity) return
            resumed = WeakReference(activity)
            val placeKey = takePending(activity.applicationContext) ?: return
            runCatching {
                if (kitGatesPass(activity.applicationContext, placeKey)) fire(activity, placeKey)
            }.onFailure { Log.w(TAG, "drain('$placeKey') failed", it); SpotKit.onError(it, "charging.drain") }
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
}
