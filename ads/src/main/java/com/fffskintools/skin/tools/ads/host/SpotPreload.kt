package com.fffskintools.skin.tools.ads.host

import android.app.Activity
import com.fffskintools.skin.tools.ads.policy.SpotOptionsChest
import com.fffskintools.skin.tools.ads.render.EmbeddedSpotPainter
import com.fffskintools.skin.tools.ads.render.EmbeddedStripPainter
import com.fffskintools.skin.tools.ads.render.ProceedSpotLeader
import com.fffskintools.skin.tools.ads.render.PrizeSpotPainter
import com.fffskintools.skin.tools.ads.render.interstitial.ChainOverlay
import com.fffskintools.skin.tools.ads.render.interstitial.NavigationOverlay

/** Warms every full-screen + slot ad so most places show instantly (spec §6 rule 1). Safe to call repeatedly. */
object SpotPreload {
    fun all(activity: Activity) {
        if (activity.isFinishing || activity.isDestroyed) return
        val vault = SpotOptionsChest.getInstance(activity)
        if (!vault.getBoolean("IsAdsON")) return
        runCatching { ProceedSpotLeader.loadAd(activity) }
        runCatching { NavigationOverlay().obtainInterstitials(activity) }
        // Back-button inter has its own unit (ad_units.inter_back); MSG-06 warmed it in BackPressInterstitial.
        val back = vault.getString("googleBackInter").orEmpty()
        if (vault.getBoolean("IsBack") && back.isNotBlank()) runCatching { ChainOverlay.preload(activity, back) }
        runCatching { PrizeSpotPainter.preload(activity) }
        runCatching { if (!EmbeddedSpotPainter.holdsCachedSpot()) EmbeddedSpotPainter().obtainEmbeddedSpots(activity) }
        runCatching { EmbeddedStripPainter().obtainEmbeddedStripSpots(activity) }
    }
}
