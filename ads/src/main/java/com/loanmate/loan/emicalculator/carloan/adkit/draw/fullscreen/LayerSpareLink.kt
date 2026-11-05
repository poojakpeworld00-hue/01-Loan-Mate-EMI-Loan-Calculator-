package com.loanmate.loan.emicalculator.carloan.adkit.draw.fullscreen

import android.app.Activity
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.content.ContextCompat
import org.json.JSONArray
import com.loanmate.loan.emicalculator.carloan.adkit.R
import com.loanmate.loan.emicalculator.carloan.adkit.draw.AdvancePromoLead
import com.loanmate.loan.emicalculator.carloan.adkit.draw.InlinePromoRenderer
import com.loanmate.loan.emicalculator.carloan.adkit.draw.NativePromoLead
import com.loanmate.loan.emicalculator.carloan.adkit.draw.RewardPromoRenderer
import com.loanmate.loan.emicalculator.carloan.adkit.rules.PromoSettingsBox
import com.loanmate.loan.emicalculator.carloan.adkit.rules.PromoFetchStyle
import com.loanmate.loan.emicalculator.carloan.adkit.rules.PromoPieceKeys
import com.loanmate.loan.emicalculator.carloan.adkit.metrics.MetricsTwo
import com.loanmate.loan.emicalculator.carloan.adkit.bridge.PromoSignals

object LayerSpareLink {
    private const val DEFAULT_ORDER = "full_native,custom"







    internal fun fullEmbedded(
        activity: Activity,
        placement: String?,
        next: () -> Unit,
        done: () -> Unit,
        loadIfMissing: Boolean = false,
    ) {
        if (!InlinePromoRenderer.holdsCachedSpot()) {
            // On-load natives: load this step's full native now; a miss moves to the next step.
            if (!loadIfMissing && PromoFetchStyle.preload(activity, PromoFetchStyle.PackShape.NATIVE)) return next()
            return InlinePromoRenderer().presentFullEmbeddedWithUnit(
                activity, PromoPieceKeys.pickFullEmbedded(activity, placement), onClose = done, onMiss = next,
            )
        }
        val overlay = kitAddOverlay(activity, dark = true)
        InlinePromoRenderer().presentFullEmbedded(activity, overlay, onClose = {
            dropOverlay(activity, overlay)
            done()
        })
    }

    internal fun custom(activity: Activity, next: () -> Unit, done: () -> Unit) {
        if (!PromoSettingsBox.getInstance(activity).getBoolean("IsCustomADS")) return next()
        val overlay = kitAddOverlay(activity, dark = true)
        // The house ad gets its own slot: fetchHouseAd clears its container, which used to delete the close button.
        val slot = FrameLayout(activity).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
        }
        overlay.addView(slot)
        val place = PromoSignals.place
        val close = attachDismissTrigger(overlay) {
            MetricsTwo.click("custom_ad_close")
            PromoSignals.send(PromoSignals.CUSTOM, "close", place)
            dropOverlay(activity, overlay)
            done()
        }
        NativePromoLead(place).obtainInhouseSpot(
            context = activity,
            container = slot,
            type = NativePromoLead.NativePromoLook.FULLSCREEN_NATIVE,
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
