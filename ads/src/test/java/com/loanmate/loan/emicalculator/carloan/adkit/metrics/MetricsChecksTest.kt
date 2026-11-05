package com.loanmate.loan.emicalculator.carloan.adkit.metrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MetricsChecksTest {
    @Test fun elevenFixedCues_allValidNames() {
        assertEquals(12, MetricsTwo.EVENTS.size)
        assertTrue(MetricsTwo.EVENTS.all(SignalChecks::kitValidName))
    }

    @Test fun clean_dropsNullsAndBadHotkeys_cutsCaption_widensNumbers() {
        val out = SignalChecks.clean(listOf("screen" to "home", "value" to null, "Bad-Key" to "x", "n" to 3, "f" to 1.5f, "t" to "a".repeat(150)))
        assertEquals(listOf("screen" to "home", "n" to 3L, "f" to 1.5, "t" to "a".repeat(100)), out)
        assertFalse(SignalChecks.kitValidName("x".repeat(41)))
    }
}
