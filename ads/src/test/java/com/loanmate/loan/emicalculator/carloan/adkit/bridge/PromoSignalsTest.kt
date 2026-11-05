package com.loanmate.loan.emicalculator.carloan.adkit.bridge

import com.loanmate.loan.emicalculator.carloan.adkit.setup.PromoStep
import org.junit.Assert.assertEquals
import org.junit.Test

class PromoSignalsTest {
    @Test
    fun kitErrorCode3IsNoFillOthersFailed() {
        assertEquals("no_fill", PromoSignals.failPhase(3))
        assertEquals("failed", PromoSignals.failPhase(0))
        assertEquals("failed", PromoSignals.failPhase(2))
    }

    @Test
    fun engineLabelsMapToFirebaseSpotFormat() {
        mapOf(
            "banner" to "banner",
            "native_big" to "native", "native_medium2" to "native", "native_full_unit" to "native", "native_strip" to "native",
            "inter_chain" to "interstitial", "inter_navigation" to "interstitial",
            "rewarded" to "rewarded", "rewarded_fallback" to "rewarded",
            "app_open" to "app_open", "app_open_resume" to "app_open",
        ).forEach { (label, format) -> assertEquals(label, format, PromoSignals.kitStandardFormat(label)) }
    }

    @Test
    fun outPhasesMapToSpotFormat() {
        assertEquals("inter", PromoSignals.formatOf(PromoStep.INTER))
        assertEquals("rewarded", PromoSignals.formatOf(PromoStep.REWARDED))
        assertEquals("app_open", PromoSignals.formatOf(PromoStep.APP_OPEN))
        assertEquals("native", PromoSignals.formatOf(PromoStep.FULL_NATIVE))
        assertEquals("custom", PromoSignals.formatOf(PromoStep.CUSTOM))
    }
}
