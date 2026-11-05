package com.loanmate.loan.emicalculator.carloan.homelloan.ads

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.loanmate.loan.emicalculator.carloan.homelloan.ads.config.SpotNetwork
import com.loanmate.loan.emicalculator.carloan.homelloan.ads.config.KitSlot
import com.loanmate.loan.emicalculator.carloan.homelloan.ads.config.SlotSort
import com.loanmate.loan.emicalculator.carloan.homelloan.ads.host.SpotCues
import com.loanmate.loan.emicalculator.carloan.homelloan.ads.policy.SpotWaits
import com.loanmate.loan.emicalculator.carloan.homelloan.ads.render.SpotSlotBarrier
import com.loanmate.loan.emicalculator.carloan.homelloan.ads.render.BandLifecycleHook
import com.loanmate.loan.emicalculator.carloan.homelloan.ads.render.BandPainter
import com.loanmate.loan.emicalculator.carloan.homelloan.ads.render.BandSizing
import com.loanmate.loan.emicalculator.carloan.homelloan.ads.render.BandStyle
import com.loanmate.loan.emicalculator.carloan.homelloan.ads.render.InhouseSpotLeader
import com.loanmate.loan.emicalculator.carloan.homelloan.ads.render.EmbeddedSpotPainter
import com.loanmate.loan.emicalculator.carloan.homelloan.ads.render.EmbeddedStripPainter
import com.loanmate.loan.emicalculator.carloan.homelloan.ads.render.SpotFrameStyling
import com.facebook.shimmer.ShimmerFrameLayout

/**
 * An on-screen slot (`bottom_ad` / `list_ad`): shimmer while loading, then the slot's [SlotSort] on the ported
 * renderers. A failed banner shows the slot's `on_fail` type instead (when that format is on in `formats_on`).
 * Fail or no ad within `loader_wait_sec` → a custom-ad card when `custom_ads.on`, else the container
 * collapses. `slot.on` false → collapse, no request.
 */
