package com.loanmate.loan.emicalculator.carloan.adkit.draw

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.InsetDrawable
import android.graphics.drawable.LayerDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import com.loanmate.loan.emicalculator.carloan.adkit.R
import com.loanmate.loan.emicalculator.carloan.adkit.rules.PromoSettingsBox

/**
 * Native ad styling = the previous version's look: our layout shapes (ads_bg card, form_btn_primary CTA) are
 * kept and only tinted with the RC palette (ad_colors). Defaults and contrast rules live in [PromoTints].
 */
object PromoFrameLooks {
    @JvmStatic
    fun kitTintIfSet(view: View?, configured: String?) {
        view ?: return
        view.backgroundTintList = PromoTints.parse(configured)?.let { ColorStateList.valueOf(it) }
    }

    /** Card: tint the layout's own background shape; default white. */
    @JvmStatic
    @Suppress("UNUSED_PARAMETER")
    fun paintTileBackground(context: Context, view: View?, configured: String?) {
        view ?: return
        view.backgroundTintList = ColorStateList.valueOf(PromoTints.card(configured))
    }

    /** CTA: tint the layout's own button shape; default black. */
    @JvmStatic
    @Suppress("UNUSED_PARAMETER")
    fun kitPaintCtaBackground(context: Context, view: View?, configured: String?) {
        view ?: return
        view.backgroundTintList = ColorStateList.valueOf(PromoTints.cta(configured))
    }

    /** Headline: RC colour, default black. */
    @JvmStatic
    fun headlineColor2(context: Context, configured: String?): Int = PromoTints.text(configured, card(context))

    /** Body uses the same text colour as the headline (previous version). */
    @JvmStatic
    fun kitBodyColor(context: Context, configured: String?): Int = PromoTints.text(configured, card(context))

    /** CTA text: RC colour, default white. */
    @JvmStatic
    fun ctaLabelColor(context: Context, configured: String?): Int =
        PromoTints.ctaText(configured, PromoTints.cta(PromoSettingsBox.getInstance(context).getString("NativebtnColor")))

    /**
     * Design layouts (NativeAd.dc.html): the "Spot" badge takes the RC button colours and the card's hairline the RC
     * text colour at low alpha, so every colour of the ad still comes from `ads.colors`.
     */
    @JvmStatic
    fun kitPaintDesignExtras(context: Context, root: View) {
        val vault = PromoSettingsBox.getInstance(context)
        val cta = PromoTints.cta(vault.getString("NativebtnColor"))
        root.findViewById<TextView>(R.id.ad_badge)?.apply {
            backgroundTintList = ColorStateList.valueOf(cta)
            setTextColor(ctaLabelColor(context, vault.getString("NativebtntxtColor")))
        }
        val text = headlineColor2(context, vault.getString("NativetxtColor"))
        val dp = context.resources.displayMetrics.density
        // Stroke only, built here: tinting a shape drawable would tint its fill over the RC background too.
        root.foreground = GradientDrawable().apply {
            cornerRadius = sdp(context, 16f)
            setStroke(dp.toInt().coerceAtLeast(1), (text and 0x00FFFFFF) or 0x29000000)
        }
    }

    /**
     * Design banner: a full-width strip at least 54dp tall with a 1dp hairline on top; the banner sits centred in it.
     * Strip = RC `background`, hairline = RC `text` at low alpha.
     */
    @JvmStatic
    fun bandBar(context: Context, banner: View): FrameLayout {
        val vault = PromoSettingsBox.getInstance(context)
        val dp = context.resources.displayMetrics.density
        val text = headlineColor2(context, vault.getString("NativetxtColor"))
        (banner.parent as? ViewGroup)?.removeView(banner) // a reused banner may still sit in the previous strip
        return FrameLayout(context).apply {
            background = LayerDrawable(arrayOf(
                ColorDrawable((text and 0x00FFFFFF) or 0x29000000),
                InsetDrawable(ColorDrawable(card(context)), 0, dp.toInt().coerceAtLeast(1), 0, 0),
            ))
            minimumHeight = sdp(context, 54f).toInt()
            addView(banner, FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER))
        }
    }

    /**
     * Design dp → px scaled by screen width (360dp design), the same scale as the app's Compose UI and the
     * `@dimen/_Nsdp` values in the native layouts, so ads look the same size on every phone.
     */
    @JvmStatic
    fun sdp(context: Context, designDp: Float): Float {
        val res = context.resources
        return designDp * res.displayMetrics.density * (res.configuration.smallestScreenWidthDp / 360f).coerceIn(0.85f, 1.4f)
    }

    private fun card(context: Context): Int =
        PromoTints.card(PromoSettingsBox.getInstance(context).getString("NativeBgColor"))
}
