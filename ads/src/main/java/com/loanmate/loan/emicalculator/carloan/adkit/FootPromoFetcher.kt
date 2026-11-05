package com.loanmate.loan.emicalculator.carloan.adkit

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.loanmate.loan.emicalculator.carloan.adkit.setup.PromoNet
import com.loanmate.loan.emicalculator.carloan.adkit.setup.PackBay
import com.loanmate.loan.emicalculator.carloan.adkit.setup.BayOrder
import com.loanmate.loan.emicalculator.carloan.adkit.bridge.PromoSignals
import com.loanmate.loan.emicalculator.carloan.adkit.rules.PromoDelays
import com.loanmate.loan.emicalculator.carloan.adkit.draw.PromoBayWall
import com.loanmate.loan.emicalculator.carloan.adkit.draw.StripLifeTap
import com.loanmate.loan.emicalculator.carloan.adkit.draw.StripRenderer
import com.loanmate.loan.emicalculator.carloan.adkit.draw.StripDims
import com.loanmate.loan.emicalculator.carloan.adkit.draw.StripLook
import com.loanmate.loan.emicalculator.carloan.adkit.draw.NativePromoLead
import com.loanmate.loan.emicalculator.carloan.adkit.draw.InlinePromoRenderer
import com.loanmate.loan.emicalculator.carloan.adkit.draw.InlineBarRenderer
import com.loanmate.loan.emicalculator.carloan.adkit.draw.PromoFrameLooks
import com.facebook.shimmer.ShimmerFrameLayout

/**
 * An on-screen slot (`bottom_ad` / `list_ad`): shimmer while loading, then the slot's [BayOrder] on the ported
 * renderers. A failed banner shows the slot's `on_fail` type instead (when that format is on in `formats_on`).
 * Fail or no ad within `loader_wait_sec` → a custom-ad card when `custom_ads.on`, else the container
 * collapses. `slot.on` false → collapse, no request.
 */
object FootPromoFetcher {
    fun load(activity: Activity, container: FrameLayout, slot: PackBay, placeKey: String) {
        if (activity.isFinishing || activity.isDestroyed) return
        val config = PromoPack.config
        PromoBayWall.open(container)
        // Banners and natives in this slot are paused / destroyed with the screen.
        PromoBayWall.dismissWithView(activity, container)
        container.setOnHierarchyChangeListener(null)
        if (!PromoPack.spotsOn() || !slot.on) return collapse(container)
        kitEdgeToEdge(container, isBand(slot.type))
        PromosSpecSetter.kitWritePalette(activity, config)

        val customOn = config.customAds.on && config.customAds.list.isNotEmpty()
        if (config.network == PromoNet.CUSTOM || !kitFormatOn(slot.type)) {
            return if (customOn) showCustom(activity, container, slot.type, placeKey) else collapse(container)
        }

        val shimmer = kitShimmerFor(activity, slot.type)
        container.removeAllViews()
        container.minimumHeight = 0
        container.visibility = View.VISIBLE
        container.addView(shimmer)
        shimmer.startShimmer()

        val wait = PromoDelays.main(activity)
        container.setOnHierarchyChangeListener(object : ViewGroup.OnHierarchyChangeListener {
            override fun onChildViewAdded(parent: View?, child: View?) {
                if (child !== shimmer) wait.kitArrived()
            }

            override fun onChildViewRemoved(parent: View?, child: View?) = Unit
        })
        wait.start {
            PromoSignals.send(if (isBand(slot.type)) PromoSignals.BANNER else PromoSignals.NATIVE, "timeout", placeKey)
            if (activity.isFinishing || activity.isDestroyed) return@start
            PromoBayWall.close(container) // a late banner is dropped, not stacked over the card
            PromoBayWall.open(container)
            if (customOn) showCustom(activity, container, slot.type, placeKey) else collapse(container)
        }
        val onFail = slot.onFail?.takeIf { isBand(slot.type) && !isBand(it) && kitFormatOn(it) }
        render(activity, container, shimmer, slot.type, placeKey, onFail) { !wait.timedOut }
    }

    /** A banner fills the slot's width with no margin; other types keep the margins the layout gave the slot. */
    private fun kitEdgeToEdge(container: FrameLayout, on: Boolean) {
        val lp = container.layoutParams as? ViewGroup.MarginLayoutParams ?: return
        val layout = container.getTag(R.id.tag_ad_slot_margins) as? IntArray
            ?: intArrayOf(lp.leftMargin, lp.topMargin, lp.rightMargin, lp.bottomMargin).also { container.setTag(R.id.tag_ad_slot_margins, it) }
        if (on) lp.setMargins(0, layout[1], 0, 0) else lp.setMargins(layout[0], layout[1], layout[2], layout[3])
        container.layoutParams = lp
    }

