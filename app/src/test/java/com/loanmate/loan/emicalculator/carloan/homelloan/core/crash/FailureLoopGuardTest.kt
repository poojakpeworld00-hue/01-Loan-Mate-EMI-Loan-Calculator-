package com.loanmate.loan.emicalculator.carloan.homelloan.core.crash

import com.loanmate.loan.emicalculator.carloan.homelloan.core.crash.FailureLoopGuard.Task2.GIVE_UP
import com.loanmate.loan.emicalculator.carloan.homelloan.core.crash.FailureLoopGuard.Task2.RESTART
import com.loanmate.loan.emicalculator.carloan.homelloan.core.crash.FailureLoopGuard.Task2.SAFE_SCREEN
import org.junit.Assert.assertEquals
import org.junit.Test

class FailureLoopGuardTest {
    private val guard = FailureLoopGuard(windowMs = 60_000, safeAt = 3, giveUpAt = 5)

    @Test
    fun `restart, then safe screen, then give up within the window`() {
        var history = emptyList<Long>()
        val actions = (1..5).map { i -> guard.onFailure2(history, i * 1_000L).also { history = it.second }.first }
        assertEquals(listOf(RESTART, RESTART, SAFE_SCREEN, SAFE_SCREEN, GIVE_UP), actions)
    }

    @Test
    fun `crashes older than the window are forgotten`() {
        assertEquals(SAFE_SCREEN, guard.onFailure2(listOf(0L, 1_000L), 60_000).first)
        assertEquals(RESTART to listOf(130_000L), guard.onFailure2(listOf(0L, 1_000L, 60_000L), 130_000))
    }
}
