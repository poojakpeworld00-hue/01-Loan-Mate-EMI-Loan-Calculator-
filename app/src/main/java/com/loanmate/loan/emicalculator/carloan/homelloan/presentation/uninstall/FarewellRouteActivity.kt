package com.loanmate.loan.emicalculator.carloan.homelloan.presentation.uninstall

import android.animation.ObjectAnimator
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.os.SystemClock
import android.provider.Settings
import androidx.activity.addCallback
import com.loanmate.loan.emicalculator.carloan.homelloan.presentation.common.kitEnableDarkEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.loanmate.loan.emicalculator.carloan.homelloan.ads.Spots
import com.loanmate.loan.emicalculator.carloan.homelloan.ads.OutSpotDriver
import com.loanmate.loan.emicalculator.carloan.homelloan.ads.analytics.Stats2
import com.loanmate.loan.emicalculator.carloan.homelloan.ads.analytics.Views
import com.loanmate.loan.emicalculator.carloan.homelloan.Tool2
import com.loanmate.loan.emicalculator.carloan.homelloan.R
import com.loanmate.loan.emicalculator.carloan.homelloan.data.remote.config.Follow
import com.loanmate.loan.emicalculator.carloan.homelloan.databinding.PageBootBinding
import com.loanmate.loan.emicalculator.carloan.homelloan.databinding.PageFarewellBinding
import com.loanmate.loan.emicalculator.carloan.homelloan.databinding.CellToolBinding
import com.loanmate.loan.emicalculator.carloan.homelloan.databinding.CellFarewellReasonBinding
import com.loanmate.loan.emicalculator.carloan.homelloan.launcherhost.SelfBadge
import com.loanmate.loan.emicalculator.carloan.homelloan.presentation.common.kitGlowPulse
import com.loanmate.loan.emicalculator.carloan.homelloan.presentation.common.padForOsBars
import com.loanmate.loan.emicalculator.carloan.homelloan.presentation.home.BaseActivity
import com.loanmate.loan.emicalculator.carloan.homelloan.presentation.onboarding.PickAdapter
import com.loanmate.loan.emicalculator.carloan.homelloan.presentation.onboarding.tonguePhase
import com.loanmate.loan.emicalculator.carloan.homelloan.presentation.settings.OptionsActivity
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The reference app's premium uninstall flow (`uninstall_flow.flow` = "premium"), opened by Uninstall in the long-press
 * menu on our icon:
 *  - paid (track_b): splash → language → step1 (quick fixes) → step2 (reasons) → progress → step3 → our icon leaves the
 *    launcher home and drawer and this screen closes; the app stays installed.
 *  - organic (track_a): starts on step1 (no splash, no language) → step2 → progress → step3 → Android's Tool2 info page.
 * Each screen but the splash shows `uninstall_flow.<screen>`'s bottom ad and moving forward runs its exit ad. Keep / a quick
 * fix go back to the app.
 */
class FarewellRouteActivity : AppCompatActivity() {
    private enum class View2 { SPLASH, LANGUAGE, STEP1, STEP2, PROGRESS, STEP3 }

    /** Each reason has a quick fix; step 1 shows [TIPS] of them. */
    private enum class KitReason(val label: Int, val fix: Int, val fixButton: Int, val icon: Int) {
        BROKEN(R.string.un_r_broken, R.string.un_fix_broken, R.string.un_fix_broken_btn, R.drawable.ic_replay),
        ADS(R.string.un_r_ads, R.string.un_fix_ads, R.string.un_fix_settings_btn, R.drawable.ic_diamond),
        CONTENT(R.string.un_r_content, R.string.un_fix_content, R.string.un_fix_content_btn, R.drawable.ic_search),
        HARD(R.string.un_r_hard, R.string.un_fix_hard, R.string.un_fix_settings_btn, R.drawable.ic_settings),
        NOT_NEEDED(R.string.un_r_not_needed, R.string.un_fix_not_needed, R.string.sh_home, R.drawable.ic_home),
    }