    private fun isBand(type: BayOrder) =
        type == BayOrder.BANNER || type == BayOrder.BANNER_INLINE || type == BayOrder.BANNER_COLLAPSIBLE

    private fun kitFormatOn(type: BayOrder) = PromoPack.config.formatsOn.let {
        when {
            isBand(type) -> it.banner
            type == BayOrder.NATIVE_BANNER -> it.nativeBanner
            else -> it.native
        }
    }

    private fun render(
        activity: Activity,
        container: FrameLayout,
        shimmer: ShimmerFrameLayout,
        type: BayOrder,
        placeKey: String,
        /** A failed banner renders this type instead of the house-ad fallback. */
        onFail: BayOrder?,
        stillWanted: () -> Boolean,
    ) {
        when (type) {
            BayOrder.BANNER, BayOrder.BANNER_INLINE, BayOrder.BANNER_COLLAPSIBLE -> StripRenderer(placeKey).presentBand(
                activity = activity,
                container = container,
                type = StripLook.AUTO,
                size = if (type == BayOrder.BANNER_INLINE) StripDims.INLINE else StripDims.ADAPTIVE,
                isCollapsable = type == BayOrder.BANNER_COLLAPSIBLE,
                shimmer = shimmer,
                observer = onFail?.let { next ->
                    object : StripLifeTap {
                        override fun onAdFailed() {
                            if (!stillWanted() || activity.isFinishing || activity.isDestroyed) return
                            shimmer.layoutParams = kitShimmerFor(activity, next).layoutParams
                            kitEdgeToEdge(container, false)
                            render(activity, container, shimmer, next, placeKey, null, stillWanted)
                        }
                    }
                },
                disableInternalFallback = onFail != null,
            )
            BayOrder.NATIVE_BANNER -> InlineBarRenderer(placeKey).presentEmbeddedStrip(activity, container, shimmer)
            else -> {
                val native = InlinePromoRenderer(placeKey)
                val paint: () -> Unit = {
                    if (stillWanted() && !activity.isFinishing && !activity.isDestroyed) when (type) {
                        BayOrder.NATIVE_BIG -> native.presentLargeEmbedded(activity, container, shimmer)
                        BayOrder.NATIVE_MID -> native.presentMediumEmbedded(activity, container, shimmer)
                        BayOrder.NATIVE_MID2 -> native.presentMediumEmbeddedAlt(activity, container, shimmer)
                        else -> native.presentFullEmbedded(activity, container, onClose = { collapse(container) }, shimmer)
                    }
                }
                // MSG-06 ScreenPromoMapper.presentNative: no native cached yet → load one, render when it arrives.
                if (InlinePromoRenderer.holdsCachedSpot()) paint() else native.obtainEmbeddedSpots(
                    activity,
                    object : InlinePromoRenderer.InlinePromoTracker {
                        override fun onNativeAdLoaded() = paint()
                        override fun onNativeAdFailed() = paint()
                    },
                    placeKey,
                )
            }
        }
    }

    private fun showCustom(activity: Activity, container: FrameLayout, type: BayOrder, placeKey: String) {
        val style = when (type) {
            BayOrder.NATIVE_BIG -> NativePromoLead.NativePromoLook.BIG_NATIVE
            BayOrder.NATIVE_MID, BayOrder.NATIVE_MID2 -> NativePromoLead.NativePromoLook.MID_NATIVE
            BayOrder.NATIVE_FULL -> NativePromoLead.NativePromoLook.FULLSCREEN_NATIVE
            else -> NativePromoLead.NativePromoLook.BANNER
        }
        NativePromoLead(placeKey).obtainInhouseSpot(activity, container, style)
    }

    private fun collapse(container: FrameLayout) {
        PromoBayWall.destroySpots(container)
        container.removeAllViews()
        container.minimumHeight = 0
        container.visibility = View.GONE
    }

    private fun kitShimmerFor(activity: Activity, type: BayOrder): ShimmerFrameLayout {
        val dp = activity.resources.displayMetrics.density
        val heightDp = when (type) {
            BayOrder.BANNER, BayOrder.BANNER_INLINE, BayOrder.BANNER_COLLAPSIBLE -> 54
            BayOrder.NATIVE_BANNER -> 74
            BayOrder.NATIVE_MID, BayOrder.NATIVE_MID2 -> 150
            BayOrder.NATIVE_BIG -> 242 // design sizes + the 4dp / 8dp dock spacing around natives
            BayOrder.NATIVE_FULL -> 480
        }
        val block = View(activity).apply {
            background = GradientDrawable().apply {
                cornerRadius = PromoFrameLooks.sdp(activity, 16f)
                setColor(Color.parseColor("#1F888888"))
            }
        }
        return ShimmerFrameLayout(activity).apply {
            layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, PromoFrameLooks.sdp(activity, heightDp.toFloat()).toInt())
            addView(block, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        }
    }
}
