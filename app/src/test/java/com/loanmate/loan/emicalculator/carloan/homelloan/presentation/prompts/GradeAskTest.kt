package com.loanmate.loan.emicalculator.carloan.homelloan.presentation.prompts

import com.loanmate.loan.emicalculator.carloan.homelloan.data.remote.config.Asks
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GradeAskTest {
    private val prompts = Asks(rateAfterOpens = 3, rateRetryAfterHours = 24)
    private val hour = 3_600_000L

    @Test
    fun `asks only after enough opens`() {
        assertFalse(GradeAsk.kitDue(opens = 2, askedAtMs = 0, nowMs = 100 * hour, prompts = prompts))
        assertTrue(GradeAsk.kitDue(opens = 3, askedAtMs = 0, nowMs = 100 * hour, prompts = prompts))
    }

    @Test
    fun `waits retry hours after the last ask`() {
        assertFalse(GradeAsk.kitDue(opens = 9, askedAtMs = 90 * hour, nowMs = 100 * hour, prompts = prompts))
        assertTrue(GradeAsk.kitDue(opens = 9, askedAtMs = 76 * hour, nowMs = 100 * hour, prompts = prompts))
    }
}
