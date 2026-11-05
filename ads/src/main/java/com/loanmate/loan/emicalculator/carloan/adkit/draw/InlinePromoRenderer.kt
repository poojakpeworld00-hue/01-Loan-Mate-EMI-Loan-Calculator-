package com.loanmate.loan.emicalculator.carloan.adkit.draw

import com.loanmate.loan.emicalculator.carloan.adkit.PromoPack
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
import com.loanmate.loan.emicalculator.carloan.adkit.model.PromoBayType
import com.loanmate.loan.emicalculator.carloan.adkit.model.PromoBayType.*
import com.loanmate.loan.emicalculator.carloan.adkit.rules.PromoFetchStyle
import com.loanmate.loan.emicalculator.carloan.adkit.rules.PromoSettingsBox
import com.loanmate.loan.emicalculator.carloan.adkit.rules.PromoPieceKeys
import com.loanmate.loan.emicalculator.carloan.adkit.rules.PromoDelays
import com.loanmate.loan.emicalculator.carloan.adkit.rules.ScreenPromoMapper
import com.loanmate.loan.emicalculator.carloan.adkit.metrics.MetricsTwo
import com.loanmate.loan.emicalculator.carloan.adkit.bridge.PromoSignals
import com.loanmate.loan.emicalculator.carloan.adkit.databinding.AduGmsEmbeddedLargeBinding
import androidx.core.content.ContextCompat
import com.loanmate.loan.emicalculator.carloan.adkit.R
import com.loanmate.loan.emicalculator.carloan.adkit.databinding.AduGmsEmbeddedFullpageBinding
import com.loanmate.loan.emicalculator.carloan.adkit.databinding.AduGmsEmbeddedMediumAltBinding
import com.loanmate.loan.emicalculator.carloan.adkit.databinding.AduGmsEmbeddedMediumBinding
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions

/** [place] = the RC place key this native runs for (`ad` / `ad_impression` events). */
class InlinePromoRenderer(private val place: String = PromoSignals.place) {
    interface InlinePromoTracker {
        fun onNativeAdLoaded()
        fun onNativeAdFailed()
    }

    companion object {
        private var nativeAd: NativeAd? = null
        /** Events and unit of the cached [nativeAd]: its place is set when it goes on screen. */
        private var cachedEvents: PromoSignals.PackListener? = null
        private var cachedUnit = ""

        var isFullNativeShowing: Boolean = false

        fun holdsCachedSpot(): Boolean = nativeAd != null

        fun isLoading(): Boolean = sharedLoad.inFlight

        /** One native load at a time; preload and screens share it instead of racing separate loads. */
        private val sharedLoad = com.loanmate.loan.emicalculator.carloan.adkit.rules.CommonFetch<Boolean>()
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
        if (PromoFetchStyle.preload(context, PromoFetchStyle.PackShape.NATIVE)) return missing()
        kitPresentWithUnit(context, ScreenPromoMapper.embeddedUnitId(context), format, layout, shimmer) { missing() }
    }

    private fun Activity.kitIsActivityGone(): Boolean {
        if (isFinishing) return true
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && isDestroyed
    }

    /** [place]: the slot waiting for this load; "" for a warm-up preload. */
    fun obtainEmbeddedSpots(context: Activity, observer: InlinePromoTracker? = null, place: String = "") {
        val adsPreference = PromoSettingsBox.getInstance(context)

        // On-load natives (PromoFetchStyle): nothing is warmed; each slot loads its own when shown.
        if (!PromoFetchStyle.preload(context, PromoFetchStyle.PackShape.NATIVE)) {
            observer?.onNativeAdFailed()
            return
        }

        if (!adsPreference.getBoolean("IsAdsON") || PromoBayType.fromString(adsPreference.getString("IsAdType")) != PromoBayType.GOOGLE) {
            observer?.onNativeAdFailed()
            return
        }

        if (!adsPreference.getBoolean("NativeAd")) {
            observer?.onNativeAdFailed()
            return
        }

        val adUnit = ScreenPromoMapper.embeddedUnitId(context)
        if (adUnit.isEmpty()) {
            observer?.onNativeAdFailed()
            return
        }
        val starts = sharedLoad.join { ok ->
            if (context.kitIsActivityGone()) return@join // each requester's own screen must still be alive
            if (ok) observer?.onNativeAdLoaded() else observer?.onNativeAdFailed()
        }
        if (!starts) return // a load is already running; this observer hears its result
        val events = object : PromoSignals.PackListener(PromoSignals.NATIVE, place) {
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
                    PromoSignals.send(PromoSignals.NATIVE, "loaded", place)
                    // Waiters check their own activity; the cached ad is kept for the next screen either way.
                    sharedLoad.finish(true)

                    Log.e("NativeAds", "Google Load: nativeAd")
                }
                .withAdListener(events)
                .withNativeAdOptions(NativeAdOptions.Builder().build())
                .build()

        PromoSignals.send(PromoSignals.NATIVE, "request", place)
        adLoader.loadAd(AdRequest.Builder().build())
    }

