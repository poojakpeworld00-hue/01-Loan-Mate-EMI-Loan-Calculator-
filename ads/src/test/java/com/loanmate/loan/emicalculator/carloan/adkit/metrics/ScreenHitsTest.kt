package com.loanmate.loan.emicalculator.carloan.adkit.metrics

import org.junit.Assert.assertEquals
import org.junit.Test

class ScreenHitsTest {
    private var now = 1_000_000L
    private val sent = mutableListOf<Map<String, Any?>>()
    private val visits = ScreenHits(clock = { now }, uptime = { now }) { sent += it.toMap() }

    @Test fun oneCuePerCall_withEnterOutAndDuration() {
        visits.show("splash")
        now += 4_200
        visits.outBy("next")
        visits.show("language")
        assertEquals(1, sent.size)
        assertEquals(mapOf("screen" to "splash", "enter_time" to 1_000_000L, "exit_time" to 1_004_200L, "duration_sec" to 4L, "exit_by" to "next"), sent[0])
    }

    @Test fun sameViewAgain_isNotANewCall_baseOutIsTrigger() {
        visits.show("home"); visits.show("home")
        now += 1_000
        visits.show("setting")
        assertEquals(1, sent.size)
        assertEquals("button", sent[0]["exit_by"])
    }

    @Test fun leavingTheTool_endsCall_returnStartsNewOne() {
        visits.show("home")
        now += 2_000
        visits.outBy("home")
        visits.leaveLoan()
        assertEquals("home", sent.last()["exit_by"])
        now += 60_000
        visits.returnToLoan()
        now += 5_000
        visits.leaveLoan()
        assertEquals(listOf("home", "background"), sent.map { it["exit_by"] })
        assertEquals(5L, sent.last()["duration_sec"])
        assertEquals("home", visits.current)
    }

    @Test fun staleQueuedReason_isIgnored() {
        visits.show("home")
        visits.outBy("back")
        now += 10_000
        visits.show("setting")
        assertEquals("button", sent[0]["exit_by"])
    }

    @Test fun viewChangeWhileAway_startsOnlyWhenPrior() {
        visits.show("app_drawer")
        visits.leaveLoan() // launched another app from the drawer
        visits.show("launcher") // drawer closes behind it
        now += 30_000 // user in the other app
        visits.returnToLoan()
        now += 2_000
        visits.show("app_drawer")
        assertEquals(listOf("app_drawer", "launcher"), sent.map { it["screen"] })
        assertEquals(2L, sent.last()["duration_sec"])
    }
}
