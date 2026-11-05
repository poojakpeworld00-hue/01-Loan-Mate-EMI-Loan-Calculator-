package com.fffskintools.skin.tools.ads.render
import com.fffskintools.skin.tools.ads.host.kitOnceWindowFocused
import android.app.Activity
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.BaseAdView
import com.google.android.gms.ads.admanager.AdManagerAdRequest
import com.google.android.gms.ads.admanager.AdManagerAdView
import com.google.android.gms.ads.LoadAdError
import com.fffskintools.skin.tools.ads.entity.SpotSlotKind
import com.fffskintools.skin.tools.ads.policy.SpotOptionsChest
import com.fffskintools.skin.tools.ads.host.SpotCues

enum class BandSizing { ADAPTIVE, INLINE, NORMAL }
enum class BandStyle { AUTO, GOOGLE, CUSTOM }

interface BandLifecycleHook {
    fun onAdLoaded() {}
    fun onAdFailed() {}
    /** Banner intentionally not shown (switched off or frequency skip): callers must not fall back. */
    fun onSpotSkipped() {}
}

/** [place] = the RC place key this banner runs for (`ad` / `ad_impression` events). */
class BandPainter(private val place: String = SpotCues.place) {
    private var googleBanner: BaseAdView? = null

    companion object {
        var bannerCounter = 0
    }

    fun presentBand(
        activity: Activity,
        container: FrameLayout,
        type: BandStyle = BandStyle.AUTO,
        size: BandSizing = BandSizing.ADAPTIVE,
        isCollapsable: Boolean = false,
        shimmer: ShimmerFrameLayout? = null,
        observer: BandLifecycleHook? = null,
        customAdUnitId: String? = null,
        disableInternalFallback: Boolean = false
    ) {
        val pref = SpotOptionsChest.getInstance(activity)

        if (!pref.getBoolean("IsAdsON") || !pref.getBoolean("BannerAdPresenter")) {
            hide(container)
            observer?.onSpotSkipped()
            return
        }
        if (!kitHasNetwork(activity)) {
            hide(container)
            observer?.onAdFailed()
            return
        }

        if (bannerCounter < pref.getInt("BannerCounter")) {
            bannerCounter++
            hide(container)
            observer?.onSpotSkipped()
            return
        }
        bannerCounter = 0

        when (type) {
            BandStyle.AUTO -> {
                when (SpotSlotKind.fromString(pref.getString("IsAdType"))) {
                    SpotSlotKind.GOOGLE -> obtainGmsBand(
                        activity,
                        container,
                        size,
                        isCollapsable,
                        shimmer,
                        observer,
                        customAdUnitId,
                        disableInternalFallback
                    )

                    SpotSlotKind.CUSTOM, SpotSlotKind.UNKNOWN -> {
                        if (disableInternalFallback) {
                            observer?.onAdFailed()
                        } else {
                            InhouseSpotLeader(place).obtainInhouseSpot(
                                activity,
                                container,
                                InhouseSpotLeader.InhouseSpotStyle.BANNER
                            )
                        }
                    }
                }
            }

            BandStyle.GOOGLE -> obtainGmsBand(
                activity,
                container,
                size,
                isCollapsable,
                shimmer,
                observer,
                customAdUnitId,
                disableInternalFallback
            )

            BandStyle.CUSTOM -> {
                if (disableInternalFallback) {
                    observer?.onAdFailed()
                } else {
                    InhouseSpotLeader(place).obtainInhouseSpot(
                        activity,
                        container,
                        InhouseSpotLeader.InhouseSpotStyle.BANNER
                    )
                }
            }
        }
    }

