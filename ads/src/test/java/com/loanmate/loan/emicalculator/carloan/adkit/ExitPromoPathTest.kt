package com.loanmate.loan.emicalculator.carloan.adkit

import com.loanmate.loan.emicalculator.carloan.adkit.setup.PromoNet
import com.loanmate.loan.emicalculator.carloan.adkit.setup.PromoStep
import com.loanmate.loan.emicalculator.carloan.adkit.setup.PromosSpec
import com.loanmate.loan.emicalculator.carloan.adkit.setup.TailoredPromoTile
import com.loanmate.loan.emicalculator.carloan.adkit.setup.TailoredPromos
import com.loanmate.loan.emicalculator.carloan.adkit.setup.ExitPromo
import com.loanmate.loan.emicalculator.carloan.adkit.setup.BackupStep
import com.loanmate.loan.emicalculator.carloan.adkit.setup.PackShapesOn
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExitPromoPathTest {

    /** Synchronous fake: steps in [ready] show, the rest are not ready. Records every call in order. */
    private class StubSource(private val ready: Set<PromoStep>) : ExitPromoSource {
        val calls = mutableListOf<String>()
        override fun show(step: PromoStep, onShown: () -> Unit, onNotReady: () -> Unit) {
            calls += "show:${step.key}"
            if (step in ready) onShown() else onNotReady()
        }

        override fun openLinks(urls: List<String>, onBack: () -> Unit) {
            calls += "links:${urls.joinToString(",")}"
            onBack()
        }
    }

    private fun run(config: PromosSpec, exit: ExitPromo, source: ExitPromoSource, alive: () -> Boolean = { true }): Int {
        var done = 0
        ExitPromoPath(config, exit, source, alive) { done++ }.start()
        return done
    }

    private val on = PromosSpec(on = true)

    // ---- linksFor --------------------------------------------------------------------------------------

    private val four = listOf("u0", "u1", "u2", "u3")

    @Test fun linksFor_count2_opensOpeningTwo() {
        assertEquals(listOf("u0", "u1"), PromosSpec(links = four, linksCount = 2).kitUrlsFor(ExitPromo(on = true)))
    }

    @Test fun kitLinksFor_count6_repeatsFromIndex0() {
        assertEquals(
            listOf("u0", "u1", "u2", "u3", "u0", "u1"),
            PromosSpec(links = four, linksCount = 6).kitUrlsFor(ExitPromo(on = true)),
        )
    }

    @Test fun kitLinksFor_blockLinksAndCountOverride() {
        val config = PromosSpec(links = four, linksCount = 2)
        assertEquals(listOf("b", "b", "b"), config.kitUrlsFor(ExitPromo(on = true, links = listOf("b"), linksCount = 3)))
        assertEquals(emptyList<String>(), config.kitUrlsFor(ExitPromo(on = true, linksCount = 0)))
        assertEquals(listOf("b"), config.kitUrlsFor(ExitPromo(on = true, links = listOf("b"))).take(1))
    }

    // ---- every / pass-through -------------------------------------------------------------------------

    @Test fun kitEvery2_runsOn2ndAnd4thPass_perPlace() {
        val counter = PackEachTally()
        assertEquals(listOf(false, true, false, true), List(4) { counter.pass("screens.home", 2) })
        assertTrue(counter.pass("screens.settings", 1))
        assertEquals(listOf(false, false, true), List(3) { counter.pass("onboarding.language", 3) })
    }

    @Test fun outRuns_passThroughWhenSpotsOffOrPlaceMissingOrOff_withoutCounting() {
        val counter = PackEachTally()
        val exit = ExitPromo(on = true, ads = listOf(PromoStep.INTER), every = 2)
        assertFalse(SlotChecks.outRuns(spotsOn = false, exit = exit, every = counter, placeKey = "p"))
        assertFalse(SlotChecks.outRuns(spotsOn = true, exit = null, every = counter, placeKey = "p"))
        assertFalse(SlotChecks.outRuns(spotsOn = true, exit = exit.copy(on = false), every = counter, placeKey = "p"))
        // KitNone of the above counted: the 2nd real pass is the one that runs.
        assertFalse(SlotChecks.outRuns(true, exit, counter, "p"))
        assertTrue(SlotChecks.outRuns(true, exit, counter, "p"))
    }

    @Test fun spotsOff_orBlockOff_doneOnce_noCalls() {
        val source = StubSource(PromoStep.entries.toSet())
        assertEquals(1, run(PromosSpec(on = false), ExitPromo(on = true, ads = listOf(PromoStep.INTER)), source))
        assertEquals(1, run(on, ExitPromo(on = false, ads = listOf(PromoStep.INTER)), source))
        assertEquals(emptyList<String>(), source.calls)
    }

    // ---- links, ads, fallback -------------------------------------------------------------------------

    @Test fun linksEnterTogetherBeforeSpots() {
        val source = StubSource(setOf(PromoStep.INTER))
        val config = on.copy(links = four, linksCount = 3)
        assertEquals(1, run(config, ExitPromo(on = true, ads = listOf(PromoStep.INTER)), source))
        assertEquals(listOf("links:u0,u1,u2", "show:inter"), source.calls)
    }

    @Test fun linkPhase_opensLinksAtItsPosition_once() {
        val source = StubSource(setOf(PromoStep.INTER, PromoStep.REWARDED))
        val config = on.copy(links = four, linksCount = 2)
        val exit = ExitPromo(on = true, ads = listOf(PromoStep.INTER, PromoStep.LINK, PromoStep.REWARDED))
        assertEquals(1, run(config, exit, source))
        assertEquals(listOf("show:inter", "links:u0,u1", "show:rewarded"), source.calls)
    }

    @Test fun linkPhase_afterShownSpot_stillOpensWhenPresentAllFalse() {
        val source = StubSource(setOf(PromoStep.REWARDED, PromoStep.INTER))
        val exit = ExitPromo(on = true, ads = listOf(PromoStep.REWARDED, PromoStep.INTER, PromoStep.LINK), showAll = false)
        assertEquals(1, run(on.copy(links = four, linksCount = 1), exit, source))
        assertEquals(listOf("show:rewarded", "links:u0"), source.calls)
    }

    @Test fun linkPhase_withCount0_isSkipped() {
        val source = StubSource(setOf(PromoStep.INTER))
        val exit = ExitPromo(on = true, ads = listOf(PromoStep.LINK, PromoStep.INTER), linksCount = 0)
        assertEquals(1, run(on.copy(links = four, linksCount = 3), exit, source))
        assertEquals(listOf("show:inter"), source.calls)
    }

    @Test fun fallback_runsInOrder_openingShownEndsIt() {
        val source = StubSource(setOf(PromoStep.APP_OPEN))
        val config = on.copy(
            ifAdNotReady = listOf(BackupStep.Spot(PromoStep.REWARDED), BackupStep.Spot(PromoStep.APP_OPEN), BackupStep.LaunchUrl),
        )
        assertEquals(1, run(config, ExitPromo(on = true, ads = listOf(PromoStep.INTER)), source))
        assertEquals(listOf("show:inter", "show:rewarded", "show:app_open"), source.calls)
    }

    @Test fun fallback_openingLink_usesBlockLinksElseSpotsLinks() {
        val config = on.copy(links = four, ifAdNotReady = listOf(BackupStep.Spot(PromoStep.REWARDED), BackupStep.LaunchUrl))
        val a = StubSource(emptySet())
        run(config, ExitPromo(on = true, ads = listOf(PromoStep.INTER), linksCount = 0, links = listOf("own")), a)
        assertEquals(listOf("show:inter", "show:rewarded", "links:own"), a.calls)

        val b = StubSource(emptySet())
        run(config, ExitPromo(on = true, ads = listOf(PromoStep.INTER)), b)
        assertEquals(listOf("show:inter", "show:rewarded", "links:u0"), b.calls)
    }

    @Test fun fallback_url_alwaysShows_soLaterUnitsNeverRun() {
        val source = StubSource(emptySet())
        val config = on.copy(ifAdNotReady = listOf(BackupStep.PackLink("https://x.example"), BackupStep.Spot(PromoStep.REWARDED)))
        run(config, ExitPromo(on = true, ads = listOf(PromoStep.INTER)), source)
        assertEquals(listOf("show:inter", "links:https://x.example"), source.calls)
    }

    @Test fun presentAllTrue_everySpotShows_falseStopsAtOpening() {
        val ads = listOf(PromoStep.REWARDED, PromoStep.INTER)
        val all = StubSource(setOf(PromoStep.REWARDED, PromoStep.INTER))
        run(on.copy(showAll = true), ExitPromo(on = true, ads = ads), all)
        assertEquals(listOf("show:rewarded", "show:inter"), all.calls)

        val first = StubSource(setOf(PromoStep.REWARDED, PromoStep.INTER))
        run(on.copy(showAll = true), ExitPromo(on = true, ads = ads, showAll = false), first)
        assertEquals(listOf("show:rewarded"), first.calls)
    }

    @Test fun presentAllFalse_notReadySpotWithNoFallback_movesToOnwardSpot() {
        val source = StubSource(setOf(PromoStep.INTER))
        run(on.copy(showAll = false), ExitPromo(on = true, ads = listOf(PromoStep.REWARDED, PromoStep.INTER)), source)
        assertEquals(listOf("show:rewarded", "show:inter"), source.calls)
    }

    @Test fun kitFormatOff_countsAsNotReady_withoutAskingTheEngine() {
        val source = StubSource(PromoStep.entries.toSet())
        val config = on.copy(formatsOn = PackShapesOn(inter = false), ifAdNotReady = listOf(BackupStep.Spot(PromoStep.REWARDED)))
        run(config, ExitPromo(on = true, ads = listOf(PromoStep.INTER)), source)
        assertEquals(listOf("show:rewarded"), source.calls)
    }

    @Test fun networkCustom_onlyCustomPhaseShows() {
        val source = StubSource(PromoStep.entries.toSet())
        val config = on.copy(
            network = PromoNet.CUSTOM,
            customAds = TailoredPromos(on = true, list = listOf(TailoredPromoTile(title = "t"))),
        )
        run(config, ExitPromo(on = true, ads = listOf(PromoStep.INTER, PromoStep.CUSTOM)), source)
        assertEquals(listOf("show:custom"), source.calls)
    }

    @Test fun doneExactlyOnce_whenFeedCallsBothCallbacks() {
        val twice = object : ExitPromoSource {
            override fun show(step: PromoStep, onShown: () -> Unit, onNotReady: () -> Unit) {
                onShown(); onNotReady(); onShown()
            }

            override fun openLinks(urls: List<String>, onBack: () -> Unit) = onBack()
        }
        assertEquals(1, run(on, ExitPromo(on = true, ads = listOf(PromoStep.INTER, PromoStep.REWARDED)), twice))
    }

    @Test fun activityGone_endsRouteOnce() {
        val source = StubSource(setOf(PromoStep.INTER, PromoStep.REWARDED))
        var alive = true
        val counting = object : ExitPromoSource by source {
            override fun show(step: PromoStep, onShown: () -> Unit, onNotReady: () -> Unit) {
                source.show(step, { alive = false; onShown() }, onNotReady)
            }
        }
        assertEquals(1, run(on, ExitPromo(on = true, ads = listOf(PromoStep.INTER, PromoStep.REWARDED)), counting) { alive })
        assertEquals(listOf("show:inter"), source.calls)
    }

    // ---- place counters -------------------------------------------------------------------------------

    @Test fun kitGapPassed() {
        assertTrue(SlotChecks.kitGapPassed(lastRunMs = 0, gapSec = 120, nowMs = 1_000))
        assertTrue(SlotChecks.kitGapPassed(lastRunMs = 5_000, gapSec = 0, nowMs = 5_001))
        assertFalse(SlotChecks.kitGapPassed(lastRunMs = 10_000, gapSec = 120, nowMs = 129_999))
        assertTrue(SlotChecks.kitGapPassed(lastRunMs = 10_000, gapSec = 120, nowMs = 130_000))
    }

    @Test fun openLimits() {
        val hour = 3_600_000L
        val install = 0L
        fun allowed(now: Long, last: Long = 0, today: Int = 0) =
            SlotChecks.launchAllowed(now, install, startAfterHours = 24, gapMinutes = 30, lastRunMs = last, kitTodayCount = today, maxPerDay = 5)
        assertFalse(allowed(now = 23 * hour)) // before start_after_hours
        assertTrue(allowed(now = 24 * hour))
        assertFalse(allowed(now = 25 * hour, last = 25 * hour - 29 * 60_000L)) // inside gap_minutes
        assertTrue(allowed(now = 25 * hour, last = 25 * hour - 30 * 60_000L))
        assertFalse(allowed(now = 25 * hour, today = 5)) // max_per_day reached
        assertFalse(SlotChecks.launchAllowed(25 * hour, install, 24, 30, 0, 0, maxPerDay = 0))
    }

    @Test fun kitCountToday_resetsOnNewDay() {
        assertEquals(3, SlotChecks.kitCountToday("20261027", 3, "20261027"))
        assertEquals(0, SlotChecks.kitCountToday("20261026", 3, "20261027"))
        assertEquals(0, SlotChecks.kitCountToday(null, 3, "20261027"))
    }

    @Test fun playKeeperWindow() {
        assertTrue(SlotChecks.kitWithinWindow(nowMs = 6_000_000, installMs = 0, windowSec = 6000))
        assertFalse(SlotChecks.kitWithinWindow(nowMs = 6_000_001, installMs = 0, windowSec = 6000))
    }
}
