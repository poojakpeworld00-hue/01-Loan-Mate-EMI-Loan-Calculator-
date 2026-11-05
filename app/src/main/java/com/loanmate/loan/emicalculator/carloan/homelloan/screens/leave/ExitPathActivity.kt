package com.loanmate.loan.emicalculator.carloan.homelloan.screens.leave

import android.animation.ObjectAnimator
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.os.SystemClock
import android.provider.Settings
import androidx.activity.addCallback
import com.loanmate.loan.emicalculator.carloan.homelloan.screens.shared.kitEnableDarkEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.loanmate.loan.emicalculator.carloan.adkit.Spots
import com.loanmate.loan.emicalculator.carloan.adkit.ExitPromoRunner
import com.loanmate.loan.emicalculator.carloan.adkit.metrics.MetricsTwo
import com.loanmate.loan.emicalculator.carloan.adkit.metrics.Views
import com.loanmate.loan.emicalculator.carloan.homelloan.LoanTwo
import com.loanmate.loan.emicalculator.carloan.homelloan.R
import com.loanmate.loan.emicalculator.carloan.homelloan.store.cloud.flags.Follow
import com.loanmate.loan.emicalculator.carloan.homelloan.databinding.ScreenBootBinding
import com.loanmate.loan.emicalculator.carloan.homelloan.databinding.ScreenFarewellBinding
import com.loanmate.loan.emicalculator.carloan.homelloan.databinding.RowToolBinding
import com.loanmate.loan.emicalculator.carloan.homelloan.databinding.RowFarewellReasonBinding
import com.loanmate.loan.emicalculator.carloan.homelloan.homebridge.OwnMark
import com.loanmate.loan.emicalculator.carloan.homelloan.screens.shared.kitGlowPulse
import com.loanmate.loan.emicalculator.carloan.homelloan.screens.shared.padForOsBars
import com.loanmate.loan.emicalculator.carloan.homelloan.screens.dashboard.RootActivity
import com.loanmate.loan.emicalculator.carloan.homelloan.screens.welcome.ChooseAdapter
import com.loanmate.loan.emicalculator.carloan.homelloan.screens.welcome.tonguePhase
import com.loanmate.loan.emicalculator.carloan.homelloan.screens.prefs.SettingsActivity
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The reference app's premium uninstall flow (`uninstall_flow.flow` = "premium"), opened by Uninstall in the long-press
 * menu on our icon:
 *  - paid (track_b): splash → language → step1 (quick fixes) → step2 (reasons) → progress → step3 → our icon leaves the
 *    launcher home and drawer and this screen closes; the app stays installed.
 *  - organic (track_a): starts on step1 (no splash, no language) → step2 → progress → step3 → Android's LoanTwo info page.
 * Each screen but the splash shows `uninstall_flow.<screen>`'s bottom ad and moving forward runs its exit ad. Keep / a quick
 * fix go back to the app.
 */
class ExitPathActivity : AppCompatActivity() {
    private enum class ScreenTwo { SPLASH, LANGUAGE, STEP1, STEP2, PROGRESS, STEP3 }

    /** Each reason has a quick fix; step 1 shows [TIPS] of them. */
    private enum class PackCause(val label: Int, val fix: Int, val fixButton: Int, val icon: Int) {
        BROKEN(R.string.un_r_broken, R.string.un_fix_broken, R.string.un_fix_broken_btn, R.drawable.glyph_replay),
        ADS(R.string.un_r_ads, R.string.un_fix_ads, R.string.un_fix_settings_btn, R.drawable.glyph_diamond),
        CONTENT(R.string.un_r_content, R.string.un_fix_content, R.string.un_fix_content_btn, R.drawable.glyph_search),
        HARD(R.string.un_r_hard, R.string.un_fix_hard, R.string.un_fix_settings_btn, R.drawable.glyph_settings),
        NOT_NEEDED(R.string.un_r_not_needed, R.string.un_fix_not_needed, R.string.sh_home, R.drawable.glyph_home),
    }

