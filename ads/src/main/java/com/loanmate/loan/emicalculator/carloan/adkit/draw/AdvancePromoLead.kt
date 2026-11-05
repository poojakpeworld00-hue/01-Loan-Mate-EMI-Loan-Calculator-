package com.loanmate.loan.emicalculator.carloan.adkit.draw

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.appopen.AppOpenAd.AppOpenAdLoadCallback
import com.loanmate.loan.emicalculator.carloan.adkit.model.PromoBayType
import com.loanmate.loan.emicalculator.carloan.adkit.rules.PromoFetchStyle
import com.loanmate.loan.emicalculator.carloan.adkit.rules.PromoSettingsBox
import com.loanmate.loan.emicalculator.carloan.adkit.rules.PromoDelays
import com.loanmate.loan.emicalculator.carloan.adkit.draw.fullscreen.LayerPrimer
import com.loanmate.loan.emicalculator.carloan.adkit.draw.fullscreen.NavLayer
import com.loanmate.loan.emicalculator.carloan.adkit.bridge.PromoEra
import com.loanmate.loan.emicalculator.carloan.adkit.bridge.PromoSignals

object AdvancePromoLead {
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
            if (appOpenAd != null && PromoEra.stale(loadedAt, PromoEra.APP_OPEN_MS)) appOpenAd = null
            return appOpenAd != null
        }

    /**
     * Warms the app-open ad. On-load app-open (PromoFetchStyle) warms nothing unless [force]d by the
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
        if (!force && !PromoFetchStyle.preload(context, PromoFetchStyle.PackShape.APP_OPEN)) return
        val adsPreference = PromoSettingsBox.getInstance(context)

        if (!adsPreference.getBoolean("AppopenAds")) {
            Log.e(LOG_TAG, "AppopenAds disabled by Firebase flag")
            onResult?.invoke(false)
            return
        }
        // A forced load is for the place about to show it; a warm-up preload has none.
        val place = if (force) PromoSignals.place else ""
        if (PromoBayType.fromString(adsPreference.getString("IsAdType")) == PromoBayType.GOOGLE) {
            isLoadingAd = true
            val request = AdRequest.Builder().build()

            if (adsPreference.getString("IsAdType").equals("Google", true)) {
                PromoSettingsBox.getInstance(context).getString("googleAppopen")?.let { adUnitId ->
                    PromoSignals.send(PromoSignals.APP_OPEN, "request", place)
                    AppOpenAd.load(
                        context, adUnitId, request, object : AppOpenAdLoadCallback() {
                            override fun onAdLoaded(ad: AppOpenAd) {
                                PromoSignals.send(PromoSignals.APP_OPEN, "loaded", place)

                                Log.d(LOG_TAG, "Ad was loaded.")
                                appOpenAd = ad
                                loadedAt = PromoEra.now()
                                isLoadingAd = false
                                onResult?.invoke(true)
                            }

                            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                                Log.e(
                                    LOG_TAG, "Ad failed to load: ${loadAdError.message}"
                                )

                                PromoSignals.loadFailed(PromoSignals.APP_OPEN, loadAdError.code, place)
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
        LayerPrimer.show(activity, true)
        val wait = PromoDelays.main(activity)
        wait.start {
            LayerPrimer.hide()
            Log.d(LOG_TAG, "on-load: app open timed out — skip")
            PromoSignals.send(PromoSignals.APP_OPEN, "timeout", PromoSignals.place)
            onMiss()
        }
        loadAd(activity, force = true) { ok ->
            if (!wait.kitArrived()) return@loadAd
            LayerPrimer.hide()
            if (ok && !activity.isFinishing && !activity.isDestroyed) onReady() else onMiss()
        }
    }


    private fun presentReady(activity: Activity, onShowAdCompleteListener: PromoCloseListener) {
        if (!isAdAvailable || isShowingAd) return onShowAdCompleteListener.onPresentSpotComplete()

        val place = PromoSignals.place
        appOpenAd?.fullScreenContentCallback = object : PromoSignals.WholeScreen(PromoSignals.APP_OPEN, place) {
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
        ad.setOnPaidEventListener { PromoSignals.paid(it, "app_open_resume", ad.adUnitId, ad.responseInfo, place) }

        isShowingAd = true

        if (PromoSettingsBox.getInstance(activity).getString("IsAdType").equals("Google", true)) {
            appOpenAd?.show(activity)
        }
    }

    /** [loadIfMissing] (exit_ad steps): with nothing preloaded, load now behind the loader even in preload mode. */
    fun presentLoaded(activity: Activity, onShown: () -> Unit, onMiss: () -> Unit, loadIfMissing: Boolean = false) {
        val ad = appOpenAd
        val google = PromoSettingsBox.getInstance(activity).getString("IsAdType").equals("Google", true)
        if (ad == null && google && !isShowingAd && !activity.isFinishing && !activity.isDestroyed &&
            (loadIfMissing || !PromoFetchStyle.preload(activity, PromoFetchStyle.PackShape.APP_OPEN))
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
        val place = PromoSignals.place
        ad.fullScreenContentCallback = object : PromoSignals.WholeScreen(PromoSignals.APP_OPEN, place) {
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
        ad.setOnPaidEventListener { PromoSignals.paid(it, "app_open", ad.adUnitId, ad.responseInfo, place) }
        isShowingAd = true
        ad.show(activity)
    }

    interface PromoCloseListener {
        fun onPresentSpotComplete()
    }
}