    /** The cached ad goes on screen here: its show, impression, click and paid events carry this [place]. */
    private fun kitShownCached(ad: NativeAd, label: String) {
        cachedEvents?.place = place
        PromoSignals.send(PromoSignals.NATIVE, "show", place)
        val unit = cachedUnit
        ad.setOnPaidEventListener { PromoSignals.paid(it, label, unit, ad.responseInfo, place) }
    }

    fun kitPresentWithUnit(
        context: Activity,
        unitId: String,
        format: String,
        layout: FrameLayout,
        shimmer: ShimmerFrameLayout? = null,
        fallback: () -> Unit,
    ) {
        val pref = PromoSettingsBox.getInstance(context)
        val kind = when (format.lowercase()) {
            "bignative", "big_native", "big" -> 0
            "mediumnative", "mid_native", "mid" -> 1
            "mediumnative2", "mid_native_2", "mid2" -> 2
            else -> -1
        }
        if (unitId.isBlank() || kind < 0 || context.kitIsActivityGone() || !kitHasNetwork(context) ||
            !pref.getBoolean("IsAdsON") || !pref.getBoolean("NativeAd") ||
            PromoBayType.fromString(pref.getString("IsAdType")) != PromoBayType.GOOGLE
        ) return fallback()

        layout.visible()
        shimmer?.startShimmer()
        shimmer?.isVisible = true
        PromoSignals.send(PromoSignals.NATIVE, "request", place)
        AdLoader.Builder(context, unitId)
            .forNativeAd { ad ->
                PromoSignals.send(PromoSignals.NATIVE, "loaded", place)
                if (context.kitIsActivityGone() || !layout.isAttachedToWindow) {
                    ad.destroy()
                    return@forNativeAd
                }
                val root: View = when (kind) {
                    0 -> AduGmsEmbeddedLargeBinding.inflate(context.layoutInflater)
                        .also { largeEmbeddedLayout(ad, it, context) }.root
                    1 -> AduGmsEmbeddedMediumBinding.inflate(context.layoutInflater)
                        .also { mediumEmbeddedLayout(ad, it, context) }.root
                    else -> AduGmsEmbeddedMediumAltBinding.inflate(context.layoutInflater)
                        .also { mediumEmbeddedLayoutAlt(ad, it, context) }.root
                }
                ad.setOnPaidEventListener { PromoSignals.paid(it, "native_unit", unitId, ad.responseInfo, place) }
                layout.removeAllViews()
                shimmer?.stopShimmer()
                shimmer?.isVisible = false
                layout.addView(root)
                PromoSignals.send(PromoSignals.NATIVE, "show", place)
            }
            .withAdListener(object : PromoSignals.PackListener(PromoSignals.NATIVE, place) {
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
        val adsPreference = PromoSettingsBox.getInstance(context)

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

        when (PromoBayType.fromString(adsPreference.getString("IsAdType"))) {
            PromoBayType.GOOGLE -> {
                layout.post {
                    try {
                        if (context.isFinishing || context.isDestroyed) return@post

                        if (nativeAd != null) {
                            val rootView: View = run {
                                val binding = AduGmsEmbeddedLargeBinding.inflate(context.layoutInflater)
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

                            NativePromoLead(place).obtainInhouseSpot(
                                context,
                                layout,
                                NativePromoLead.NativePromoLook.BIG_NATIVE,
                                imageView
                            )
                        }
                    } catch (e: Exception) {
                        Log.e("NativeAds", "Google BigNative crash", e)
                        PromoPack.onError(e, "native.big")
                        shimmer?.stopShimmer()
                        shimmer?.visibility = View.GONE
                        layout.removeAllViews()
                        layout.visibility = View.GONE
                    }
                }
            }


            PromoBayType.UNKNOWN, PromoBayType.CUSTOM -> {
                layout.removeAllViews()
                shimmer?.stopShimmer()
                shimmer?.isVisible = false
                NativePromoLead(place).obtainInhouseSpot(
                    context,
                    layout,
                    NativePromoLead.NativePromoLook.BIG_NATIVE,
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
        val adsPreference = PromoSettingsBox.getInstance(context)

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

        if (PromoBayType.fromString(adsPreference.getString("IsAdType")) != PromoBayType.GOOGLE) {
            onClose()
            return
        }

        layout.post {
            try {
                if (context.isFinishing || context.isDestroyed) return@post

                if (nativeAd != null) {
                    val binding = AduGmsEmbeddedFullpageBinding.inflate(context.layoutInflater)

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
                if (!PromoFetchStyle.preload(context, PromoFetchStyle.PackShape.NATIVE)) {
                    presentFullEmbeddedWithUnit(context, PromoPieceKeys.pickFullEmbedded(context, null), onClose = onClose)
                } else {
                    onClose()
                }
            } catch (e: Exception) {
                Log.e("NativeAds", "Google FullNative crash", e)
                PromoPack.onError(e, "native.full")
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

        val pref = PromoSettingsBox.getInstance(activity)
        if (!pref.getBoolean("IsAdsON") || !pref.getBoolean("NativeAd") || unitId.isBlank()) return miss()
        if (PromoBayType.fromString(pref.getString("IsAdType")) != PromoBayType.GOOGLE) return miss()
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
        val wait = PromoDelays.main(activity)
        wait.start {
            if (fired) return@start
            Log.d("NativeAds", "FullNative unit $unitId timed out — skip")
            PromoSignals.send(PromoSignals.NATIVE, "timeout", place)
            dropOverlay()
            miss()
        }

        PromoSignals.send(PromoSignals.NATIVE, "request", place)
        AdLoader.Builder(activity, unitId)
            .forNativeAd { ad ->
                PromoSignals.send(PromoSignals.NATIVE, "loaded", place)
                if (!wait.kitArrived() || fired || activity.isFinishing || activity.isDestroyed) {
                    ad.destroy(); dropOverlay(); return@forNativeAd miss()
                }
                val binding = AduGmsEmbeddedFullpageBinding.inflate(activity.layoutInflater)
                isFullNativeShowing = true
                fullpageEmbeddedLayout(ad, binding, activity) {
                    isFullNativeShowing = false
                    ad.destroy()
                    dropOverlay()
                    done()
                }
                overlay.removeAllViews()
                overlay.addView(binding.root)
                PromoSignals.send(PromoSignals.NATIVE, "show", place)
                ad.setOnPaidEventListener { PromoSignals.paid(it, "native_full_unit", unitId, ad.responseInfo, place) }
            }
            .withAdListener(object : PromoSignals.PackListener(PromoSignals.NATIVE, place) {
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
        binding: AduGmsEmbeddedFullpageBinding,
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
                MetricsTwo.click("native_ad_close")
                PromoSignals.send(PromoSignals.NATIVE, "close", place)
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
        binding: AduGmsEmbeddedLargeBinding,
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

            val bgColor = PromoSettingsBox.getInstance(context).getString("NativeBgColor")
            val btnColor = PromoSettingsBox.getInstance(context).getString("NativebtnColor")
            val txtColor = PromoSettingsBox.getInstance(context).getString("NativetxtColor")
            val btntxtColor = PromoSettingsBox.getInstance(context).getString("NativebtntxtColor")

            PromoFrameLooks.paintSlabBackground(context, binding.mainNativeadView, bgColor)
            PromoFrameLooks.kitPaintCtaBackground(context, binding.mainNativeadView.callToActionView, btnColor)

            (binding.mainNativeadView.headlineView as TextView).setTextColor(PromoFrameLooks.headlineColor2(context, txtColor))
            PromoFrameLooks.kitPaintDesignExtras(context, binding.mainNativeadView)
            (binding.mainNativeadView.bodyView as TextView).setTextColor(PromoFrameLooks.kitBodyColor(context, txtColor))
            (adCallToAction as TextView).setTextColor(PromoFrameLooks.ctaCaptionColor(context, btntxtColor))

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
        val adsPref = PromoSettingsBox.getInstance(context)

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

        when (PromoBayType.fromString(adsPref.getString("IsAdType"))) {
            PromoBayType.GOOGLE -> {
                layout.post {
                    try {
                        if (context.isFinishing || context.isDestroyed) return@post
                        if (nativeAd != null) {
                            val binding = AduGmsEmbeddedMediumBinding.inflate(context.layoutInflater)
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
                            NativePromoLead(place).obtainInhouseSpot(
                                context,
                                layout,
                                NativePromoLead.NativePromoLook.MID_NATIVE
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


            PromoBayType.CUSTOM, PromoBayType.UNKNOWN -> {
                layout.removeAllViews()
                shimmer?.stopShimmer()
                shimmer?.isVisible = false
                NativePromoLead(place).obtainInhouseSpot(
                    context,
                    layout,
                    NativePromoLead.NativePromoLook.MID_NATIVE
                )
            }
        }
    }

    private fun mediumEmbeddedLayout(
        nativeAd: NativeAd,
        binding: AduGmsEmbeddedMediumBinding,
        context: Activity
    ) {
        binding.apply {
            binding.mainNativeadView.headlineView = adHeadline
            binding.mainNativeadView.bodyView = adBody
            binding.mainNativeadView.callToActionView = adCallToAction
            binding.mainNativeadView.iconView = adAppIcon

            (binding.mainNativeadView.headlineView as TextView).text = nativeAd.headline

            val bgColor = PromoSettingsBox.getInstance(context).getString("NativeBgColor")
            val btnColor = PromoSettingsBox.getInstance(context).getString("NativebtnColor")
            val txtColor = PromoSettingsBox.getInstance(context).getString("NativetxtColor")
            val btntxtColor = PromoSettingsBox.getInstance(context).getString("NativebtntxtColor")

            PromoFrameLooks.paintSlabBackground(context, binding.mainNativeadView, bgColor)
            PromoFrameLooks.kitPaintCtaBackground(context, binding.mainNativeadView.callToActionView, btnColor)

            (binding.mainNativeadView.headlineView as TextView).setTextColor(PromoFrameLooks.headlineColor2(context, txtColor))
            PromoFrameLooks.kitPaintDesignExtras(context, binding.mainNativeadView)
            (binding.mainNativeadView.bodyView as TextView).setTextColor(PromoFrameLooks.kitBodyColor(context, txtColor))
            (adCallToAction as TextView).setTextColor(PromoFrameLooks.ctaCaptionColor(context, btntxtColor))

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
        val adsPref = PromoSettingsBox.getInstance(context)

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
        when (PromoBayType.fromString(adsPref.getString("IsAdType"))) {
            GOOGLE -> {
                layout.post {
                    try {
                        if (context.isFinishing || context.isDestroyed) return@post
                        if (Companion.nativeAd != null) {
                            val binding = AduGmsEmbeddedMediumAltBinding.inflate(context.layoutInflater)
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
                            NativePromoLead(place).obtainInhouseSpot(
                                context,
                                layout,
                                NativePromoLead.NativePromoLook.MID_NATIVE
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
                NativePromoLead(place).obtainInhouseSpot(
                    context,
                    layout,
                    NativePromoLead.NativePromoLook.MID_NATIVE
                )
            }

        }
    }

    private fun mediumEmbeddedLayoutAlt(
        nativeAd: NativeAd,
        binding: AduGmsEmbeddedMediumAltBinding,
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

            val bgColor = PromoSettingsBox.getInstance(context).getString("NativeBgColor")
            val btnColor = PromoSettingsBox.getInstance(context).getString("NativebtnColor")
            val txtColor = PromoSettingsBox.getInstance(context).getString("NativetxtColor")
            val btntxtColor = PromoSettingsBox.getInstance(context).getString("NativebtntxtColor")

            PromoFrameLooks.paintSlabBackground(context, binding.mainNativeadView, bgColor)
            PromoFrameLooks.kitPaintCtaBackground(context, binding.mainNativeadView.callToActionView, btnColor)

            (binding.mainNativeadView.headlineView as TextView).setTextColor(PromoFrameLooks.headlineColor2(context, txtColor))
            PromoFrameLooks.kitPaintDesignExtras(context, binding.mainNativeadView)
            (binding.mainNativeadView.bodyView as TextView).setTextColor(PromoFrameLooks.kitBodyColor(context, txtColor))
            (adCallToAction as TextView).setTextColor(PromoFrameLooks.ctaCaptionColor(context, btntxtColor))

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
