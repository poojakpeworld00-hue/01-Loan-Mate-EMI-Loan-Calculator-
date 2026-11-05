package com.loanmate.loan.emicalculator.carloan.homelloan.ads.render.interstitial

import android.app.Activity
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.content.ContextCompat
import org.json.JSONArray
import com.loanmate.loan.emicalculator.carloan.homelloan.ads.R
import com.loanmate.loan.emicalculator.carloan.homelloan.ads.render.ProceedSpotLeader
import com.loanmate.loan.emicalculator.carloan.homelloan.ads.render.EmbeddedSpotPainter
import com.loanmate.loan.emicalculator.carloan.homelloan.ads.render.InhouseSpotLeader
import com.loanmate.loan.emicalculator.carloan.homelloan.ads.render.PrizeSpotPainter
import com.loanmate.loan.emicalculator.carloan.homelloan.ads.policy.SpotOptionsChest
import com.loanmate.loan.emicalculator.carloan.homelloan.ads.policy.SpotPullMode
import com.loanmate.loan.emicalculator.carloan.homelloan.ads.policy.SpotUnitIds
import com.loanmate.loan.emicalculator.carloan.homelloan.ads.analytics.Stats2
import com.loanmate.loan.emicalculator.carloan.homelloan.ads.host.SpotCues

object OverlayBackupChain {
    private const val DEFAULT_ORDER = "full_native,custom"







    internal fun fullEmbedded(
        activity: Activity,
        placement: String?,
        next: () -> Unit,
        done: () -> Unit,
        loadIfMissing: Boolean = false,
    ) {
        if (!EmbeddedSpotPainter.holdsCachedSpot()) {
            // On-load natives: load this step's full native now; a miss moves to the next step.
            if (!loadIfMissing && SpotPullMode.preload(activity, SpotPullMode.KitFormat.NATIVE)) return next()
            return EmbeddedSpotPainter().presentFullEmbeddedWithUnit(
                activity, SpotUnitIds.pickFullEmbedded(activity, placement), onClose = done, onMiss = next,
            )
        }
        val overlay = kitAddOverlay(activity, dark = true)
        EmbeddedSpotPainter().presentFullEmbedded(activity, overlay, onClose = {
            dropOverlay(activity, overlay)
            done()
        })
    }

    internal fun custom(activity: Activity, next: () -> Unit, done: () -> Unit) {
        if (!SpotOptionsChest.getInstance(activity).getBoolean("IsCustomADS")) return next()
        val overlay = kitAddOverlay(activity, dark = true)
        // The house ad gets its own slot: fetchHouseAd clears its container, which used to delete the close button.
        val slot = FrameLayout(activity).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
        }
        overlay.addView(slot)
        val place = SpotCues.place
        val close = attachDismissTrigger(overlay) {
            Stats2.click("custom_ad_close")
            SpotCues.send(SpotCues.CUSTOM, "close", place)
            dropOverlay(activity, overlay)
            done()
        }
        InhouseSpotLeader(place).obtainInhouseSpot(
            context = activity,
            container = slot,
            type = InhouseSpotLeader.InhouseSpotStyle.FULLSCREEN_NATIVE,
            onFail = {
                dropOverlay(activity, overlay)
                next()
            },
        )

        close.bringToFront()
    }



    private fun kitAddOverlay(activity: Activity, dark: Boolean): FrameLayout {
        val content = activity.findViewById<ViewGroup>(android.R.id.content)
        val overlay = FrameLayout(activity).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            if (dark) setBackgroundColor(android.graphics.Color.BLACK) // previous version: black
            elevation = 64f
            isClickable = true
        }
        content.addView(overlay)
        return overlay
    }

    private fun dropOverlay(activity: Activity, overlay: FrameLayout) {
        (overlay.parent as? ViewGroup)?.removeView(overlay)
    }

    private fun attachDismissTrigger(overlay: FrameLayout, onClick: () -> Unit): View {
        val density = overlay.resources.displayMetrics.density
        val size = (40 * density).toInt()
        val margin = (10 * density).toInt()
        val close = ImageView(overlay.context).apply {
            setImageResource(R.drawable.glyph_close_cross)
            setColorFilter(ContextCompat.getColor(context, R.color.on_ink))
            // Previous version geometry: top-end, 40dp from the top, 20dp from the edge.
            layoutParams = FrameLayout.LayoutParams(size, size, Gravity.TOP or Gravity.END).apply {
                setMargins(margin, (40 * density).toInt(), (20 * density).toInt(), margin)
            }
            setPadding(margin / 2, margin / 2, margin / 2, margin / 2)
            elevation = 65f
            setOnClickListener { onClick() }
        }
        overlay.addView(close)
        return close
    }
}
