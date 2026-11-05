package com.fffskintools.skin.tools.ads.render

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.appopen.AppOpenAd.AppOpenAdLoadCallback
import com.fffskintools.skin.tools.ads.entity.SpotSlotKind
import com.fffskintools.skin.tools.ads.policy.SpotPullMode
import com.fffskintools.skin.tools.ads.policy.SpotOptionsChest
import com.fffskintools.skin.tools.ads.policy.SpotWaits
import com.fffskintools.skin.tools.ads.render.interstitial.OverlayWarmer
import com.fffskintools.skin.tools.ads.render.interstitial.NavigationOverlay
import com.fffskintools.skin.tools.ads.host.SpotAge
import com.fffskintools.skin.tools.ads.host.SpotCues

object ProceedSpotLeader {
    private const val LOG_TAG = "ResumeAdDirector"
    private var appOpenAd: AppOpenAd? = null
    private var isLoadingAd = false
    var isShowingAd: Boolean = false
    var callbackshow:Boolean = false
    var isOpenAppDismiss: Boolean = false

    var skipNextAppOpenAd: Boolean = false

    private var flagReturnAd: Boolean = false

    fun flagReturnAd() {
        flagReturnAd = true
    }

    fun takeReturnSpotFlag(): Boolean {
        val v = flagReturnAd
        flagReturnAd = false
        return v
    }

    private var foregroundCounter = 0

    private var loadedAt = 0L

    val isAdAvailable: Boolean
        get() {
            if (appOpenAd != null && SpotAge.stale(loadedAt, SpotAge.APP_OPEN_MS)) appOpenAd = null
            return appOpenAd != null
        }