    private lateinit var b: PageFarewellBinding
    private val container get() = (application as Tool2).container
    private var screen = View2.STEP1
    private var picked: KitReason? = null
    private var paid = false
    private var splash: PageBootBinding? = null
    private var timer: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val config = container.config.value
        if (!config.uninstallFlow.premium) return finish() // normal flow: there is no Uninstall entry that opens this
        kitEnableDarkEdgeToEdge()
        b = PageFarewellBinding.inflate(layoutInflater)
        setContentView(b.root)
        b.content.padForOsBars()
        b.glow.kitGlowPulse()
        b.header.title.setText(R.string.un_title)
        b.header.back.setOnClickListener { Stats2.click("back"); goBack() }
        paid = config.track == Follow.B
        screen = savedInstanceState?.getString(KEY_SCREEN)?.let(View2::valueOf) ?: if (paid) View2.SPLASH else View2.STEP1
        picked = savedInstanceState?.getString(KEY_REASON)?.let(KitReason::valueOf)
        if (savedInstanceState == null) Stats2.step(Stats2.UNINSTALL, "start")
        onBackPressedDispatcher.addCallback(this) { Stats2.hotkeyPress("back"); goBack() }
        show(screen)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_SCREEN, screen.name)
        picked?.let { outState.putString(KEY_REASON, it.name) }
    }

    /** The language picked here applied in place (manifest configChanges, as the reference): the page again in it. */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        b.header.title.setText(R.string.un_title)
        if (screen != View2.SPLASH && screen != View2.PROGRESS) show(screen)
    }

    override fun onResume() {
        super.onResume()
        Views.show("uninstall_flow")
    }

    /** As the reference: splash and progress move on by themselves; language keeps the current pick; step1 → reasons. */
    private fun goBack() = when (screen) {
        View2.SPLASH, View2.PROGRESS -> Unit
        View2.LANGUAGE -> pickTongue(container.settingsRepository.currentLanguageIndex())
        View2.STEP1 -> leave { show(View2.STEP2) }
        View2.STEP2 -> show(View2.STEP1)
        View2.STEP3 -> show(View2.STEP2)
    }

    private fun show(next: View2) {
        screen = next
        timer?.cancel()
        Stats2.step(Stats2.UNINSTALL, next.name.lowercase())
        val flow = container.config.value.uninstallFlow
        splash?.root?.let(b.root::removeView)
        splash = null
        // Every page shows its own parts; the rest is hidden.
        listOf(b.header.root, b.pageTitle, b.sub, b.list, b.rowsScroll, b.center, b.progress, b.percent, b.primary, b.secondary)
            .forEach { it.visibility = View.GONE }
        b.adSlot.removeAllViews()
        if (next != View2.SPLASH) Spots.bottom(this, b.adSlot, place())
        when (next) {
            View2.SPLASH -> presentBoot(flow.splashMs)
            View2.LANGUAGE -> {
                show(b.pageTitle, b.list, b.primary)
                b.pageTitle.setText(R.string.un_language_title)
                // A tap marks the row and moves on after 250 ms; Continue moves on with the current pick.
                var pick = container.settingsRepository.currentLanguageIndex()
                b.list.layoutManager = LinearLayoutManager(this)
                b.list.adapter = PickAdapter(tonguePhase(container.settingsRepository.languages), setOf(pick)) {
                    pick = it
                    (b.list.adapter as PickAdapter).kitSubmitSelection(setOf(it))
                    b.list.postDelayed({ if (screen == View2.LANGUAGE) pickTongue(it) }, 250)
                }
                b.primary.setText(R.string.next)
                b.primary.setOnClickListener { pickTongue(pick) }
            }
            View2.STEP1 -> {
                show(b.header.root, b.pageTitle, b.sub, b.rowsScroll, b.primary, b.secondary)
                b.pageTitle.setText(R.string.un_sorry_title)
                b.sub.setText(R.string.un_sorry_body)
                b.rows.removeAllViews()
                TIPS.forEach { r ->
                    CellToolBinding.inflate(layoutInflater, b.rows, true).apply {
                        icon.setImageResource(r.icon)
                        name.setText(r.label)
                        sub.setText(r.fix)
                        root.setOnClickListener { fix(r) }
                    }
                }
                b.primary.setText(R.string.un_keep)
                b.primary.setOnClickListener { keepTool() }
                b.secondary.setText(R.string.un_still)
                b.secondary.setOnClickListener { Stats2.click("still_uninstall"); leave { show(View2.STEP2) } }
            }
            View2.STEP2 -> {
                show(b.header.root, b.pageTitle, b.rowsScroll, b.secondary)
                b.pageTitle.setText(R.string.un_reason_title)
                wireReasons()
                b.secondary.setText(R.string.un_keep)
                b.secondary.setOnClickListener { keepTool() }
            }
            View2.PROGRESS -> {
                show(b.center, b.progress, b.percent)
                b.centerTitle.text = getString(R.string.un_progress, getString(R.string.app_name))
                b.centerSub.setText(R.string.un_progress_sub)
                runHeadway(flow.progressMs.coerceIn(500L, 20_000L))
            }
            View2.STEP3 -> {
                show(b.header.root, b.center, b.primary, b.secondary)
                b.centerTitle.setText(R.string.un_confirm_title)
                b.centerSub.setText(R.string.un_confirm_body)
                b.primary.setText(R.string.un_keep)
                b.primary.setOnClickListener { keepTool() }
                b.secondary.setText(R.string.un_uninstall)
                b.secondary.setOnClickListener { Stats2.click("uninstall"); leave { end() } }
                // As the reference: no tap within 3 s ends the flow.
                timer = lifecycleScope.launch { delay(3_000); leave { end() } }
            }
        }
    }

    private fun show(vararg views: View) = views.forEach { it.visibility = View.VISIBLE }

    /** The app's own splash (art, logo, name, bar) for `splash_ms`, as the reference shows its splash screen here. */
    private fun presentBoot(durationMs: Long) {
        val s = PageBootBinding.inflate(layoutInflater, b.root, false)
        b.root.addView(s.root)
        s.content.padForOsBars()
        splash = s
        ObjectAnimator.ofInt(s.progress, "progress", 0, 100).setDuration(durationMs).start()
        timer = lifecycleScope.launch { delay(durationMs); kitLeaveWhenFree { show(View2.LANGUAGE) } }
    }

    /** Phase 2: a picked reason opens its fix, the fix button and "Uninstall anyway" (→ progress). */
    private fun wireReasons() {
        b.rows.removeAllViews()
        KitReason.entries.forEach { r ->
            val selected = picked == r
            CellFarewellReasonBinding.inflate(layoutInflater, b.rows, true).apply {
                card.strokeColor = getColor(if (selected) R.color.sh_primary else R.color.sh_border)
                radio.setImageResource(if (selected) R.drawable.ic_radio_checked else R.drawable.ic_radio_unchecked)
                if (selected) radio.setColorFilter(getColor(R.color.sh_primary))
                label.setText(r.label)
                row.setOnClickListener {
                    Stats2.click("reason", r.name.lowercase())
                    Stats2.step(Stats2.UNINSTALL, "reason", r.name.lowercase())
                    picked = r
                    wireReasons()
                }
                fix.visibility = if (selected) View.VISIBLE else View.GONE
                fixText.setText(r.fix)
                fixButton.setText(r.fixButton)
                fixButton.setOnClickListener { fix(r) }
                anyway.setOnClickListener { Stats2.click("uninstall_anyway"); leave { show(View2.PROGRESS) } }
            }
        }
    }

    /** 0 → 100 % over [durationMs], "Ready to uninstall" for 1.2 s, then step 3. */
    private fun runHeadway(durationMs: Long) {
        b.progress.progress = 0
        b.percent.setTextColor(getColor(R.color.sh_muted))
        val start = SystemClock.elapsedRealtime()
        timer = lifecycleScope.launch {
            while (true) {
                val p = ((SystemClock.elapsedRealtime() - start) * 100 / durationMs).toInt().coerceAtMost(100)
                b.progress.progress = p
                b.percent.text = getString(R.string.un_progress_percent, p)
                if (p >= 100) break
                delay(40)
            }
            b.percent.setText(R.string.un_progress_done)
            b.percent.setTextColor(getColor(R.color.sh_primary))
            delay(1_200)
            kitLeaveWhenFree { show(View2.STEP3) }
        }
    }

    private fun place() = "uninstall_flow.${screen.name.lowercase()}"

    private fun pickTongue(index: Int) {
        Stats2.click("language", container.settingsRepository.languages[index].tag)
        leave {
            show(View2.STEP1)
            container.settingsRepository.setLanguage(index) // in place: onConfigurationChanged redraws step 1
        }
    }

    /** Runs the screen's exit ad, then [action]: once, even on double taps. */
    private fun leave(action: () -> Unit) {
        if (OutSpotDriver.busy()) return
        val from = screen
        Spots.exit(this, place()) { if (screen == from && !isFinishing) action() }
    }

    /** Views that move on by themselves wait out an ad still on screen instead of dropping the step. */
    private fun kitLeaveWhenFree(action: () -> Unit) {
        lifecycleScope.launch {
            while (OutSpotDriver.busy()) delay(300)
            leave(action)
        }
    }

    private fun keepTool() {
        Stats2.click("keep")
        Stats2.step(Stats2.UNINSTALL, "keep")
        startActivity(Intent(this, BaseActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        finish()
    }

    /** A quick fix: back into the app at the place that fixes it. */
    private fun fix(r: KitReason) {
        Stats2.click("fix", r.name.lowercase())
        Stats2.step(Stats2.UNINSTALL, "fix", r.name.lowercase())
        val home = Intent(this, BaseActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        when (r) {
            KitReason.BROKEN -> startActivity(home.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK))
            KitReason.ADS, KitReason.HARD -> startActivities(arrayOf(home, Intent(this, OptionsActivity::class.java)))
            KitReason.CONTENT, KitReason.NOT_NEEDED -> startActivity(home)
        }
        finish()
    }

    /** As the reference: paid hides our icon (the app stays installed, the user is back on the launcher); organic opens Tool2 info. */
    private fun end() {
        timer?.cancel()
        endFarewell(this, paid)
        finish()
    }

    private companion object {
        const val KEY_SCREEN = "screen"
        const val KEY_REASON = "reason"
        val TIPS = listOf(KitReason.BROKEN, KitReason.CONTENT, KitReason.HARD)
    }
}

/** The end of the premium uninstall (flow or dialog), as the reference: paid hides our icon; organic opens Tool2 info. */
internal fun endFarewell(activity: android.app.Activity, paid: Boolean) {
    if (paid) {
        Stats2.step(Stats2.UNINSTALL, "hidden")
        SelfBadge.hide(activity)
    } else {
        Stats2.step(Stats2.UNINSTALL, "app_info")
        runCatching {
            activity.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", activity.packageName, null)))
        }
    }
}
