package com.fffskintools.skin.tools.ads.host

import com.fffskintools.skin.tools.ads.config.SpotPhase
import org.junit.Assert.assertEquals
import org.junit.Test

class SpotCuesTest {
    @Test
    fun kitErrorCode3IsNoFillOthersFailed() {
        assertEquals("no_fill", SpotCues.failPhase(3))
        assertEquals("failed", SpotCues.failPhase(0))
        assertEquals("failed", SpotCues.failPhase(2))
    }

    @Test
    fun engineLabelsMapToFirebaseSpotFormat() {
        mapOf(
            "banner" to "banner",
            "native_big" to "native", "native_medium2" to "native", "native_full_unit" to "native", "native_strip" to "native",
            "inter_chain" to "interstitial", "inter_navigation" to "interstitial",
            "rewarded" to "rewarded", "rewarded_fallback" to "rewarded",
            "app_open" to "app_open", "app_open_resume" to "app_open",
        ).forEach { (label, format) -> assertEquals(label, format, SpotCues.kitStandardFormat(label)) }
    }

    @Test
    fun outPhasesMapToSpotFormat() {
        assertEquals("inter", SpotCues.formatOf(SpotPhase.INTER))
        assertEquals("rewarded", SpotCues.formatOf(SpotPhase.REWARDED))
        assertEquals("app_open", SpotCues.formatOf(SpotPhase.APP_OPEN))
        assertEquals("native", SpotCues.formatOf(SpotPhase.FULL_NATIVE))
        assertEquals("custom", SpotCues.formatOf(SpotPhase.CUSTOM))
    }
}
