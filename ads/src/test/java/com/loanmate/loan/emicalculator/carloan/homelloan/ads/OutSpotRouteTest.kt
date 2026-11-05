package com.loanmate.loan.emicalculator.carloan.homelloan.ads

import com.loanmate.loan.emicalculator.carloan.homelloan.ads.config.SpotNetwork
import com.loanmate.loan.emicalculator.carloan.homelloan.ads.config.SpotPhase
import com.loanmate.loan.emicalculator.carloan.homelloan.ads.config.SpotsProfile
import com.loanmate.loan.emicalculator.carloan.homelloan.ads.config.CustomSpotSlab
import com.loanmate.loan.emicalculator.carloan.homelloan.ads.config.CustomSpots
import com.loanmate.loan.emicalculator.carloan.homelloan.ads.config.OutSpot
import com.loanmate.loan.emicalculator.carloan.homelloan.ads.config.FallbackPhase
import com.loanmate.loan.emicalculator.carloan.homelloan.ads.config.KitFormatsOn
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OutSpotRouteTest {

    /** Synchronous fake: steps in [ready] show, the rest are not ready. Records every call in order. */
    private class FakeFeed(private val ready: Set<SpotPhase>) : OutSpotFeed {
        val calls = mutableListOf<String>()
        override fun show(step: SpotPhase, onShown: () -> Unit, onNotReady: () -> Unit) {
            calls += "show:${step.key}"
            if (step in ready) onShown() else onNotReady()
        }

        override fun openLinks(urls: List<String>, onBack: () -> Unit) {
            calls += "links:${urls.joinToString(",")}"
            onBack()
        }
    }

    private fun run(config: SpotsProfile, exit: OutSpot, source: OutSpotFeed, alive: () -> Boolean = { true }): Int {
        var done = 0
        OutSpotRoute(config, exit, source, alive) { done++ }.start()
        return done
    }

    private val on = SpotsProfile(on = true)

    // ---- linksFor --------------------------------------------------------------------------------------

    private val four = listOf("u0", "u1", "u2", "u3")

    @Test fun linksFor_count2_opensOpeningTwo() {
        assertEquals(listOf("u0", "u1"), SpotsProfile(links = four, linksCount = 2).kitLinksFor(OutSpot(on = true)))
    }

    @Test fun kitLinksFor_count6_repeatsFromIndex0() {
        assertEquals(
            listOf("u0", "u1", "u2", "u3", "u0", "u1"),
            SpotsProfile(links = four, linksCount = 6).kitLinksFor(OutSpot(on = true)),
        )
    }

    @Test fun kitLinksFor_blockLinksAndCountOverride() {
        val config = SpotsProfile(links = four, linksCount = 2)
        assertEquals(listOf("b", "b", "b"), config.kitLinksFor(OutSpot(on = true, links = listOf("b"), linksCount = 3)))
        assertEquals(emptyList<String>(), config.kitLinksFor(OutSpot(on = true, linksCount = 0)))
        assertEquals(listOf("b"), config.kitLinksFor(OutSpot(on = true, links = listOf("b"))).take(1))
    }

    // ---- every / pass-through -------------------------------------------------------------------------

    @Test fun kitEvery2_runsOn2ndAnd4thPass_perPlace() {
        val counter = KitEveryCounter()
        assertEquals(listOf(false, true, false, true), List(4) { counter.pass("screens.home", 2) })
        assertTrue(counter.pass("screens.settings", 1))
        assertEquals(listOf(false, false, true), List(3) { counter.pass("onboarding.language", 3) })
    }

    @Test fun outRuns_passThroughWhenSpotsOffOrPlaceMissingOrOff_withoutCounting() {
        val counter = KitEveryCounter()
        val exit = OutSpot(on = true, ads = listOf(SpotPhase.INTER), every = 2)
        assertFalse(PlaceGuards.outRuns(spotsOn = false, exit = exit, every = counter, placeKey = "p"))
        assertFalse(PlaceGuards.outRuns(spotsOn = true, exit = null, every = counter, placeKey = "p"))
        assertFalse(PlaceGuards.outRuns(spotsOn = true, exit = exit.copy(on = false), every = counter, placeKey = "p"))
        // KitNone of the above counted: the 2nd real pass is the one that runs.
        assertFalse(PlaceGuards.outRuns(true, exit, counter, "p"))
        assertTrue(PlaceGuards.outRuns(true, exit, counter, "p"))
    }

    @Test fun spotsOff_orBlockOff_doneOnce_noCalls() {
        val source = FakeFeed(SpotPhase.entries.toSet())
        assertEquals(1, run(SpotsProfile(on = false), OutSpot(on = true, ads = listOf(SpotPhase.INTER)), source))
        assertEquals(1, run(on, OutSpot(on = false, ads = listOf(SpotPhase.INTER)), source))
        assertEquals(emptyList<String>(), source.calls)
    }

    // ---- links, ads, fallback -------------------------------------------------------------------------

    @Test fun linksEnterTogetherBeforeSpots() {
        val source = FakeFeed(setOf(SpotPhase.INTER))
        val config = on.copy(links = four, linksCount = 3)
        assertEquals(1, run(config, OutSpot(on = true, ads = listOf(SpotPhase.INTER)), source))
        assertEquals(listOf("links:u0,u1,u2", "show:inter"), source.calls)
    }

    @Test fun linkPhase_opensLinksAtItsPosition_once() {
        val source = FakeFeed(setOf(SpotPhase.INTER, SpotPhase.REWARDED))
        val config = on.copy(links = four, linksCount = 2)
        val exit = OutSpot(on = true, ads = listOf(SpotPhase.INTER, SpotPhase.LINK, SpotPhase.REWARDED))
        assertEquals(1, run(config, exit, source))
        assertEquals(listOf("show:inter", "links:u0,u1", "show:rewarded"), source.calls)
    }

    @Test fun linkPhase_afterShownSpot_stillOpensWhenPresentAllFalse() {
        val source = FakeFeed(setOf(SpotPhase.REWARDED, SpotPhase.INTER))
        val exit = OutSpot(on = true, ads = listOf(SpotPhase.REWARDED, SpotPhase.INTER, SpotPhase.LINK), showAll = false)
        assertEquals(1, run(on.copy(links = four, linksCount = 1), exit, source))
        assertEquals(listOf("show:rewarded", "links:u0"), source.calls)
    }

    @Test fun linkPhase_withCount0_isSkipped() {
        val source = FakeFeed(setOf(SpotPhase.INTER))
        val exit = OutSpot(on = true, ads = listOf(SpotPhase.LINK, SpotPhase.INTER), linksCount = 0)
        assertEquals(1, run(on.copy(links = four, linksCount = 3), exit, source))
        assertEquals(listOf("show:inter"), source.calls)
    }

    @Test fun fallback_runsInOrder_openingShownEndsIt() {
        val source = FakeFeed(setOf(SpotPhase.APP_OPEN))
        val config = on.copy(
            ifAdNotReady = listOf(FallbackPhase.Spot(SpotPhase.REWARDED), FallbackPhase.Spot(SpotPhase.APP_OPEN), FallbackPhase.OpeningLink),
        )
        assertEquals(1, run(config, OutSpot(on = true, ads = listOf(SpotPhase.INTER)), source))
        assertEquals(listOf("show:inter", "show:rewarded", "show:app_open"), source.calls)
    }

    @Test fun fallback_openingLink_usesBlockLinksElseSpotsLinks() {
        val config = on.copy(links = four, ifAdNotReady = listOf(FallbackPhase.Spot(SpotPhase.REWARDED), FallbackPhase.OpeningLink))
        val a = FakeFeed(emptySet())
        run(config, OutSpot(on = true, ads = listOf(SpotPhase.INTER), linksCount = 0, links = listOf("own")), a)
        assertEquals(listOf("show:inter", "show:rewarded", "links:own"), a.calls)

        val b = FakeFeed(emptySet())
        run(config, OutSpot(on = true, ads = listOf(SpotPhase.INTER)), b)
        assertEquals(listOf("show:inter", "show:rewarded", "links:u0"), b.calls)
    }

    @Test fun fallback_url_alwaysShows_soLaterUnitsNeverRun() {
        val source = FakeFeed(emptySet())
        val config = on.copy(ifAdNotReady = listOf(FallbackPhase.KitUrl("https://x.example"), FallbackPhase.Spot(SpotPhase.REWARDED)))
        run(config, OutSpot(on = true, ads = listOf(SpotPhase.INTER)), source)
        assertEquals(listOf("show:inter", "links:https://x.example"), source.calls)
    }

    @Test fun presentAllTrue_everySpotShows_falseStopsAtOpening() {
        val ads = listOf(SpotPhase.REWARDED, SpotPhase.INTER)
        val all = FakeFeed(setOf(SpotPhase.REWARDED, SpotPhase.INTER))
        run(on.copy(showAll = true), OutSpot(on = true, ads = ads), all)
        assertEquals(listOf("show:rewarded", "show:inter"), all.calls)

        val first = FakeFeed(setOf(SpotPhase.REWARDED, SpotPhase.INTER))
        run(on.copy(showAll = true), OutSpot(on = true, ads = ads, showAll = false), first)
        assertEquals(listOf("show:rewarded"), first.calls)
    }

    @Test fun presentAllFalse_notReadySpotWithNoFallback_movesToOnwardSpot() {
        val source = FakeFeed(setOf(SpotPhase.INTER))
        run(on.copy(showAll = false), OutSpot(on = true, ads = listOf(SpotPhase.REWARDED, SpotPhase.INTER)), source)
        assertEquals(listOf("show:rewarded", "show:inter"), source.calls)
    }

    @Test fun kitFormatOff_countsAsNotReady_withoutAskingTheEngine() {
        val source = FakeFeed(SpotPhase.entries.toSet())
        val config = on.copy(formatsOn = KitFormatsOn(inter = false), ifAdNotReady = listOf(FallbackPhase.Spot(SpotPhase.REWARDED)))
        run(config, OutSpot(on = true, ads = listOf(SpotPhase.INTER)), source)
        assertEquals(listOf("show:rewarded"), source.calls)
    }

    @Test fun networkCustom_onlyCustomPhaseShows() {
        val source = FakeFeed(SpotPhase.entries.toSet())
        val config = on.copy(
            network = SpotNetwork.CUSTOM,
            customAds = CustomSpots(on = true, list = listOf(CustomSpotSlab(title = "t"))),
        )
        run(config, OutSpot(on = true, ads = listOf(SpotPhase.INTER, SpotPhase.CUSTOM)), source)
        assertEquals(listOf("show:custom"), source.calls)
    }

    @Test fun doneExactlyOnce_whenFeedCallsBothCallbacks() {
        val twice = object : OutSpotFeed {
            override fun show(step: SpotPhase, onShown: () -> Unit, onNotReady: () -> Unit) {
                onShown(); onNotReady(); onShown()
            }

            override fun openLinks(urls: List<String>, onBack: () -> Unit) = onBack()
        }
        assertEquals(1, run(on, OutSpot(on = true, ads = listOf(SpotPhase.INTER, SpotPhase.REWARDED)), twice))
    }

    @Test fun activityGone_endsRouteOnce() {
        val source = FakeFeed(setOf(SpotPhase.INTER, SpotPhase.REWARDED))
        var alive = true
        val counting = object : OutSpotFeed by source {
            override fun show(step: SpotPhase, onShown: () -> Unit, onNotReady: () -> Unit) {
                source.show(step, { alive = false; onShown() }, onNotReady)
            }
        }
        assertEquals(1, run(on, OutSpot(on = true, ads = listOf(SpotPhase.INTER, SpotPhase.REWARDED)), counting) { alive })
        assertEquals(listOf("show:inter"), source.calls)
    }

    // ---- place counters -------------------------------------------------------------------------------

    @Test fun kitGapPassed() {
        assertTrue(PlaceGuards.kitGapPassed(lastRunMs = 0, gapSec = 120, nowMs = 1_000))
        assertTrue(PlaceGuards.kitGapPassed(lastRunMs = 5_000, gapSec = 0, nowMs = 5_001))
        assertFalse(PlaceGuards.kitGapPassed(lastRunMs = 10_000, gapSec = 120, nowMs = 129_999))
        assertTrue(PlaceGuards.kitGapPassed(lastRunMs = 10_000, gapSec = 120, nowMs = 130_000))
    }

    @Test fun openLimits() {
        val hour = 3_600_000L
        val install = 0L
        fun allowed(now: Long, last: Long = 0, today: Int = 0) =
            PlaceGuards.openAllowed(now, install, startAfterHours = 24, gapMinutes = 30, lastRunMs = last, kitTodayCount = today, maxPerDay = 5)
        assertFalse(allowed(now = 23 * hour)) // before start_after_hours
        assertTrue(allowed(now = 24 * hour))
        assertFalse(allowed(now = 25 * hour, last = 25 * hour - 29 * 60_000L)) // inside gap_minutes
        assertTrue(allowed(now = 25 * hour, last = 25 * hour - 30 * 60_000L))
        assertFalse(allowed(now = 25 * hour, today = 5)) // max_per_day reached
        assertFalse(PlaceGuards.openAllowed(25 * hour, install, 24, 30, 0, 0, maxPerDay = 0))
    }

    @Test fun kitCountToday_resetsOnNewDay() {
        assertEquals(3, PlaceGuards.kitCountToday("20261027", 3, "20261027"))
        assertEquals(0, PlaceGuards.kitCountToday("20261026", 3, "20261027"))
        assertEquals(0, PlaceGuards.kitCountToday(null, 3, "20261027"))
    }

    @Test fun playKeeperWindow() {
        assertTrue(PlaceGuards.kitWithinWindow(nowMs = 6_000_000, installMs = 0, windowSec = 6000))
        assertFalse(PlaceGuards.kitWithinWindow(nowMs = 6_000_001, installMs = 0, windowSec = 6000))
    }
}
