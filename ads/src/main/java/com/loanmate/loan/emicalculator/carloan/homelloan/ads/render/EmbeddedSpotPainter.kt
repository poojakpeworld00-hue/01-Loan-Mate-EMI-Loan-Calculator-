package com.loanmate.loan.emicalculator.carloan.homelloan.ads.render

import com.loanmate.loan.emicalculator.carloan.homelloan.ads.SpotKit
import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnAttach
import androidx.core.view.updatePadding
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import com.loanmate.loan.emicalculator.carloan.homelloan.ads.entity.SpotSlotKind
import com.loanmate.loan.emicalculator.carloan.homelloan.ads.entity.SpotSlotKind.*
import com.loanmate.loan.emicalculator.carloan.homelloan.ads.policy.SpotPullMode
import com.loanmate.loan.emicalculator.carloan.homelloan.ads.policy.SpotOptionsChest
import com.loanmate.loan.emicalculator.carloan.homelloan.ads.policy.SpotUnitIds
import com.loanmate.loan.emicalculator.carloan.homelloan.ads.policy.SpotWaits
import com.loanmate.loan.emicalculator.carloan.homelloan.ads.policy.ViewSpotPlanner
import com.loanmate.loan.emicalculator.carloan.homelloan.ads.analytics.Stats2
import com.loanmate.loan.emicalculator.carloan.homelloan.ads.host.SpotCues
import com.loanmate.loan.emicalculator.carloan.homelloan.ads.databinding.AdGmsEmbeddedLargeBinding
import androidx.core.content.ContextCompat
import com.loanmate.loan.emicalculator.carloan.homelloan.ads.R
import com.loanmate.loan.emicalculator.carloan.homelloan.ads.databinding.AdGmsEmbeddedFullpageBinding
import com.loanmate.loan.emicalculator.carloan.homelloan.ads.databinding.AdGmsEmbeddedMediumAltBinding
import com.loanmate.loan.emicalculator.carloan.homelloan.ads.databinding.AdGmsEmbeddedMediumBinding
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions

/** [place] = the RC place key this native runs for (`ad` / `ad_impression` events). */
class EmbeddedSpotPainter(private val place: String = SpotCues.place) {
    interface EmbeddedSpotMonitor {
        fun onNativeAdLoaded()
        fun onNativeAdFailed()
    }

    companion object {
        private var nativeAd: NativeAd? = null
        /** Events and unit of the cached [nativeAd]: its place is set when it goes on screen. */
        private var cachedEvents: SpotCues.KitListener? = null
        private var cachedUnit = ""

        var isFullNativeShowing: Boolean = false

        fun holdsCachedSpot(): Boolean = nativeAd != null

        fun isLoading(): Boolean = sharedLoad.inFlight

        /** One native load at a time; preload and screens share it instead of racing separate loads. */
        private val sharedLoad = com.loanmate.loan.emicalculator.carloan.homelloan.ads.policy.SharedPull<Boolean>()
    }

    /**
     * The cached native is empty. On-load natives load one for this slot now, in [format]
     * ("big" / "mid" / "mid2"); [missing] runs as before if they are preloaded or the load fails.
     */
    private fun pullNowOr(
        context: Activity,
        format: String,
        layout: FrameLayout,
        shimmer: ShimmerFrameLayout?,
        missing: () -> Unit,
    ) {
        if (SpotPullMode.preload(context, SpotPullMode.KitFormat.NATIVE)) return missing()
        kitPresentWithUnit(context, ViewSpotPlanner.embeddedUnitId(context), format, layout, shimmer) { missing() }
    }

    private fun Activity.kitIsActivityGone(): Boolean {
        if (isFinishing) return true
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && isDestroyed
    }