    private fun obtainGmsBand(
        activity: Activity,
        container: FrameLayout,
        size: BandSizing,
        isCollapsable: Boolean,
        shimmer: ShimmerFrameLayout? = null,
        observer: BandLifecycleHook?,
        customAdUnitId: String? = null,
        disableInternalFallback: Boolean = false
    ) {
        val pref = SpotOptionsChest.getInstance(activity)
        val adUnitId = customAdUnitId ?: pref.getString("googleBanner")

        if (!SpotSlotBarrier.isOpen(container)) return // slot off screen (SpotSlotBarrier)
        if (!adUnitId.isNullOrEmpty() && !SpotSlotBarrier.beginPull(container)) return // this slot is already loading

        if (adUnitId.isNullOrEmpty()) {
            if (disableInternalFallback) observer?.onAdFailed()
            else degradeToFanOrInhouse(activity, container, shimmer, observer)
            return
        }

        hide(container)

        shimmer?.startShimmer()
        shimmer?.visibility = View.VISIBLE
        container.removeAllViews()
        shimmer?.let { container.addView(it) }
        container.visibility = View.VISIBLE

        // Spot Manager units ("/network/...") load through AdManagerAdView + AdManagerAdRequest, per the Spot Manager banner guide.
        val adManager = adUnitId.startsWith("/")
        if (googleBanner == null) googleBanner = if (adManager) AdManagerAdView(activity) else AdView(activity)
        googleBanner?.adUnitId = adUnitId

        if (isCollapsable) {
            googleBanner?.setAdSize(getAdSize(activity, container))
        } else {
            googleBanner?.setAdSize(gmsBandSize(activity, container, size, isCollapsable))
        }

        googleBanner?.adListener = object : SpotCues.KitListener(SpotCues.BANNER, place) {
            override fun onAdLoaded() {
                SpotCues.send(SpotCues.BANNER, "loaded", place)
                // Attach only while this screen is in front: a collapsible opens its expanded popup on attach,
                // and behind a full-screen ad (app-open / interstitial) that popup would show through.
                activity.kitOnceWindowFocused { kitAttachLoaded() }
            }

            private fun kitAttachLoaded() {
                SpotSlotBarrier.endPull(container)
                // The slot closed meanwhile (panel slid away, side menu opened): drop the ad instead of
                // letting a collapsible open its expanded popup over another screen.
                if (!SpotSlotBarrier.isOpen(container) || activity.isFinishing || activity.isDestroyed) {
                    runCatching { googleBanner?.destroy() }
                    googleBanner = null
                    return
                }
                Log.i(
                    "BannerRenderer",
                    "Ad loaded. adView.isCollapsible() is ${googleBanner?.isCollapsible}.",
                )

                val banner = googleBanner!!
                banner.setOnPaidEventListener { SpotCues.paid(it, "banner", banner.adUnitId, banner.responseInfo, place) }

                shimmer?.stopShimmer()
                shimmer?.visibility = View.GONE
                SpotSlotBarrier.destroySpots(container) // a ribbon from an earlier load may still sit here; removed-but-live keeps its popup
                container.removeAllViews()

                container.addView(SpotFrameStyling.bandStrip(activity, banner))
                container.visibility = View.VISIBLE
                SpotCues.send(SpotCues.BANNER, "show", place)
                // Any screen: the slot (and an expanded ad's popup) goes away with its screen.
                SpotSlotBarrier.dismissWithView(activity, container)
                observer?.onAdLoaded()
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                super.onAdFailedToLoad(error)
                SpotSlotBarrier.endPull(container)
                shimmer?.stopShimmer()
                shimmer?.visibility = View.GONE
                Log.e("BannerRenderer", "Google Banner Failed: ${error.message}")
                observer?.onAdFailed()
                if (!disableInternalFallback) {
                    degradeToFanOrInhouse(activity, container, shimmer, observer)
                }
            }

            // Collapsible expanded / collapsed (onAdOpened / onAdClosed): white backdrop under Google's popup.
            override fun onAdOpened() {
                val banner = googleBanner ?: return
                if (banner.isCollapsible) ShrinkableScrim.show(activity, banner)
            }

            override fun onAdClosed() = ShrinkableScrim.hide(activity)
        }

        // RC banner_collapsible -> every load asks for the expanding version (no app-side limit).
        val expand = isCollapsable
        val extras = Bundle().apply { putString("collapsible", "bottom") }
        val request: AdRequest = when {
            adManager && expand -> AdManagerAdRequest.Builder().addNetworkExtrasBundle(AdMobAdapter::class.java, extras).build()
            adManager -> AdManagerAdRequest.Builder().build()
            expand -> AdRequest.Builder().addNetworkExtrasBundle(AdMobAdapter::class.java, extras).build()
            else -> AdRequest.Builder().build()
        }

        SpotCues.send(SpotCues.BANNER, "request", place)
        googleBanner?.loadAd(request)
    }

    private fun getAdSize(
        activity: Activity,
        sponsorContainer: FrameLayout
    ): com.google.android.gms.ads.AdSize {
        val display = DisplayMetrics()
        activity.windowManager.defaultDisplay.getMetrics(display)

        val density = display.density
        val widthPixels =
            if (sponsorContainer.width == 0) display.widthPixels.toFloat()
            else sponsorContainer.width.toFloat()

        val adWidth = (widthPixels / density).toInt()
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, adWidth)
    }

    private fun gmsBandSize(
        activity: Activity,
        container: FrameLayout,
        size: BandSizing,
        isCollapsable: Boolean
    ): AdSize {
        return when (size) {
            BandSizing.NORMAL -> AdSize.BANNER
            BandSizing.INLINE -> AdSize.getCurrentOrientationInlineAdaptiveBannerAdSize(
                activity,
                kitDisplayWidthDp(activity)
            )

            BandSizing.ADAPTIVE -> AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(
                activity,
                kitDisplayWidthDp(activity)
            )
        }
    }

    private fun kitDisplayWidthDp(activity: Activity): Int {
        val display = DisplayMetrics()
        activity.windowManager.defaultDisplay.getMetrics(display)
        return (display.widthPixels / display.density).toInt()
    }

    private fun degradeToFanOrInhouse(
        activity: Activity,
        container: FrameLayout,
        shimmer: ShimmerFrameLayout? = null,
        observer: BandLifecycleHook?
    ) {
        shimmer?.startShimmer()
        shimmer?.visibility = View.VISIBLE
        InhouseSpotLeader(place).obtainInhouseSpot(
            activity,
            container,
            InhouseSpotLeader.InhouseSpotStyle.BANNER
        )
    }

    private fun hide(container: FrameLayout) {
        ShrinkableScrim.maskFor(container.context)
        SpotSlotBarrier.destroySpots(container) // the banner being replaced must not keep drawing (half-and-half look)
        container.removeAllViews()
        container.visibility = View.GONE
    }
}
