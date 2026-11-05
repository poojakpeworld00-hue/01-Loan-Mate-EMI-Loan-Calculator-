package com.fffskintools.skin.tools.ads.render.interstitial

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.fffskintools.skin.tools.ads.entity.SpotSlotKind
import com.fffskintools.skin.tools.ads.policy.SpotOptionsChest
import com.fffskintools.skin.tools.ads.policy.SpotPullMode
import com.fffskintools.skin.tools.ads.host.SpotAge
import com.fffskintools.skin.tools.ads.host.SpotCues
import com.fffskintools.skin.tools.ads.render.EmbeddedSpotPainter
import com.fffskintools.skin.tools.ads.policy.SpotWaits
import com.fffskintools.skin.tools.ads.render.ProceedSpotLeader

// The "inter" step of an ad chain.
// Preload mode (SpotPullMode: ads.preload / the place's own "preload"): show the preloaded ad if there is one, otherwise skip the step.
// On-load mode: load it now behind a full-screen loader; a failure or ads.loader_wait_sec (SpotWaits) skips the step.
// Each ad leaves the cache before show(), and one load per unit runs at a time, so two chains can't stack two inters.
object ChainOverlay {
    private const val TAG = "ChainInter"

    private val ready = mutableMapOf<String, InterstitialAd>()
    private val readyAt = mutableMapOf<String, Long>()
    private val loading = mutableSetOf<String>()
    private val main = Handler(Looper.getMainLooper())

    // Warms the ad ahead of the step; a no-op in on-load mode, which loads only when the step is reached.
    fun preload(context: Context, unitId: String, placement: String? = null) {
        kitDropStale(unitId)
        if (unitId.isBlank() || unitId in ready || unitId in loading) return
        val pref = SpotOptionsChest.getInstance(context)
        if (!SpotPullMode.preload(context, SpotPullMode.KitFormat.INTER, placement)) return
        // The global unit already sits in NavigationOverlay's preload: don't load it twice.
        if (kitIsGlobal(pref, unitId) && (NavigationOverlay.hasPreloadedGoogle || NavigationOverlay.googleInterLoading)) return
        if (!pref.getBoolean("IsAdsON")) return
        if (SpotSlotKind.fromString(pref.getString("IsAdType")) != SpotSlotKind.GOOGLE) return

        loading += unitId
        SpotCues.send(SpotCues.INTER, "request", "") // a warm-up preload has no place
        InterstitialAd.load(
            context.applicationContext,
            unitId,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    SpotCues.send(SpotCues.INTER, "loaded", "")
                    loading -= unitId
                    keep(unitId, ad)
                    Log.d(TAG, "preloaded $unitId")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    SpotCues.loadFailed(SpotCues.INTER, error.code, "")
                    loading -= unitId
                    Log.e(TAG, "preload failed for $unitId: ${error.message}")
                }
            },
        )
    }

    /** A preloaded ad is waiting for [unitId]. */
    fun kitHasReady(unitId: String): Boolean = unitId in ready

    fun show(
        activity: Activity,
        unitId: String,
        placement: String? = null,
        onMiss: () -> Unit,
        /** true (exit_ad steps): with nothing preloaded, load now behind the loader even in preload mode. */
        loadIfMissing: Boolean = false,
        onDone: () -> Unit,
    ) {
        if (busy(activity) || unitId.isBlank()) return onMiss()

        val pref = SpotOptionsChest.getInstance(activity)
        kitDropStale(unitId)
        val ad = ready.remove(unitId)
            ?: if (kitIsGlobal(pref, unitId)) NavigationOverlay.kitTakePreloadedGoogle() else null
        if (ad != null) return present(activity, unitId, placement, ad, onMiss, onDone)

        if (!loadIfMissing && SpotPullMode.preload(activity, SpotPullMode.KitFormat.INTER, placement)) {
            Log.d(TAG, "no preloaded ad for $unitId — skip")
            preload(activity, unitId, placement)
            return onMiss()
        }
        // Another chain is already loading this unit and will show it: this one skips.
        if (!loadIfMissing && unitId in loading) return onMiss()

        Log.d(TAG, "on-load: loading $unitId behind the loader")
        loading += unitId
        var settled = false
        val place = SpotCues.place
        val timeout = Runnable {
            if (settled) return@Runnable
            settled = true
            SpotCues.send(SpotCues.INTER, "timeout", place)
            OverlayWarmer.hide()
            Log.d(TAG, "on-load: $unitId timed out — skip")
            onMiss()
        }
        OverlayWarmer.show(activity, true)
        main.postDelayed(timeout, pullTimeoutMs(activity))
        SpotCues.send(SpotCues.INTER, "request", place)

        InterstitialAd.load(
            activity,
            unitId,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(loaded: InterstitialAd) {
                    SpotCues.send(SpotCues.INTER, "loaded", place)
                    loading -= unitId
                    if (settled) { keep(unitId, loaded); return } // late: keep it for the next step
                    settled = true
                    main.removeCallbacks(timeout)
                    OverlayWarmer.hide()
                    if (busy(activity)) return onMiss()
                    present(activity, unitId, placement, loaded, onMiss, onDone)
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    SpotCues.loadFailed(SpotCues.INTER, error.code, place)
                    loading -= unitId
                    if (settled) return
                    settled = true
                    main.removeCallbacks(timeout)
                    OverlayWarmer.hide()
                    Log.e(TAG, "on-load failed for $unitId: ${error.message} — skip")
                    onMiss()
                }
            },
        )
    }

    private fun present(
        activity: Activity,
        unitId: String,
        placement: String?,
        ad: InterstitialAd,
        onMiss: () -> Unit,
        onDone: () -> Unit,
    ) {
        var fired = false
        val place = SpotCues.place
        ad.fullScreenContentCallback = object : SpotCues.FullView(SpotCues.INTER, place) {
            override fun onAdShowedFullScreenContent() {
                super.onAdShowedFullScreenContent()
                NavigationOverlay.isInterShow = true
            }

            override fun onAdDismissedFullScreenContent() {
                super.onAdDismissedFullScreenContent()
                NavigationOverlay.isInterShow = false
                preload(activity, unitId, placement)
                if (!fired) { fired = true; onDone() }
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                super.onAdFailedToShowFullScreenContent(error)
                NavigationOverlay.isInterShow = false
                Log.e(TAG, "show failed for $unitId: ${error.message} — skip")
                preload(activity, unitId, placement)
                if (!fired) { fired = true; onMiss() }
            }
        }
        ad.setOnPaidEventListener { SpotCues.paid(it, "inter_chain", ad.adUnitId, ad.responseInfo, place) }
        // Flag it before show(): a second chain arriving before onAdShowedFullScreenContent must skip.
        NavigationOverlay.isInterShow = true
        ad.show(activity)
    }

    private fun busy(activity: Activity) = activity.isFinishing || activity.isDestroyed ||
        NavigationOverlay.isInterShow || ProceedSpotLeader.isShowingAd || EmbeddedSpotPainter.isFullNativeShowing

    internal fun pullTimeoutMs(context: Context): Long = SpotWaits.waitMs(context)

    private fun kitIsGlobal(pref: SpotOptionsChest, unitId: String) = unitId == pref.getString("googleInter").orEmpty()

    private fun keep(unitId: String, ad: InterstitialAd) {
        ready[unitId] = ad
        readyAt[unitId] = SpotAge.now()
    }

    private fun kitDropStale(unitId: String) {
        if (unitId in ready && SpotAge.stale(readyAt[unitId] ?: 0L, SpotAge.FULL_SCREEN_MS)) ready.remove(unitId)
    }
}