    /** [place]: the slot waiting for this load; "" for a warm-up preload. */
    fun obtainEmbeddedSpots(context: Activity, observer: EmbeddedSpotMonitor? = null, place: String = "") {
        val adsPreference = SpotOptionsChest.getInstance(context)

        // On-load natives (SpotPullMode): nothing is warmed; each slot loads its own when shown.
        if (!SpotPullMode.preload(context, SpotPullMode.KitFormat.NATIVE)) {
            observer?.onNativeAdFailed()
            return
        }

        if (!adsPreference.getBoolean("IsAdsON") || SpotSlotKind.fromString(adsPreference.getString("IsAdType")) != SpotSlotKind.GOOGLE) {
            observer?.onNativeAdFailed()
            return
        }

        if (!adsPreference.getBoolean("NativeAd")) {
            observer?.onNativeAdFailed()
            return
        }

        val adUnit = ViewSpotPlanner.embeddedUnitId(context)
        if (adUnit.isEmpty()) {
            observer?.onNativeAdFailed()
            return
        }
        val starts = sharedLoad.join { ok ->
            if (context.kitIsActivityGone()) return@join // each requester's own screen must still be alive
            if (ok) observer?.onNativeAdLoaded() else observer?.onNativeAdFailed()
        }
        if (!starts) return // a load is already running; this observer hears its result
        val events = object : SpotCues.KitListener(SpotCues.NATIVE, place) {
            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                super.onAdFailedToLoad(loadAdError)
                sharedLoad.finish(false)
                if (context.kitIsActivityGone()) return
                Log.e(
                    "NativeAds",
                    "Google onAdFailedToLoad:nativeAd ${loadAdError.message}"
                )
                nativeAd = null
            }
        }
        val adLoader =
            AdLoader.Builder(context, adUnit)
                .forNativeAd { nativeAds ->

                    nativeAd?.destroy()
                    nativeAd = nativeAds
                    cachedEvents = events
                    cachedUnit = adUnit
                    SpotCues.send(SpotCues.NATIVE, "loaded", place)
                    // Waiters check their own activity; the cached ad is kept for the next screen either way.
                    sharedLoad.finish(true)

                    Log.e("NativeAds", "Google Load: nativeAd")
                }
                .withAdListener(events)
                .withNativeAdOptions(NativeAdOptions.Builder().build())
                .build()