    /**
     * Warms the app-open ad. On-load app-open (SpotPullMode) warms nothing unless [force]d by the
     * moment that is about to show it; [onResult] then hears whether an ad is ready.
     */
    fun loadAd(context: Context?, force: Boolean = false, onResult: ((Boolean) -> Unit)? = null) {
        if (isAdAvailable) {
            onResult?.invoke(true)
            return
        }
        if (isLoadingAd || context == null) {
            onResult?.invoke(false)
            return
        }
        if (!force && !SpotPullMode.preload(context, SpotPullMode.KitFormat.APP_OPEN)) return
        val adsPreference = SpotOptionsChest.getInstance(context)

        if (!adsPreference.getBoolean("AppopenAds")) {
            Log.e(LOG_TAG, "AppopenAds disabled by Firebase flag")
            onResult?.invoke(false)
            return
        }
        // A forced load is for the place about to show it; a warm-up preload has none.
        val place = if (force) SpotCues.place else ""
        if (SpotSlotKind.fromString(adsPreference.getString("IsAdType")) == SpotSlotKind.GOOGLE) {
            isLoadingAd = true
            val request = AdRequest.Builder().build()

            if (adsPreference.getString("IsAdType").equals("Google", true)) {
                SpotOptionsChest.getInstance(context).getString("googleAppopen")?.let { adUnitId ->
                    SpotCues.send(SpotCues.APP_OPEN, "request", place)
                    AppOpenAd.load(
                        context, adUnitId, request, object : AppOpenAdLoadCallback() {
                            override fun onAdLoaded(ad: AppOpenAd) {
                                SpotCues.send(SpotCues.APP_OPEN, "loaded", place)

                                Log.d(LOG_TAG, "Ad was loaded.")
                                appOpenAd = ad
                                loadedAt = SpotAge.now()
                                isLoadingAd = false
                                onResult?.invoke(true)
                            }

                            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                                Log.e(
                                    LOG_TAG, "Ad failed to load: ${loadAdError.message}"
                                )

                                SpotCues.loadFailed(SpotCues.APP_OPEN, loadAdError.code, place)
                                isLoadingAd = false
                                onResult?.invoke(false)
                            }
                        })
                } ?: run {
                    isLoadingAd = false
                    onResult?.invoke(false)
                }
            } else {
                isLoadingAd = false
                onResult?.invoke(false)
            }
        } else {
            onResult?.invoke(false)
            return
        }
    }

    /**
     * On-load app-open: load now behind the full-screen loader, then [onReady]; [onMiss] if the
     * load fails or takes longer than ads.loader_wait_sec. A late ad stays for next time.
     */
    private fun pullThenPresent(activity: Activity, onReady: () -> Unit, onMiss: () -> Unit) {
        OverlayWarmer.show(activity, true)
        val wait = SpotWaits.main(activity)
        wait.start {
            OverlayWarmer.hide()
            Log.d(LOG_TAG, "on-load: app open timed out — skip")
            SpotCues.send(SpotCues.APP_OPEN, "timeout", SpotCues.place)
            onMiss()
        }
        loadAd(activity, force = true) { ok ->
            if (!wait.kitArrived()) return@loadAd
            OverlayWarmer.hide()
            if (ok && !activity.isFinishing && !activity.isDestroyed) onReady() else onMiss()
        }
    }


    private fun presentReady(activity: Activity, onShowAdCompleteListener: SpotDismissListener) {
        if (!isAdAvailable || isShowingAd) return onShowAdCompleteListener.onPresentSpotComplete()

        val place = SpotCues.place
        appOpenAd?.fullScreenContentCallback = object : SpotCues.FullView(SpotCues.APP_OPEN, place) {
            override fun onAdDismissedFullScreenContent() {
                super.onAdDismissedFullScreenContent()
                Log.e(
                    LOG_TAG, "Ad dismissed fullscreen content."
                )

                appOpenAd = null
                isShowingAd = false
                isOpenAppDismiss = true
                onShowAdCompleteListener.onPresentSpotComplete()
                loadAd(
                    activity
                )
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                super.onAdFailedToShowFullScreenContent(adError)
                Log.e(
                    LOG_TAG, adError.message
                )
                appOpenAd = null
                isShowingAd = false
                onShowAdCompleteListener.onPresentSpotComplete()
                loadAd(
                    activity
                )
            }

            override fun onAdShowedFullScreenContent() {
                super.onAdShowedFullScreenContent()
                Log.e(
                    LOG_TAG, "Ad showed fullscreen content."
                )
            }
        }

        val ad = appOpenAd!!
        ad.setOnPaidEventListener { SpotCues.paid(it, "app_open_resume", ad.adUnitId, ad.responseInfo, place) }

        isShowingAd = true

        if (SpotOptionsChest.getInstance(activity).getString("IsAdType").equals("Google", true)) {
            appOpenAd?.show(activity)
        }
    }

    /** [loadIfMissing] (exit_ad steps): with nothing preloaded, load now behind the loader even in preload mode. */
    fun presentLoaded(activity: Activity, onShown: () -> Unit, onMiss: () -> Unit, loadIfMissing: Boolean = false) {
        val ad = appOpenAd
        val google = SpotOptionsChest.getInstance(activity).getString("IsAdType").equals("Google", true)
        if (ad == null && google && !isShowingAd && !activity.isFinishing && !activity.isDestroyed &&
            (loadIfMissing || !SpotPullMode.preload(activity, SpotPullMode.KitFormat.APP_OPEN))
        ) {
            // On-load app-open as a chain step: load it now; a miss moves to the next step.
            return pullThenPresent(
                activity,
                onReady = { if (appOpenAd != null) presentLoaded(activity, onShown, onMiss) else onMiss() },
                onMiss = onMiss,
            )
        }
        if (ad == null || isShowingAd || activity.isFinishing || activity.isDestroyed || !google) {
            loadAd(activity)
            return onMiss()
        }
        val place = SpotCues.place
        ad.fullScreenContentCallback = object : SpotCues.FullView(SpotCues.APP_OPEN, place) {
            override fun onAdDismissedFullScreenContent() {
                super.onAdDismissedFullScreenContent()
                appOpenAd = null
                isShowingAd = false
                onShown()
                loadAd(activity)
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                super.onAdFailedToShowFullScreenContent(adError)
                appOpenAd = null
                isShowingAd = false
                onMiss()
                loadAd(activity)
            }
        }
        ad.setOnPaidEventListener { SpotCues.paid(it, "app_open", ad.adUnitId, ad.responseInfo, place) }
        isShowingAd = true
        ad.show(activity)
    }

    interface SpotDismissListener {
        fun onPresentSpotComplete()
    }
}
