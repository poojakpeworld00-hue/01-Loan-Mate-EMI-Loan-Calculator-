package com.loanmate.loan.emicalculator.carloan.adkit.draw

import com.loanmate.loan.emicalculator.carloan.adkit.bridge.PromoEra
import com.loanmate.loan.emicalculator.carloan.adkit.bridge.PromoSignals
import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.loanmate.loan.emicalculator.carloan.adkit.rules.PromoFetchStyle
import com.loanmate.loan.emicalculator.carloan.adkit.rules.PromoSettingsBox
import com.loanmate.loan.emicalculator.carloan.adkit.rules.PromoDelays
import com.loanmate.loan.emicalculator.carloan.adkit.draw.fullscreen.LayerPrimer
import com.loanmate.loan.emicalculator.carloan.adkit.draw.fullscreen.NavLayer

class RewardPromoRenderer {
    companion object {
        private var loadedAd: RewardedAd? = null
        private var loadedAt = 0L
        val isLoaded: Boolean get() = kitDropStale() != null

        private fun kitDropStale(): RewardedAd? {
            if (loadedAd != null && PromoEra.stale(loadedAt, PromoEra.FULL_SCREEN_MS)) loadedAd = null
            return loadedAd
        }
        private var isLoading = false

        /**
         * Warms the rewarded ad. On-load rewarded (PromoFetchStyle) warms nothing unless [force]d by
         * the moment about to show it; [onResult] then hears whether an ad is ready.
         */
        fun preload(
            context: Context,
            ignoreMasterSwitch: Boolean = false,
            force: Boolean = false,
            onResult: ((Boolean) -> Unit)? = null,
        ) {
            val pref = PromoSettingsBox.getInstance(context)
            if (kitDropStale() != null) return onResult?.invoke(true) ?: Unit
            if (!force && !PromoFetchStyle.preload(context, PromoFetchStyle.PackShape.REWARDED)) return
            if ((!ignoreMasterSwitch && !pref.getBoolean("IsAdsON")) || !pref.getBoolean("RewardedAds") || isLoading) {
                return onResult?.invoke(false) ?: Unit
            }

            val unitId = pref.getString("googleRewarded")
            if (unitId.isNullOrEmpty()) return onResult?.invoke(false) ?: Unit

            isLoading = true
            Log.d("RewardAdRenderer", "Preloading…")
            // A forced load is for the place about to show it; a warm-up preload has none.
            val place = if (force) PromoSignals.place else ""
            PromoSignals.send(PromoSignals.REWARDED, "request", place)

            RewardedAd.load(
                context,
                unitId,
                AdRequest.Builder().build(),
                object : RewardedAdLoadCallback() {
                    override fun onAdLoaded(ad: RewardedAd) {
                        PromoSignals.send(PromoSignals.REWARDED, "loaded", place)
                        loadedAd = ad
                        loadedAt = PromoEra.now()
                        isLoading = false
                        Log.d("RewardAdRenderer", "Preloaded OK")
                        onResult?.invoke(true)
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        PromoSignals.fetchFailed(PromoSignals.REWARDED, error.code, place)
                        loadedAd = null
                        isLoading = false
                        Log.e("RewardAdRenderer", "Preload failed: ${error.message}")
                        onResult?.invoke(false)
                    }
                }
            )
        }

        fun kitShowIfLoaded(activity: Activity, onDone: () -> Unit, onMiss: () -> Unit = onDone): Boolean {
            val ad = kitDropStale() ?: return false
            loadedAd = null
            val place = PromoSignals.place
            ad.fullScreenContentCallback = object : PromoSignals.WholeScreen(PromoSignals.REWARDED, place) {
                override fun onAdDismissedFullScreenContent() {
                    super.onAdDismissedFullScreenContent()
                    preload(activity)
                    onDone()
                }

                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    super.onAdFailedToShowFullScreenContent(error)
                    Log.e("RewardAdRenderer", "Fallback show failed: ${error.message}")
                    preload(activity)
                    // Not shown: let the chain fall back (if_ad_not_ready / next step) instead of counting it as shown.
                    onMiss()
                }
            }
            ad.setOnPaidEventListener { PromoSignals.paid(it, "rewarded_fallback", ad.adUnitId, ad.responseInfo, place) }
            ad.show(activity) {}
            return true
        }

        /**
         * A chain's rewarded step: the ready ad, else - on-load rewarded - one loaded now behind
         * the loader. [onMiss] when there is none (or the load outlasts ads.loader_wait_sec).
         */
        fun presentOrFetch(activity: Activity, onDone: () -> Unit, onMiss: () -> Unit, loadIfMissing: Boolean = false) {
            if (kitShowIfLoaded(activity, onDone, onMiss)) return
            if (!loadIfMissing && PromoFetchStyle.preload(activity, PromoFetchStyle.PackShape.REWARDED)) {
                preload(activity)
                return onMiss()
            }
            pullNow(activity, ignoreMasterSwitch = false) { ok ->
                if (!ok || !kitShowIfLoaded(activity, onDone, onMiss)) onMiss()
            }
        }

        /** Loads now behind the full-screen loader; [onResult] is false on failure or after ads.loader_wait_sec. */
        private fun pullNow(activity: Activity, ignoreMasterSwitch: Boolean, onResult: (Boolean) -> Unit) {
            LayerPrimer.show(activity, true)
            val wait = PromoDelays.main(activity)
            wait.start {
                LayerPrimer.hide()
                Log.d("RewardAdRenderer", "on-load: timed out — skip")
                PromoSignals.send(PromoSignals.REWARDED, "timeout", PromoSignals.place)
                onResult(false)
            }
            preload(activity, ignoreMasterSwitch, force = true) { ok ->
                if (!wait.kitArrived()) return@preload // late: the ad stays cached for next time
                LayerPrimer.hide()
                onResult(ok && !activity.isFinishing && !activity.isDestroyed)
            }
        }

    }


}
