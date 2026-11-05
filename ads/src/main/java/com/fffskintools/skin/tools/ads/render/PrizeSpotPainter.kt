package com.fffskintools.skin.tools.ads.render

import com.fffskintools.skin.tools.ads.host.SpotAge
import com.fffskintools.skin.tools.ads.host.SpotCues
import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.fffskintools.skin.tools.ads.policy.SpotPullMode
import com.fffskintools.skin.tools.ads.policy.SpotOptionsChest
import com.fffskintools.skin.tools.ads.policy.SpotWaits
import com.fffskintools.skin.tools.ads.render.interstitial.OverlayWarmer
import com.fffskintools.skin.tools.ads.render.interstitial.NavigationOverlay

class PrizeSpotPainter {
    companion object {
        private var loadedAd: RewardedAd? = null
        private var loadedAt = 0L
        val isLoaded: Boolean get() = kitDropStale() != null

        private fun kitDropStale(): RewardedAd? {
            if (loadedAd != null && SpotAge.stale(loadedAt, SpotAge.FULL_SCREEN_MS)) loadedAd = null
            return loadedAd
        }
        private var isLoading = false

        /**
         * Warms the rewarded ad. On-load rewarded (SpotPullMode) warms nothing unless [force]d by
         * the moment about to show it; [onResult] then hears whether an ad is ready.
         */
        fun preload(
            context: Context,
            ignoreMasterSwitch: Boolean = false,
            force: Boolean = false,
            onResult: ((Boolean) -> Unit)? = null,
        ) {
            val pref = SpotOptionsChest.getInstance(context)
            if (kitDropStale() != null) return onResult?.invoke(true) ?: Unit
            if (!force && !SpotPullMode.preload(context, SpotPullMode.KitFormat.REWARDED)) return
            if ((!ignoreMasterSwitch && !pref.getBoolean("IsAdsON")) || !pref.getBoolean("RewardedAds") || isLoading) {
                return onResult?.invoke(false) ?: Unit
            }

            val unitId = pref.getString("googleRewarded")
            if (unitId.isNullOrEmpty()) return onResult?.invoke(false) ?: Unit

            isLoading = true
            Log.d("RewardAdRenderer", "Preloading…")
            // A forced load is for the place about to show it; a warm-up preload has none.
            val place = if (force) SpotCues.place else ""
            SpotCues.send(SpotCues.REWARDED, "request", place)

            RewardedAd.load(
                context,
                unitId,
                AdRequest.Builder().build(),
                object : RewardedAdLoadCallback() {
                    override fun onAdLoaded(ad: RewardedAd) {
                        SpotCues.send(SpotCues.REWARDED, "loaded", place)
                        loadedAd = ad
                        loadedAt = SpotAge.now()
                        isLoading = false
                        Log.d("RewardAdRenderer", "Preloaded OK")
                        onResult?.invoke(true)
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        SpotCues.loadFailed(SpotCues.REWARDED, error.code, place)
                        loadedAd = null
                        isLoading = false
                        Log.e("RewardAdRenderer", "Preload failed: ${error.message}")
                        onResult?.invoke(false)
                    }
                }
            )
        }

        fun kitPresentIfLoaded(activity: Activity, onDone: () -> Unit, onMiss: () -> Unit = onDone): Boolean {
            val ad = kitDropStale() ?: return false
            loadedAd = null
            val place = SpotCues.place
            ad.fullScreenContentCallback = object : SpotCues.FullView(SpotCues.REWARDED, place) {
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
            ad.setOnPaidEventListener { SpotCues.paid(it, "rewarded_fallback", ad.adUnitId, ad.responseInfo, place) }
            ad.show(activity) {}
            return true
        }

        /**
         * A chain's rewarded step: the ready ad, else - on-load rewarded - one loaded now behind
         * the loader. [onMiss] when there is none (or the load outlasts ads.loader_wait_sec).
         */
        fun presentOrPull(activity: Activity, onDone: () -> Unit, onMiss: () -> Unit, loadIfMissing: Boolean = false) {
            if (kitPresentIfLoaded(activity, onDone, onMiss)) return
            if (!loadIfMissing && SpotPullMode.preload(activity, SpotPullMode.KitFormat.REWARDED)) {
                preload(activity)
                return onMiss()
            }
            pullNow(activity, ignoreMasterSwitch = false) { ok ->
                if (!ok || !kitPresentIfLoaded(activity, onDone, onMiss)) onMiss()
            }
        }

        /** Loads now behind the full-screen loader; [onResult] is false on failure or after ads.loader_wait_sec. */
        private fun pullNow(activity: Activity, ignoreMasterSwitch: Boolean, onResult: (Boolean) -> Unit) {
            OverlayWarmer.show(activity, true)
            val wait = SpotWaits.main(activity)
            wait.start {
                OverlayWarmer.hide()
                Log.d("RewardAdRenderer", "on-load: timed out — skip")
                SpotCues.send(SpotCues.REWARDED, "timeout", SpotCues.place)
                onResult(false)
            }
            preload(activity, ignoreMasterSwitch, force = true) { ok ->
                if (!wait.kitArrived()) return@preload // late: the ad stays cached for next time
                OverlayWarmer.hide()
                onResult(ok && !activity.isFinishing && !activity.isDestroyed)
            }
        }

    }


}
