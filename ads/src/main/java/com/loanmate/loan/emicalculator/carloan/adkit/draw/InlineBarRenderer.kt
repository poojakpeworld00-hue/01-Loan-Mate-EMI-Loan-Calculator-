package com.loanmate.loan.emicalculator.carloan.adkit.draw

import com.loanmate.loan.emicalculator.carloan.adkit.R
import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import com.loanmate.loan.emicalculator.carloan.adkit.model.PromoBayType
import com.loanmate.loan.emicalculator.carloan.adkit.rules.PromoFetchStyle
import com.loanmate.loan.emicalculator.carloan.adkit.rules.PromoSettingsBox
import com.loanmate.loan.emicalculator.carloan.adkit.bridge.PromoSignals
import com.loanmate.loan.emicalculator.carloan.adkit.databinding.AdGmsEmbeddedCompactBinding
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions

/** [place] = the RC place key this strip runs for (`ad` / `ad_impression` events). */
class InlineBarRenderer(private val place: String = PromoSignals.place) {
    companion object {
        private var nativeAdBanner: NativeAd? = null
        /** Events of [nativeAdBanner]: its place is set when it goes on screen. */
        private var stripEvents: PromoSignals.PackListener? = null
    }

    /**
     * Warms the strip native. On-load natives (PromoFetchStyle) warm nothing; a slot then calls this
     * with [now] = true to load its own, and hears back through [onLoaded] / [onFailed].
     */
    fun obtainEmbeddedStripSpots(
        activity: Activity,
        now: Boolean = false,
        onLoaded: () -> Unit = {},
        onFailed: () -> Unit = {},
    ) {
        val adsPref = PromoSettingsBox.getInstance(activity)
        if (!now && !PromoFetchStyle.preload(activity, PromoFetchStyle.PackShape.NATIVE)) return
        if (!adsPref.getBoolean("IsAdsON")) return onFailed()

        if (!adsPref.getBoolean("NativeBannerPresenter")) return onFailed()

        when (PromoBayType.fromString(adsPref.getString("IsAdType"))) {
            PromoBayType.GOOGLE -> {
                val adUnitId = adsPref.getString("googleNative") ?: return onFailed()

                // A slot's own load ([now]) runs for its place; a warm-up preload has none.
                val events = object : PromoSignals.PackListener(PromoSignals.NATIVE, if (now) place else "") {
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        super.onAdFailedToLoad(error)
                        Log.e("NativeStripRenderer", "Ad failed to load: ${error.message}")
                        nativeAdBanner = null
                        onFailed()
                    }
                }
                val adLoader = AdLoader.Builder(activity, adUnitId).forNativeAd { ad ->
                    nativeAdBanner?.destroy()
                    nativeAdBanner = ad
                    stripEvents = events
                    PromoSignals.send(PromoSignals.NATIVE, "loaded", events.place)

                    Log.d("NativeStripRenderer", "Ad loaded successfully")
                    onLoaded()
                }.withAdListener(events).withNativeAdOptions(NativeAdOptions.Builder().build()).build()

                PromoSignals.send(PromoSignals.NATIVE, "request", events.place)
                adLoader.loadAd(AdRequest.Builder().build())
            }


            PromoBayType.UNKNOWN, PromoBayType.CUSTOM -> {
                Log.e("NativeStripRenderer", "AdSlotKind Custom NOt Pre load Google Native")
                return onFailed()
            }
        }
    }

    fun presentEmbeddedStrip(
        context: Activity, layout: FrameLayout, shimmer: ShimmerFrameLayout? = null
    ) {
        Log.e("NativeAds", "Google Show: nativeAd")
        val adsPref = PromoSettingsBox.getInstance(context)

        if (context.isFinishing || context.isDestroyed) return

        if (!kitHasNetwork(context)
            || !adsPref.getBoolean("IsAdsON")
            || !adsPref.getBoolean("NativeBannerPresenter")
        ) {
            layout.removeAllViews()
            layout.invisible()
            shimmer?.stopShimmer()
            shimmer?.isVisible = false
            return
        }


        layout.visible()
        shimmer?.startShimmer()
        shimmer?.isVisible = true

        when (PromoBayType.fromString(adsPref.getString("IsAdType"))) {
            PromoBayType.GOOGLE -> {
                layout.post {
                    try {
                        if (context.isFinishing || context.isDestroyed) return@post
                        if (nativeAdBanner != null) {
                            presentCached(context, layout, shimmer)
                            return@post
                        } else {
                            // PackNothing cached (on-load mode, or the warmed strip was already used): load this slot's strip now.
                            obtainEmbeddedStripSpots(
                                context, now = true,
                                onLoaded = {
                                    if (!context.isFinishing && !context.isDestroyed && layout.isAttachedToWindow) {
                                        presentCached(context, layout, shimmer)
                                    }
                                },
                                onFailed = { if (!context.isFinishing && !context.isDestroyed) kitStripMissing(context, layout, shimmer) },
                            )
                        }
                    } catch (e: Exception) {
                        Log.e("NativeStripRenderer", "Google NativeStripRenderer failed: ${e.message}")
                    }
                }
            }


            PromoBayType.UNKNOWN, PromoBayType.CUSTOM -> {
                layout.removeAllViews()
                shimmer?.stopShimmer()
                shimmer?.isVisible = false
                NativePromoLead(place).obtainInhouseSpot(
                    context, layout, NativePromoLead.NativePromoLook.BANNER
                )
            }
        }
    }

    private fun presentCached(context: Activity, layout: FrameLayout, shimmer: ShimmerFrameLayout?) {
        val ad = nativeAdBanner ?: return kitStripMissing(context, layout, shimmer)
        val binding = AdGmsEmbeddedCompactBinding.inflate(context.layoutInflater)
        populateGmsEmbedded(ad, binding, context)

        layout.removeAllViews()
        shimmer?.stopShimmer()
        shimmer?.isVisible = false

        layout.addView(binding.root)
        stripEvents?.place = place
        PromoSignals.send(PromoSignals.NATIVE, "show", place)
        nativeAdBanner = null
        obtainEmbeddedStripSpots(context)
    }

    private fun kitStripMissing(context: Activity, layout: FrameLayout, shimmer: ShimmerFrameLayout?) {
        layout.removeAllViews()
        shimmer?.stopShimmer()
        shimmer?.isVisible = false
        NativePromoLead(place).obtainInhouseSpot(context, layout, NativePromoLead.NativePromoLook.BANNER)
    }

    private fun populateGmsEmbedded(
        nativeAd: NativeAd, binding: AdGmsEmbeddedCompactBinding, context: Activity
    ) {
        val unit = PromoSettingsBox.getInstance(context).getString("googleNative")
        nativeAd.setOnPaidEventListener { PromoSignals.paid(it, "native_strip", unit, nativeAd.responseInfo, place) }

        binding.apply {
            mainNativeadView.headlineView = adHeadline
            mainNativeadView.bodyView = adBody
            mainNativeadView.callToActionView = adCallToAction
            mainNativeadView.iconView = adAppIcon

            (adHeadline as TextView).text = nativeAd.headline

            val bgColor = PromoSettingsBox.getInstance(context).getString("NativeBgColor")
            val btnColor = PromoSettingsBox.getInstance(context).getString("NativebtnColor")

            val txtColor =
                PromoSettingsBox.getInstance(context).getString("NativetxtColor")

            (binding.mainNativeadView.headlineView as TextView).apply {
                setTextColor(PromoFrameLooks.headlineColor2(context, txtColor))
            }

            (binding.mainNativeadView.bodyView as TextView).apply {
                setTextColor(PromoFrameLooks.kitBodyColor(context, txtColor))
            }

            val btntxtColor =
                PromoSettingsBox.getInstance(context).getString("NativebtntxtColor")

            (adCallToAction as TextView).apply {
                setTextColor(PromoFrameLooks.ctaCaptionColor(context, btntxtColor))
            }

            PromoFrameLooks.paintSlabBackground(context, mainNativeadView, bgColor)

            PromoFrameLooks.kitPaintCtaBackground(context, adCallToAction, btnColor)
            PromoFrameLooks.kitPaintDesignExtras(context, mainNativeadView)

            if (nativeAd.body != null) {
                adBody.visibility = View.VISIBLE
                (adBody as TextView).text = nativeAd.body
            } else {
                adBody.visibility = View.GONE
            }

            if (nativeAd.icon != null) {
                adAppIcon.visibility = View.VISIBLE
                (adAppIcon as ImageView).setImageDrawable(nativeAd.icon?.drawable)
            } else {
                adAppIcon.visibility = View.GONE
            }

            if (nativeAd.callToAction != null) {
                adCallToAction.visibility = View.VISIBLE
                (adCallToAction as TextView).text = nativeAd.callToAction
            } else {
                adCallToAction.visibility = View.GONE
            }

            mainNativeadView.setNativeAd(nativeAd)

            mainNativeadView.setTag(R.id.tag_native_ad, nativeAd)
        }
    }

}