        SpotCues.send(SpotCues.NATIVE, "request", place)
        adLoader.loadAd(AdRequest.Builder().build())
    }

    /** The cached ad goes on screen here: its show, impression, click and paid events carry this [place]. */
    private fun kitShownCached(ad: NativeAd, label: String) {
        cachedEvents?.place = place
        SpotCues.send(SpotCues.NATIVE, "show", place)
        val unit = cachedUnit
        ad.setOnPaidEventListener { SpotCues.paid(it, label, unit, ad.responseInfo, place) }
    }

    fun kitPresentWithUnit(
        context: Activity,
        unitId: String,
        format: String,
        layout: FrameLayout,
        shimmer: ShimmerFrameLayout? = null,
        fallback: () -> Unit,
    ) {
        val pref = SpotOptionsChest.getInstance(context)
        val kind = when (format.lowercase()) {
            "bignative", "big_native", "big" -> 0
            "mediumnative", "mid_native", "mid" -> 1
            "mediumnative2", "mid_native_2", "mid2" -> 2
            else -> -1
        }
        if (unitId.isBlank() || kind < 0 || context.kitIsActivityGone() || !kitHasNetwork(context) ||
            !pref.getBoolean("IsAdsON") || !pref.getBoolean("NativeAd") ||
            SpotSlotKind.fromString(pref.getString("IsAdType")) != SpotSlotKind.GOOGLE
        ) return fallback()

        layout.visible()
        shimmer?.startShimmer()
        shimmer?.isVisible = true
        SpotCues.send(SpotCues.NATIVE, "request", place)
        AdLoader.Builder(context, unitId)
            .forNativeAd { ad ->
                SpotCues.send(SpotCues.NATIVE, "loaded", place)
                if (context.kitIsActivityGone() || !layout.isAttachedToWindow) {
                    ad.destroy()
                    return@forNativeAd
                }
                val root: View = when (kind) {
                    0 -> AdGmsEmbeddedLargeBinding.inflate(context.layoutInflater)
                        .also { largeEmbeddedLayout(ad, it, context) }.root
                    1 -> AdGmsEmbeddedMediumBinding.inflate(context.layoutInflater)
                        .also { mediumEmbeddedLayout(ad, it, context) }.root
                    else -> AdGmsEmbeddedMediumAltBinding.inflate(context.layoutInflater)
                        .also { mediumEmbeddedLayoutAlt(ad, it, context) }.root
                }
                ad.setOnPaidEventListener { SpotCues.paid(it, "native_unit", unitId, ad.responseInfo, place) }
                layout.removeAllViews()
                shimmer?.stopShimmer()
                shimmer?.isVisible = false
                layout.addView(root)
                SpotCues.send(SpotCues.NATIVE, "show", place)
            }
            .withAdListener(object : SpotCues.KitListener(SpotCues.NATIVE, place) {
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    super.onAdFailedToLoad(loadAdError)
                    if (context.kitIsActivityGone()) return
                    Log.e("NativeAds", "slot unit $unitId failed: ${loadAdError.message} — shared native")
                    fallback()
                }
            })
            .withNativeAdOptions(NativeAdOptions.Builder().build())
            .build()
            .loadAd(AdRequest.Builder().build())
    }

    fun presentLargeEmbedded(
        context: Activity,
        layout: FrameLayout,
        shimmer: ShimmerFrameLayout? = null,
        isButtonTop: Boolean? = false,
        imageView: ImageView? = null,
        ln: LinearLayout? = null
    ) {
        val adsPreference = SpotOptionsChest.getInstance(context)

        if (context.isFinishing || context.isDestroyed) return

        if (!kitHasNetwork(context)
            || !adsPreference.getBoolean("IsAdsON")
            || !adsPreference.getBoolean("NativeAd")
        ) {
            layout.removeAllViews()
            layout.invisible()
            shimmer?.stopShimmer()
            shimmer?.isVisible = false
            imageView?.visible()
            ln?.visible()
            return
        }

        layout.visible()
        shimmer?.startShimmer()
        shimmer?.isVisible = true
        imageView?.gone()
        ln?.gone()

        when (SpotSlotKind.fromString(adsPreference.getString("IsAdType"))) {
            SpotSlotKind.GOOGLE -> {
                layout.post {
                    try {
                        if (context.isFinishing || context.isDestroyed) return@post

                        if (nativeAd != null) {
                            val rootView: View = run {
                                val binding = AdGmsEmbeddedLargeBinding.inflate(context.layoutInflater)
                                largeEmbeddedLayout(nativeAd!!, binding, context)
                                binding.root
                                }

                            layout.removeAllViews()
                            shimmer?.stopShimmer()
                            shimmer?.isVisible = false
                            layout.addView(rootView)

                            kitShownCached(nativeAd!!, "native_big")

                            nativeAd = null
                            obtainEmbeddedSpots(context)

                            return@post
                        }

                        pullNowOr(context, "big", layout, shimmer) {
                            layout.removeAllViews()
                            shimmer?.stopShimmer()
                            shimmer?.isVisible = false

                            InhouseSpotLeader(place).obtainInhouseSpot(
                                context,
                                layout,
                                InhouseSpotLeader.InhouseSpotStyle.BIG_NATIVE,
                                imageView
                            )
                        }
                    } catch (e: Exception) {
                        Log.e("NativeAds", "Google BigNative crash", e)
                        SpotKit.onError(e, "native.big")
                        shimmer?.stopShimmer()
                        shimmer?.visibility = View.GONE
                        layout.removeAllViews()
                        layout.visibility = View.GONE
                    }
                }
            }


            SpotSlotKind.UNKNOWN, SpotSlotKind.CUSTOM -> {
                layout.removeAllViews()
                shimmer?.stopShimmer()
                shimmer?.isVisible = false
                InhouseSpotLeader(place).obtainInhouseSpot(
                    context,
                    layout,
                    InhouseSpotLeader.InhouseSpotStyle.BIG_NATIVE,
                    imageView
                )
            }
        }
    }

    fun presentFullEmbedded(
        context: Activity,
        layout: FrameLayout,
        onClose: () -> Unit,
        shimmer: ShimmerFrameLayout? = null,
    ) {
        val adsPreference = SpotOptionsChest.getInstance(context)

        if (context.isFinishing || context.isDestroyed) return

        if (!kitHasNetwork(context)
            || !adsPreference.getBoolean("IsAdsON")
            || !adsPreference.getBoolean("NativeAd")
        ) {
            layout.removeAllViews()
            shimmer?.stopShimmer()
            shimmer?.isVisible = false
            onClose()
            return
        }

        shimmer?.startShimmer()
        shimmer?.isVisible = true

        if (SpotSlotKind.fromString(adsPreference.getString("IsAdType")) != SpotSlotKind.GOOGLE) {
            onClose()
            return
        }

        layout.post {
            try {
                if (context.isFinishing || context.isDestroyed) return@post

                if (nativeAd != null) {
                    val binding = AdGmsEmbeddedFullpageBinding.inflate(context.layoutInflater)

                    isFullNativeShowing = true
                    fullpageEmbeddedLayout(nativeAd!!, binding, context) {
                        isFullNativeShowing = false
                        onClose()
                    }

                    layout.removeAllViews()
                    shimmer?.stopShimmer()
                    shimmer?.isVisible = false
                    layout.addView(binding.root)

                    kitShownCached(nativeAd!!, "native_full")

                    nativeAd = null
                    obtainEmbeddedSpots(context)
                    return@post
                }

                shimmer?.stopShimmer()
                shimmer?.isVisible = false
                // On-load natives: nothing was warmed, so load one now over the screen.
                if (!SpotPullMode.preload(context, SpotPullMode.KitFormat.NATIVE)) {
                    presentFullEmbeddedWithUnit(context, SpotUnitIds.pickFullEmbedded(context, null), onClose = onClose)
                } else {
                    onClose()
                }
            } catch (e: Exception) {
                Log.e("NativeAds", "Google FullNative crash", e)
                SpotKit.onError(e, "native.full")
                onClose()
            }
        }
    }

    /**
     * Loads [unitId] now and shows it full screen. [onClose] runs once it is closed; [onMiss]
     * (null: [onClose]) if it fails to load or takes longer than ads.loader_wait_sec.
     */
    fun presentFullEmbeddedWithUnit(
        activity: Activity,
        unitId: String,
        onMiss: (() -> Unit)? = null,
        onClose: () -> Unit,
    ) {
        var fired = false
        val done = { if (!fired) { fired = true; onClose() } }
        val miss = { if (!fired) { fired = true; (onMiss ?: onClose)() } }

        val pref = SpotOptionsChest.getInstance(activity)
        if (!pref.getBoolean("IsAdsON") || !pref.getBoolean("NativeAd") || unitId.isBlank()) return miss()
        if (SpotSlotKind.fromString(pref.getString("IsAdType")) != SpotSlotKind.GOOGLE) return miss()
        if (activity.isFinishing || activity.isDestroyed) return miss()

        val content = activity.findViewById<ViewGroup>(android.R.id.content)
        val overlay = FrameLayout(activity).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            setBackgroundColor(android.graphics.Color.BLACK) // previous version: black
            elevation = 64f
            isClickable = true
        }
        content.addView(overlay)
        val dropOverlay = { (overlay.parent as? ViewGroup)?.removeView(overlay) }
        // A slow load gives up like an on-load interstitial does; a late ad is then dropped.
        val wait = SpotWaits.main(activity)
        wait.start {
            if (fired) return@start
            Log.d("NativeAds", "FullNative unit $unitId timed out — skip")
            SpotCues.send(SpotCues.NATIVE, "timeout", place)
            dropOverlay()
            miss()
        }

        SpotCues.send(SpotCues.NATIVE, "request", place)
        AdLoader.Builder(activity, unitId)
            .forNativeAd { ad ->
                SpotCues.send(SpotCues.NATIVE, "loaded", place)
                if (!wait.kitArrived() || fired || activity.isFinishing || activity.isDestroyed) {
                    ad.destroy(); dropOverlay(); return@forNativeAd miss()
                }
                val binding = AdGmsEmbeddedFullpageBinding.inflate(activity.layoutInflater)
                isFullNativeShowing = true
                fullpageEmbeddedLayout(ad, binding, activity) {
                    isFullNativeShowing = false
                    ad.destroy()
                    dropOverlay()
                    done()
                }
                overlay.removeAllViews()
                overlay.addView(binding.root)
                SpotCues.send(SpotCues.NATIVE, "show", place)
                ad.setOnPaidEventListener { SpotCues.paid(it, "native_full_unit", unitId, ad.responseInfo, place) }
            }
            .withAdListener(object : SpotCues.KitListener(SpotCues.NATIVE, place) {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    super.onAdFailedToLoad(error)
                    if (!wait.kitArrived()) return
                    Log.e("NativeAds", "FullNative unit load failed: ${error.message}")
                    dropOverlay()
                    miss()
                }
            })
            .build()
            .loadAd(AdRequest.Builder().build())
    }

    private fun fullpageEmbeddedLayout(
        nativeAd: NativeAd,
        binding: AdGmsEmbeddedFullpageBinding,
        context: Activity,
        onClose: () -> Unit,
    ) {
        binding.apply {
            mainNativeadView.mediaView = adMedia
            mainNativeadView.headlineView = adHeadline
            mainNativeadView.bodyView = adBody
            mainNativeadView.callToActionView = adCallToAction
            mainNativeadView.iconView = adAppIcon
            mainNativeadView.advertiserView = adAdvertiser

            adHeadline.text = nativeAd.headline
            adMedia.mediaContent = nativeAd.mediaContent

            // Full-page native keeps its own layout colours (previous version applied no palette here).

            adBody.apply {
                visibility = if (nativeAd.body == null) View.GONE else View.VISIBLE
                text = nativeAd.body
            }
            adAdvertiser.apply {
                visibility = if (nativeAd.advertiser == null) View.GONE else View.VISIBLE
                text = nativeAd.advertiser
            }
            adAppIcon.apply {
                visibility = if (nativeAd.icon == null) View.GONE else View.VISIBLE
                setImageDrawable(nativeAd.icon?.drawable)
            }
            adCallToAction.apply {
                visibility = if (nativeAd.callToAction == null) View.GONE else View.VISIBLE
                text = nativeAd.callToAction
            }

            adFullClose.setOnClickListener {
                Stats2.click("native_ad_close")
                SpotCues.send(SpotCues.NATIVE, "close", place)
                onClose()
            }

            mainNativeadView.setNativeAd(nativeAd)

            mainNativeadView.setTag(R.id.tag_native_ad, nativeAd)

            fullNativeContent.padOsBars()
        }
    }

    private fun View.padOsBars() {
        fun kitBarsOf(insets: WindowInsetsCompat) = insets.getInsets(
            WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
        )

        fun putFromRoot() {
            val bars = ViewCompat.getRootWindowInsets(this)?.let(::kitBarsOf) ?: return
            updatePadding(top = bars.top, bottom = bars.bottom)
        }

        if (isAttachedToWindow) putFromRoot() else doOnAttach { putFromRoot() }
        ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
            val bars = kitBarsOf(insets)
            v.updatePadding(top = bars.top, bottom = bars.bottom)
            insets
        }
        ViewCompat.requestApplyInsets(this)
    }

    private fun largeEmbeddedLayout(
        nativeAd: NativeAd,
        binding: AdGmsEmbeddedLargeBinding,
        context: Activity
    ) {
        binding.apply {
            binding.mainNativeadView.mediaView = adMedia
            binding.mainNativeadView.headlineView = adHeadline
            binding.mainNativeadView.bodyView = adBody
            binding.mainNativeadView.callToActionView = adCallToAction
            binding.mainNativeadView.iconView = adAppIcon

            (binding.mainNativeadView.headlineView as TextView).text = nativeAd.headline
            binding.mainNativeadView.mediaView?.mediaContent = nativeAd.mediaContent

            val bgColor = SpotOptionsChest.getInstance(context).getString("NativeBgColor")
            val btnColor = SpotOptionsChest.getInstance(context).getString("NativebtnColor")
            val txtColor = SpotOptionsChest.getInstance(context).getString("NativetxtColor")
            val btntxtColor = SpotOptionsChest.getInstance(context).getString("NativebtntxtColor")

            SpotFrameStyling.paintSlabBackground(context, binding.mainNativeadView, bgColor)
            SpotFrameStyling.kitPaintCtaBackground(context, binding.mainNativeadView.callToActionView, btnColor)

            (binding.mainNativeadView.headlineView as TextView).setTextColor(SpotFrameStyling.headlineColor2(context, txtColor))
            SpotFrameStyling.kitPaintDesignExtras(context, binding.mainNativeadView)
            (binding.mainNativeadView.bodyView as TextView).setTextColor(SpotFrameStyling.kitBodyColor(context, txtColor))
            (adCallToAction as TextView).setTextColor(SpotFrameStyling.ctaCaptionColor(context, btntxtColor))

            binding.mainNativeadView.bodyView?.apply {
                visibility = if (nativeAd.body == null) View.GONE else View.VISIBLE
                (binding.mainNativeadView.bodyView as TextView).text = nativeAd.body
            }

            binding.mainNativeadView.iconView?.apply {
                visibility = if (nativeAd.icon == null) View.GONE else View.VISIBLE
                (binding.mainNativeadView.iconView as ImageView).setImageDrawable(nativeAd.icon?.drawable)
            }

            binding.mainNativeadView.callToActionView?.apply {
                visibility = if (nativeAd.callToAction == null) View.GONE else View.VISIBLE
                (binding.mainNativeadView.callToActionView as TextView).text = nativeAd.callToAction
            }

            binding.mainNativeadView.setNativeAd(nativeAd)

            binding.mainNativeadView.setTag(R.id.tag_native_ad, nativeAd)
        }
    }

    fun presentMediumEmbedded(
        context: Activity,
        layout: FrameLayout,
        shimmer: ShimmerFrameLayout? = null,
        imageView: ImageView? = null,
        ln: LinearLayout? = null
    ) {
        val adsPref = SpotOptionsChest.getInstance(context)

        if (context.isFinishing || context.isDestroyed) return

        if (!kitHasNetwork(context)
            || !adsPref.getBoolean("IsAdsON")
            || !adsPref.getBoolean("NativeAd")
        ) {
            layout.removeAllViews()
            layout.invisible()
            shimmer?.stopShimmer()
            shimmer?.isVisible = false
            imageView?.visible()
            ln?.visible()
            return
        }


        shimmer?.startShimmer()
        shimmer?.isVisible = true
        imageView?.gone()
        ln?.gone()
        layout.visible()

        when (SpotSlotKind.fromString(adsPref.getString("IsAdType"))) {
            SpotSlotKind.GOOGLE -> {
                layout.post {
                    try {
                        if (context.isFinishing || context.isDestroyed) return@post
                        if (nativeAd != null) {
                            val binding = AdGmsEmbeddedMediumBinding.inflate(context.layoutInflater)
                            mediumEmbeddedLayout(
                                nativeAd!!,
                                binding,
                                context
                            )

                            layout.removeAllViews()

                            shimmer?.stopShimmer()
                            shimmer?.isVisible = false
                            layout.addView(binding.root)

                            kitShownCached(nativeAd!!, "native_medium")

                            nativeAd = null
                            obtainEmbeddedSpots(context)
                            return@post
                        }

                        pullNowOr(context, "mid", layout, shimmer) {
                            layout.removeAllViews()
                            shimmer?.stopShimmer()
                            shimmer?.isVisible = false
                            InhouseSpotLeader(place).obtainInhouseSpot(
                                context,
                                layout,
                                InhouseSpotLeader.InhouseSpotStyle.MID_NATIVE
                            )
                        }
                    } catch (e: Exception) {
                        Log.e("MidNativeAds", "Google MidNative failed: ${e.message}")
                        shimmer?.stopShimmer()
                        shimmer?.visibility = View.GONE
                        layout.removeAllViews()
                        layout.visibility = View.GONE
                    }
                }
            }


            SpotSlotKind.CUSTOM, SpotSlotKind.UNKNOWN -> {
                layout.removeAllViews()
                shimmer?.stopShimmer()
                shimmer?.isVisible = false
                InhouseSpotLeader(place).obtainInhouseSpot(
                    context,
                    layout,
                    InhouseSpotLeader.InhouseSpotStyle.MID_NATIVE
                )
            }
        }
    }

    private fun mediumEmbeddedLayout(
        nativeAd: NativeAd,
        binding: AdGmsEmbeddedMediumBinding,
        context: Activity
    ) {
        binding.apply {
            binding.mainNativeadView.headlineView = adHeadline
            binding.mainNativeadView.bodyView = adBody
            binding.mainNativeadView.callToActionView = adCallToAction
            binding.mainNativeadView.iconView = adAppIcon

            (binding.mainNativeadView.headlineView as TextView).text = nativeAd.headline

            val bgColor = SpotOptionsChest.getInstance(context).getString("NativeBgColor")
            val btnColor = SpotOptionsChest.getInstance(context).getString("NativebtnColor")
            val txtColor = SpotOptionsChest.getInstance(context).getString("NativetxtColor")
            val btntxtColor = SpotOptionsChest.getInstance(context).getString("NativebtntxtColor")

            SpotFrameStyling.paintSlabBackground(context, binding.mainNativeadView, bgColor)
            SpotFrameStyling.kitPaintCtaBackground(context, binding.mainNativeadView.callToActionView, btnColor)

            (binding.mainNativeadView.headlineView as TextView).setTextColor(SpotFrameStyling.headlineColor2(context, txtColor))
            SpotFrameStyling.kitPaintDesignExtras(context, binding.mainNativeadView)
            (binding.mainNativeadView.bodyView as TextView).setTextColor(SpotFrameStyling.kitBodyColor(context, txtColor))
            (adCallToAction as TextView).setTextColor(SpotFrameStyling.ctaCaptionColor(context, btntxtColor))

            binding.mainNativeadView.bodyView?.apply {
                visibility = if (nativeAd.body == null) View.GONE else View.VISIBLE
                (binding.mainNativeadView.bodyView as TextView).text = nativeAd.body
            }

            binding.mainNativeadView.iconView?.apply {
                visibility = if (nativeAd.icon == null) View.GONE else View.VISIBLE
                (binding.mainNativeadView.iconView as ImageView).setImageDrawable(nativeAd.icon?.drawable)
            }

            binding.mainNativeadView.callToActionView?.apply {
                visibility = if (nativeAd.callToAction == null) View.GONE else View.VISIBLE
                (binding.mainNativeadView.callToActionView as TextView).text = nativeAd.callToAction
            }

            binding.mainNativeadView.setNativeAd(nativeAd)

            binding.mainNativeadView.setTag(R.id.tag_native_ad, nativeAd)
        }
    }

    fun View.visible() {
        this.visibility = View.VISIBLE
    }

    fun View.invisible() {
        this.visibility = View.GONE
    }

    fun presentMediumEmbeddedAlt(
        context: Activity,
        layout: FrameLayout,
        shimmer: ShimmerFrameLayout? = null,
        imageView: ImageView? = null,
        ln: LinearLayout? = null
    ) {
        val adsPref = SpotOptionsChest.getInstance(context)

        if (context.isFinishing || context.isDestroyed) return

        if (!kitHasNetwork(context) || !adsPref.getBoolean("IsAdsON") || !adsPref.getBoolean("NativeAd")) {
            layout.removeAllViews()
            layout.invisible()
            shimmer?.stopShimmer()
            shimmer?.isVisible = false
            imageView?.visible()
            ln?.visible()
            return
        }


        shimmer?.startShimmer()
        shimmer?.isVisible = true
        imageView?.gone()
        ln?.gone()
        layout.visible()
        when (SpotSlotKind.fromString(adsPref.getString("IsAdType"))) {
            GOOGLE -> {
                layout.post {
                    try {
                        if (context.isFinishing || context.isDestroyed) return@post
                        if (Companion.nativeAd != null) {
                            val binding = AdGmsEmbeddedMediumAltBinding.inflate(context.layoutInflater)
                            mediumEmbeddedLayoutAlt(
                                Companion.nativeAd!!,
                                binding,
                                context
                            )

                            layout.removeAllViews()

                            shimmer?.stopShimmer()
                            shimmer?.isVisible = false
                            layout.addView(binding.root)

                            kitShownCached(nativeAd!!, "native_medium2")

                            nativeAd = null
                            obtainEmbeddedSpots(context)
                            return@post
                        }

                        pullNowOr(context, "mid2", layout, shimmer) {
                            layout.removeAllViews()
                            shimmer?.stopShimmer()
                            shimmer?.isVisible = false
                            InhouseSpotLeader(place).obtainInhouseSpot(
                                context,
                                layout,
                                InhouseSpotLeader.InhouseSpotStyle.MID_NATIVE
                            )
                        }
                    } catch (e: Exception) {
                        Log.e("MidNativeAds", "Google MidNative failed: ${e.message}")
                        shimmer?.stopShimmer()
                        shimmer?.visibility = View.GONE
                        layout.removeAllViews()
                        layout.visibility = View.GONE
                    }
                }
            }

            CUSTOM, UNKNOWN -> {
                layout.removeAllViews()
                shimmer?.stopShimmer()
                shimmer?.isVisible = false
                InhouseSpotLeader(place).obtainInhouseSpot(
                    context,
                    layout,
                    InhouseSpotLeader.InhouseSpotStyle.MID_NATIVE
                )
            }

        }
    }

    private fun mediumEmbeddedLayoutAlt(
        nativeAd: NativeAd,
        binding: AdGmsEmbeddedMediumAltBinding,
        context: Activity
    ) {
        binding.apply {
            binding.mainNativeadView.mediaView = adMedia
            binding.mainNativeadView.headlineView = adHeadline
            binding.mainNativeadView.bodyView = adBody
            binding.mainNativeadView.callToActionView = adCallToAction
            binding.mainNativeadView.iconView = adAppIcon

            (binding.mainNativeadView.headlineView as TextView).text = nativeAd.headline
            binding.mainNativeadView.mediaView?.mediaContent = nativeAd.mediaContent

            val bgColor = SpotOptionsChest.getInstance(context).getString("NativeBgColor")
            val btnColor = SpotOptionsChest.getInstance(context).getString("NativebtnColor")
            val txtColor = SpotOptionsChest.getInstance(context).getString("NativetxtColor")
            val btntxtColor = SpotOptionsChest.getInstance(context).getString("NativebtntxtColor")

            SpotFrameStyling.paintSlabBackground(context, binding.mainNativeadView, bgColor)
            SpotFrameStyling.kitPaintCtaBackground(context, binding.mainNativeadView.callToActionView, btnColor)

            (binding.mainNativeadView.headlineView as TextView).setTextColor(SpotFrameStyling.headlineColor2(context, txtColor))
            SpotFrameStyling.kitPaintDesignExtras(context, binding.mainNativeadView)
            (binding.mainNativeadView.bodyView as TextView).setTextColor(SpotFrameStyling.kitBodyColor(context, txtColor))
            (adCallToAction as TextView).setTextColor(SpotFrameStyling.ctaCaptionColor(context, btntxtColor))

            binding.mainNativeadView.bodyView?.apply {
                visibility = if (nativeAd.body == null) View.GONE else View.VISIBLE
                (binding.mainNativeadView.bodyView as TextView).text = nativeAd.body
            }

            binding.mainNativeadView.iconView?.apply {
                visibility = if (nativeAd.icon == null) View.GONE else View.VISIBLE
                (binding.mainNativeadView.iconView as ImageView).setImageDrawable(nativeAd.icon?.drawable)
            }

            binding.mainNativeadView.callToActionView?.apply {
                visibility = if (nativeAd.callToAction == null) View.GONE else View.VISIBLE
                (binding.mainNativeadView.callToActionView as TextView).text = nativeAd.callToAction
            }

            binding.mainNativeadView.setNativeAd(nativeAd)

            binding.mainNativeadView.setTag(R.id.tag_native_ad, nativeAd)
        }
    }
}