    private lateinit var b: ScreenFarewellBinding
    private val container get() = (application as LoanTwo).container
    private var screen = ScreenTwo.STEP1
    private var picked: PackCause? = null
    private var paid = false
    private var splash: ScreenBootBinding? = null
    private var timer: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val config = container.config.value
        if (!config.uninstallFlow.premium) return finish() // normal flow: there is no Uninstall entry that opens this
        kitEnableDarkEdgeToEdge()
        b = ScreenFarewellBinding.inflate(layoutInflater)
        setContentView(b.root)
        b.content.padForOsBars()
        b.glow.kitGlowPulse()
        b.header.title.setText(R.string.un_title)
        b.header.back.setOnClickListener { MetricsTwo.click("back"); goBack() }
        paid = config.track == Follow.B
        screen = savedInstanceState?.getString(KEY_SCREEN)?.let(ScreenTwo::valueOf) ?: if (paid) ScreenTwo.SPLASH else ScreenTwo.STEP1
        picked = savedInstanceState?.getString(KEY_REASON)?.let(PackCause::valueOf)
        if (savedInstanceState == null) MetricsTwo.step(MetricsTwo.UNINSTALL, "start")
        onBackPressedDispatcher.addCallback(this) { MetricsTwo.hotkeyPress("back"); goBack() }
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
        if (screen != ScreenTwo.SPLASH && screen != ScreenTwo.PROGRESS) show(screen)
    }

    override fun onResume() {
        super.onResume()
        Views.show("uninstall_flow")
    }

    /** As the reference: splash and progress move on by themselves; language keeps the current pick; step1 → reasons. */
    private fun goBack() = when (screen) {
        ScreenTwo.SPLASH, ScreenTwo.PROGRESS -> Unit
        ScreenTwo.LANGUAGE -> pickTongue(container.settingsRepository.currentLanguageIndex())
        ScreenTwo.STEP1 -> leave { show(ScreenTwo.STEP2) }
        ScreenTwo.STEP2 -> show(ScreenTwo.STEP1)
        ScreenTwo.STEP3 -> show(ScreenTwo.STEP2)
    }

    private fun show(next: ScreenTwo) {
        screen = next
        timer?.cancel()
        MetricsTwo.step(MetricsTwo.UNINSTALL, next.name.lowercase())
        val flow = container.config.value.uninstallFlow
        splash?.root?.let(b.root::removeView)
        splash = null
        // Every page shows its own parts; the rest is hidden.
        listOf(b.header.root, b.pageTitle, b.sub, b.list, b.rowsScroll, b.center, b.progress, b.percent, b.primary, b.secondary)
            .forEach { it.visibility = View.GONE }
        b.adSlot.removeAllViews()
        if (next != ScreenTwo.SPLASH) Spots.bottom(this, b.adSlot, place())
        when (next) {
            ScreenTwo.SPLASH -> presentBoot(flow.splashMs)
            ScreenTwo.LANGUAGE -> {
                show(b.pageTitle, b.list, b.primary)
                b.pageTitle.setText(R.string.un_language_title)
                // A tap marks the row and moves on after 250 ms; Continue moves on with the current pick.
                var pick = container.settingsRepository.currentLanguageIndex()
                b.list.layoutManager = LinearLayoutManager(this)
                b.list.adapter = ChooseAdapter(tonguePhase(container.settingsRepository.languages), setOf(pick)) {
                    pick = it
                    (b.list.adapter as ChooseAdapter).kitSubmitSelection(setOf(it))
                    b.list.postDelayed({ if (screen == ScreenTwo.LANGUAGE) pickTongue(it) }, 250)
                }
                b.primary.setText(R.string.next)
                b.primary.setOnClickListener { pickTongue(pick) }
            }
            ScreenTwo.STEP1 -> {
                show(b.header.root, b.pageTitle, b.sub, b.rowsScroll, b.primary, b.secondary)
                b.pageTitle.setText(R.string.un_sorry_title)
                b.sub.setText(R.string.un_sorry_body)
                b.rows.removeAllViews()
                TIPS.forEach { r ->
                    RowToolBinding.inflate(layoutInflater, b.rows, true).apply {
                        icon.setImageResource(r.icon)
                        name.setText(r.label)
                        sub.setText(r.fix)
                        root.setOnClickListener { fix(r) }
                    }
                }
                b.primary.setText(R.string.un_keep)
                b.primary.setOnClickListener { keepTool() }
                b.secondary.setText(R.string.un_still)
                b.secondary.setOnClickListener { MetricsTwo.click("still_uninstall"); leave { show(ScreenTwo.STEP2) } }
            }
            ScreenTwo.STEP2 -> {
                show(b.header.root, b.pageTitle, b.rowsScroll, b.secondary)
                b.pageTitle.setText(R.string.un_reason_title)
                wireReasons()
                b.secondary.setText(R.string.un_keep)
                b.secondary.setOnClickListener { keepTool() }
            }
            ScreenTwo.PROGRESS -> {
                show(b.center, b.progress, b.percent)
                b.centerTitle.text = getString(R.string.un_progress, getString(R.string.app_name))
                b.centerSub.setText(R.string.un_progress_sub)
                runHeadway(flow.progressMs.coerceIn(500L, 20_000L))
            }
            ScreenTwo.STEP3 -> {
                show(b.header.root, b.center, b.primary, b.secondary)
                b.centerTitle.setText(R.string.un_confirm_title)
                b.centerSub.setText(R.string.un_confirm_body)
                b.primary.setText(R.string.un_keep)
                b.primary.setOnClickListener { keepTool() }
                b.secondary.setText(R.string.un_uninstall)
                b.secondary.setOnClickListener { MetricsTwo.click("uninstall"); leave { end() } }
                // As the reference: no tap within 3 s ends the flow.
                timer = lifecycleScope.launch { delay(3_000); leave { end() } }
            }
        }
    }

    private fun show(vararg views: View) = views.forEach { it.visibility = View.VISIBLE }

    /** The app's own splash (art, logo, name, bar) for `splash_ms`, as the reference shows its splash screen here. */
    private fun presentBoot(durationMs: Long) {
        val s = ScreenBootBinding.inflate(layoutInflater, b.root, false)
        b.root.addView(s.root)
        s.content.padForOsBars()
        splash = s
        ObjectAnimator.ofInt(s.progress, "progress", 0, 100).setDuration(durationMs).start()
        timer = lifecycleScope.launch { delay(durationMs); kitLeaveWhenFree { show(ScreenTwo.LANGUAGE) } }
    }

    /** Phase 2: a picked reason opens its fix, the fix button and "Uninstall anyway" (→ progress). */
    private fun wireReasons() {
        b.rows.removeAllViews()
        PackCause.entries.forEach { r ->
            val selected = picked == r
            RowFarewellReasonBinding.inflate(layoutInflater, b.rows, true).apply {
                card.strokeColor = getColor(if (selected) R.color.sh_primary else R.color.sh_border)
                radio.setImageResource(if (selected) R.drawable.glyph_radio_checked else R.drawable.glyph_radio_unchecked)
                if (selected) radio.setColorFilter(getColor(R.color.sh_primary))
                label.setText(r.label)
                row.setOnClickListener {
                    MetricsTwo.click("reason", r.name.lowercase())
                    MetricsTwo.step(MetricsTwo.UNINSTALL, "reason", r.name.lowercase())
                    picked = r
                    wireReasons()
                }
                fix.visibility = if (selected) View.VISIBLE else View.GONE
                fixText.setText(r.fix)
                fixButton.setText(r.fixButton)
                fixButton.setOnClickListener { fix(r) }
                anyway.setOnClickListener { MetricsTwo.click("uninstall_anyway"); leave { show(ScreenTwo.PROGRESS) } }
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
            kitLeaveWhenFree { show(ScreenTwo.STEP3) }
        }
    }

    private fun place() = "uninstall_flow.${screen.name.lowercase()}"

    private fun pickTongue(index: Int) {
        MetricsTwo.click("language", container.settingsRepository.languages[index].tag)
        leave {
            show(ScreenTwo.STEP1)
            container.settingsRepository.setLanguage(index) // in place: onConfigurationChanged redraws step 1
        }
    }

    /** Runs the screen's exit ad, then [action]: once, even on double taps. */
    private fun leave(action: () -> Unit) {
        if (ExitPromoRunner.busy()) return
        val from = screen
        Spots.exit(this, place()) { if (screen == from && !isFinishing) action() }
    }

    /** Views that move on by themselves wait out an ad still on screen instead of dropping the step. */
    private fun kitLeaveWhenFree(action: () -> Unit) {
        lifecycleScope.launch {
            while (ExitPromoRunner.busy()) delay(300)
            leave(action)
        }
    }

    private fun keepTool() {
        MetricsTwo.click("keep")
        MetricsTwo.step(MetricsTwo.UNINSTALL, "keep")
        startActivity(Intent(this, RootActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        finish()
    }

    /** A quick fix: back into the app at the place that fixes it. */
    private fun fix(r: PackCause) {
        MetricsTwo.click("fix", r.name.lowercase())
        MetricsTwo.step(MetricsTwo.UNINSTALL, "fix", r.name.lowercase())
        val home = Intent(this, RootActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        when (r) {
            PackCause.BROKEN -> startActivity(home.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK))
            PackCause.ADS, PackCause.HARD -> startActivities(arrayOf(home, Intent(this, SettingsActivity::class.java)))
            PackCause.CONTENT, PackCause.NOT_NEEDED -> startActivity(home)
        }
        finish()
    }

    /** As the reference: paid hides our icon (the app stays installed, the user is back on the launcher); organic opens LoanTwo info. */
    private fun end() {
        timer?.cancel()
        endFarewell(this, paid)
        finish()
    }

    private companion object {
        const val KEY_SCREEN = "screen"
        const val KEY_REASON = "reason"
        val TIPS = listOf(PackCause.BROKEN, PackCause.CONTENT, PackCause.HARD)
    }
}

/** The end of the premium uninstall (flow or dialog), as the reference: paid hides our icon; organic opens LoanTwo info. */
internal fun endFarewell(activity: android.app.Activity, paid: Boolean) {
    if (paid) {
        MetricsTwo.step(MetricsTwo.UNINSTALL, "hidden")
        OwnMark.hide(activity)
    } else {
        MetricsTwo.step(MetricsTwo.UNINSTALL, "app_info")
        runCatching {
            activity.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", activity.packageName, null)))
        }
    }
}
