package com.loanmate.loan.emicalculator.carloan.adkit.bridge

import android.app.Activity
import com.loanmate.loan.emicalculator.carloan.adkit.rules.PromoSettingsBox
import com.loanmate.loan.emicalculator.carloan.adkit.draw.InlinePromoRenderer
import com.loanmate.loan.emicalculator.carloan.adkit.draw.InlineBarRenderer
import com.loanmate.loan.emicalculator.carloan.adkit.draw.AdvancePromoLead
import com.loanmate.loan.emicalculator.carloan.adkit.draw.RewardPromoRenderer
import com.loanmate.loan.emicalculator.carloan.adkit.draw.fullscreen.LinkLayer
import com.loanmate.loan.emicalculator.carloan.adkit.draw.fullscreen.NavLayer

/** Warms every full-screen + slot ad so most places show instantly (spec §6 rule 1). Safe to call repeatedly. */
object PromoPrefetch {
    fun all(activity: Activity) {
        if (activity.isFinishing || activity.isDestroyed) return
        val vault = PromoSettingsBox.getInstance(activity)
        if (!vault.getBoolean("IsAdsON")) return
        runCatching { AdvancePromoLead.loadAd(activity) }
        runCatching { NavLayer().obtainInterstitials(activity) }
        // Back-button inter has its own unit (ad_units.inter_back); MSG-06 warmed it in BackPressInterstitial.
        val back = vault.getString("googleBackInter").orEmpty()
        if (vault.getBoolean("IsBack") && back.isNotBlank()) runCatching { LinkLayer.preload(activity, back) }
        runCatching { RewardPromoRenderer.preload(activity) }
        runCatching { if (!InlinePromoRenderer.holdsCachedPromo()) InlinePromoRenderer().obtainInlinePromos(activity) }
        runCatching { InlineBarRenderer().obtainInlineBarPromos(activity) }
    }
}
