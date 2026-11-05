package com.loanmate.loan.emicalculator.carloan.homelloan.ads.analytics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StatsGuardsTest {
    @Test fun elevenFixedCues_allValidNames() {
        assertEquals(12, Stats2.EVENTS.size)
        assertTrue(Stats2.EVENTS.all(CueGuards::kitValidName))
    }

    @Test fun clean_dropsNullsAndBadHotkeys_cutsCaption_widensNumbers() {
        val out = CueGuards.clean(listOf("screen" to "home", "value" to null, "Bad-Key" to "x", "n" to 3, "f" to 1.5f, "t" to "a".repeat(150)))
        assertEquals(listOf("screen" to "home", "n" to 3L, "f" to 1.5, "t" to "a".repeat(100)), out)
        assertFalse(CueGuards.kitValidName("x".repeat(41)))
    }
}
