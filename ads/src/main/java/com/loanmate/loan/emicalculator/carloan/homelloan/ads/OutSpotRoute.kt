package com.loanmate.loan.emicalculator.carloan.homelloan.ads

import com.loanmate.loan.emicalculator.carloan.homelloan.ads.config.SpotNetwork
import com.loanmate.loan.emicalculator.carloan.homelloan.ads.config.SpotPhase
import com.loanmate.loan.emicalculator.carloan.homelloan.ads.config.SpotsProfile
import com.loanmate.loan.emicalculator.carloan.homelloan.ads.config.OutSpot
import com.loanmate.loan.emicalculator.carloan.homelloan.ads.config.FallbackPhase

/** What an exit_ad flow asks of the ad engine. Android: [OutSpotDriver]'s engine source; tests: fakes. */
interface OutSpotFeed {
    /**
     * Show [step]; an ad that is not cached is loaded behind the loader for at most `loader_wait_sec`.
     * Exactly one callback: [onShown] once the ad is closed, [onNotReady] when it could not show.
     */
    fun show(step: SpotPhase, onShown: () -> Unit, onNotReady: () -> Unit)

    /** Open [urls] together (stacked); [onBack] once the user is back in the app. */
    fun openLinks(urls: List<String>, onBack: () -> Unit)
}

/** `every` per place since app start: every=2 runs on the 2nd, 4th… pass. */
class KitEveryCounter {
    private val passes = HashMap<String, Int>()

    fun pass(placeKey: String, every: Int): Boolean {
        val n = (passes[placeKey] ?: 0) + 1
        passes[placeKey] = n
        return every <= 1 || n % every == 0
    }
}

/**
 * One run of an exit_ad (pure rules, no Android): links at the `link` step (first when the list has none), the ads in list order, each with the
 * `if_ad_not_ready` fallback, stopping at the first shown ad unless `show_all`. [onDone] runs exactly once.
 */
class OutSpotRoute(
    private val config: SpotsProfile,
    private val exit: OutSpot,
    private val source: OutSpotFeed,
    private val isAlive: () -> Boolean = { true },
    private val onDone: () -> Unit,
) {
    var finished = false
        private set

    fun start() {
        if (!config.on || !exit.on) return finish()
        if (SpotPhase.LINK in exit.ads) runSpot(0) else openLinks { runSpot(0) }
    }

    /** Ends the flow now (e.g. its activity finished); later callbacks are ignored. */
    fun finish() {
        if (finished) return
        finished = true
        onDone()
    }

    private fun stopped(): Boolean {
        if (finished) return true
        if (!isAlive()) {
            finish()
            return true
        }
        return false
    }

    private fun openLinks(then: () -> Unit) {
        val links = config.kitLinksFor(exit)
        if (links.isEmpty()) then() else source.openLinks(links) { if (!stopped()) then() }
    }

    private fun runSpot(i: Int) {
        if (stopped()) return
        if (i >= exit.ads.size) return finish()
        if (exit.ads[i] == SpotPhase.LINK) return openLinks { runSpot(i + 1) }
        // show_all false stops the ads, but links always open: a later `link` step still runs.
        val shown = { if (config.presentAllFor(exit)) runSpot(i + 1) else if (SpotPhase.LINK in exit.ads.drop(i + 1)) openLinks(::finish) else finish() }
        trySpot(exit.ads[i], shown) { fallback(0, shown) { runSpot(i + 1) } }
    }

    /** if_ad_not_ready: the first item that shows ends the fallback; none shows → [none]. */
    private fun fallback(j: Int, shown: () -> Unit, none: () -> Unit) {
        if (stopped()) return
        val items = config.ifAdNotReady
        if (j >= items.size) return none()
        val next = { fallback(j + 1, shown, none) }
        when (val item = items[j]) {
            is FallbackPhase.Spot -> trySpot(item.step, shown, next)
            FallbackPhase.OpeningLink -> {
                val url = (exit.links?.takeIf { it.isNotEmpty() } ?: config.links).firstOrNull()
                if (url == null) next() else source.openLinks(listOf(url)) { if (!stopped()) shown() }
            }
            is FallbackPhase.KitUrl -> source.openLinks(listOf(item.url)) { if (!stopped()) shown() }
        }
    }

    private fun trySpot(step: SpotPhase, onShown: () -> Unit, onNotReady: () -> Unit) {
        if (!kitFormatOn(config, step)) return onNotReady()
        var settled = false
        source.show(
            step,
            { if (!settled) { settled = true; onShown() } },
            { if (!settled) { settled = true; onNotReady() } },
        )
    }

    companion object {
        /** A format switched off in `formats_on` (or any Google format with network=custom) counts as not ready. */
        fun kitFormatOn(config: SpotsProfile, step: SpotPhase): Boolean {
            if (step == SpotPhase.CUSTOM) return config.customAds.on && config.customAds.list.isNotEmpty()
            if (config.network == SpotNetwork.CUSTOM) return false
            return when (step) {
                SpotPhase.INTER -> config.formatsOn.inter
                SpotPhase.REWARDED -> config.formatsOn.rewarded
                SpotPhase.APP_OPEN -> config.formatsOn.appOpen
                SpotPhase.FULL_NATIVE -> config.formatsOn.native
                SpotPhase.CUSTOM, SpotPhase.LINK -> true
            }
        }
    }
}

/** Pure place rules: gap_sec, phone_unlock limits, play_store_return window. Times in ms. */
object PlaceGuards {
    /**
     * Steps (1)–(2) of every exit_ad: ads off, place missing or block off → pass through (not counted);
     * otherwise the place's `every` counter decides.
     */
    fun outRuns(spotsOn: Boolean, exit: OutSpot?, every: KitEveryCounter, placeKey: String): Boolean =
        spotsOn && exit != null && exit.on && every.pass(placeKey, exit.every)

    /** `gap_sec`: true when [gapSec] ≤ 0, it never ran, or at least [gapSec] seconds passed. */
    fun kitGapPassed(lastRunMs: Long, gapSec: Long, nowMs: Long): Boolean =
        gapSec <= 0L || lastRunMs <= 0L || nowMs - lastRunMs >= gapSec * 1000L

    /** Runs of today: the stored count when it was stored today, else 0. */
    fun kitCountToday(storedDay: String?, storedCount: Int, today: String): Int =
        if (storedDay == today) storedCount else 0

    /** phone_unlock: `start_after_hours` after install, `gap_minutes` between two, at most `max_per_day`. */
    fun openAllowed(
        nowMs: Long,
        installMs: Long,
        startAfterHours: Long,
        gapMinutes: Long,
        lastRunMs: Long,
        kitTodayCount: Int,
        maxPerDay: Int,
    ): Boolean {
        if (nowMs - installMs < startAfterHours * 3_600_000L) return false
        if (!kitGapPassed(lastRunMs, gapMinutes.coerceAtLeast(0L) * 60L, nowMs)) return false
        return maxPerDay > 0 && kitTodayCount < maxPerDay
    }

    /** play_store_return: still inside `window_sec` after install. */
    fun kitWithinWindow(nowMs: Long, installMs: Long, windowSec: Long): Boolean =
        nowMs - installMs <= windowSec.coerceAtLeast(0L) * 1000L
}