object BottomSpotPuller {
    fun load(activity: Activity, container: FrameLayout, slot: KitSlot, placeKey: String) {
        if (activity.isFinishing || activity.isDestroyed) return
        val config = SpotKit.config
        SpotSlotBarrier.open(container)
        // Banners and natives in this slot are paused / destroyed with the screen.
        SpotSlotBarrier.dismissWithView(activity, container)
        container.setOnHierarchyChangeListener(null)
        if (!SpotKit.spotsOn() || !slot.on) return collapse(container)
        kitEdgeToEdge(container, isBand(slot.type))
        SpotsProfileApplier.kitWritePalette(activity, config)

        val customOn = config.customAds.on && config.customAds.list.isNotEmpty()
        if (config.network == SpotNetwork.CUSTOM || !kitFormatOn(slot.type)) {
            return if (customOn) showCustom(activity, container, slot.type, placeKey) else collapse(container)
        }

        val shimmer = kitShimmerFor(activity, slot.type)
        container.removeAllViews()
        container.minimumHeight = 0
        container.visibility = View.VISIBLE
        container.addView(shimmer)
        shimmer.startShimmer()

        val wait = SpotWaits.main(activity)
        container.setOnHierarchyChangeListener(object : ViewGroup.OnHierarchyChangeListener {
            override fun onChildViewAdded(parent: View?, child: View?) {
                if (child !== shimmer) wait.kitArrived()
            }

            override fun onChildViewRemoved(parent: View?, child: View?) = Unit
        })
        wait.start {
            SpotCues.send(if (isBand(slot.type)) SpotCues.BANNER else SpotCues.NATIVE, "timeout", placeKey)
            if (activity.isFinishing || activity.isDestroyed) return@start
            SpotSlotBarrier.close(container) // a late banner is dropped, not stacked over the card
            SpotSlotBarrier.open(container)
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

    private fun isBand(type: SlotSort) =
        type == SlotSort.BANNER || type == SlotSort.BANNER_INLINE || type == SlotSort.BANNER_COLLAPSIBLE

    private fun kitFormatOn(type: SlotSort) = SpotKit.config.formatsOn.let {
        when {
            isBand(type) -> it.banner
            type == SlotSort.NATIVE_BANNER -> it.nativeBanner
            else -> it.native
        }
    }

    private fun render(
        activity: Activity,
        container: FrameLayout,
        shimmer: ShimmerFrameLayout,
        type: SlotSort,
        placeKey: String,
        /** A failed banner renders this type instead of the house-ad fallback. */
        onFail: SlotSort?,
        stillWanted: () -> Boolean,
    ) {
        when (type) {
            SlotSort.BANNER, SlotSort.BANNER_INLINE, SlotSort.BANNER_COLLAPSIBLE -> BandPainter(placeKey).presentBand(
                activity = activity,
                container = container,
                type = BandStyle.AUTO,
                size = if (type == SlotSort.BANNER_INLINE) BandSizing.INLINE else BandSizing.ADAPTIVE,
                isCollapsable = type == SlotSort.BANNER_COLLAPSIBLE,
                shimmer = shimmer,
                observer = onFail?.let { next ->
                    object : BandLifecycleHook {
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
            SlotSort.NATIVE_BANNER -> EmbeddedStripPainter(placeKey).presentEmbeddedStrip(activity, container, shimmer)
            else -> {
                val native = EmbeddedSpotPainter(placeKey)
                val paint: () -> Unit = {
                    if (stillWanted() && !activity.isFinishing && !activity.isDestroyed) when (type) {
                        SlotSort.NATIVE_BIG -> native.presentLargeEmbedded(activity, container, shimmer)
                        SlotSort.NATIVE_MID -> native.presentMediumEmbedded(activity, container, shimmer)
                        SlotSort.NATIVE_MID2 -> native.presentMediumEmbeddedAlt(activity, container, shimmer)
                        else -> native.presentFullEmbedded(activity, container, onClose = { collapse(container) }, shimmer)
                    }
                }
                // MSG-06 ViewSpotPlanner.presentNative: no native cached yet → load one, render when it arrives.
                if (EmbeddedSpotPainter.holdsCachedSpot()) paint() else native.obtainEmbeddedSpots(
                    activity,
                    object : EmbeddedSpotPainter.EmbeddedSpotMonitor {
                        override fun onNativeAdLoaded() = paint()
                        override fun onNativeAdFailed() = paint()
                    },
                    placeKey,
                )
            }
        }
    }

    private fun showCustom(activity: Activity, container: FrameLayout, type: SlotSort, placeKey: String) {
        val style = when (type) {
            SlotSort.NATIVE_BIG -> InhouseSpotLeader.InhouseSpotStyle.BIG_NATIVE
            SlotSort.NATIVE_MID, SlotSort.NATIVE_MID2 -> InhouseSpotLeader.InhouseSpotStyle.MID_NATIVE
            SlotSort.NATIVE_FULL -> InhouseSpotLeader.InhouseSpotStyle.FULLSCREEN_NATIVE
            else -> InhouseSpotLeader.InhouseSpotStyle.BANNER
        }
        InhouseSpotLeader(placeKey).obtainInhouseSpot(activity, container, style)
    }

    private fun collapse(container: FrameLayout) {
        SpotSlotBarrier.destroySpots(container)
        container.removeAllViews()
        container.minimumHeight = 0
        container.visibility = View.GONE
    }

    private fun kitShimmerFor(activity: Activity, type: SlotSort): ShimmerFrameLayout {
        val dp = activity.resources.displayMetrics.density
        val heightDp = when (type) {
            SlotSort.BANNER, SlotSort.BANNER_INLINE, SlotSort.BANNER_COLLAPSIBLE -> 54
            SlotSort.NATIVE_BANNER -> 74
            SlotSort.NATIVE_MID, SlotSort.NATIVE_MID2 -> 150
            SlotSort.NATIVE_BIG -> 242 // design sizes + the 4dp / 8dp dock spacing around natives
            SlotSort.NATIVE_FULL -> 480
        }
        val block = View(activity).apply {
            background = GradientDrawable().apply {
                cornerRadius = SpotFrameStyling.sdp(activity, 16f)
                setColor(Color.parseColor("#1F888888"))
            }
        }
        return ShimmerFrameLayout(activity).apply {
            layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, SpotFrameStyling.sdp(activity, heightDp.toFloat()).toInt())
            addView(block, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        }
    }
}
