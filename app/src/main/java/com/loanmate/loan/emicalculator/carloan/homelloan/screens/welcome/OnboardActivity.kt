package com.loanmate.loan.emicalculator.carloan.homelloan.screens.welcome

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.animation.LinearInterpolator
import android.widget.LinearLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import java.lang.ref.WeakReference
import java.util.Locale
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.MediaPlayer
import android.os.Bundle
import android.content.res.Configuration
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import androidx.activity.addCallback
import com.loanmate.loan.emicalculator.carloan.homelloan.screens.shared.kitEnableDarkEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withResumed
import androidx.recyclerview.widget.GridLayoutManager
import com.loanmate.loan.emicalculator.carloan.adkit.PromoPack
import com.loanmate.loan.emicalculator.carloan.adkit.Spots
import com.loanmate.loan.emicalculator.carloan.adkit.ExitPromoRunner
import com.loanmate.loan.emicalculator.carloan.adkit.metrics.MetricsTwo
import com.loanmate.loan.emicalculator.carloan.adkit.metrics.Views
import com.loanmate.loan.emicalculator.carloan.homepanel.Launcher
import com.loanmate.loan.emicalculator.carloan.homelloan.LoanTwo
import com.loanmate.loan.emicalculator.carloan.homelloan.R
import com.loanmate.loan.emicalculator.carloan.homelloan.kit.device.PrimeScreens
import com.loanmate.loan.emicalculator.carloan.homelloan.kit.device.ToggleShield
import com.loanmate.loan.emicalculator.carloan.homelloan.kit.device.SysShortcutAim
import com.loanmate.loan.emicalculator.carloan.homelloan.store.cloud.flags.SysShortcut
import com.loanmate.loan.emicalculator.carloan.homelloan.databinding.ScreenSetupBinding
import com.loanmate.loan.emicalculator.carloan.homelloan.databinding.SheetSetupBinding
import com.loanmate.loan.emicalculator.carloan.homelloan.screens.shared.kitAccentBold
import com.loanmate.loan.emicalculator.carloan.homelloan.screens.shared.toolScreenModel
import com.loanmate.loan.emicalculator.carloan.homelloan.screens.shared.kitCollectWhenStarted
import com.loanmate.loan.emicalculator.carloan.homelloan.screens.shared.kitGlowPulse
import com.loanmate.loan.emicalculator.carloan.homelloan.screens.shared.maskNavBar
import com.loanmate.loan.emicalculator.carloan.homelloan.screens.shared.padForSysBars
import com.loanmate.loan.emicalculator.carloan.homelloan.screens.shared.kitShine
import com.loanmate.loan.emicalculator.carloan.homelloan.screens.dashboard.RootActivity
import com.loanmate.loan.emicalculator.carloan.homelloan.screens.access.Grants
import com.loanmate.loan.emicalculator.carloan.homelloan.screens.welcome.OnboardChecks.ShortcutResult
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.intuit.sdp.R as SdpR
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

/**
 * Onboarding, Claude Design "Skin Hub Onboarding v2", with the reference app's onboarding behaviour: Remote Config order,
 * an exit ad on every way forward, system keys (`launcher.system_keys`), the bottom sheet and the set-as-default-home step.
 */
class OnboardActivity : AppCompatActivity(), SysShortcutAim {
    private val viewModel by toolScreenModel { handle ->
        OnboardScreenModel(settingsRepository, config.value, Launcher.isDefaultHome(this@OnboardActivity), handle)
    }
    private val config get() = (application as LoanTwo).container.config.value
    private lateinit var b: ScreenSetupBinding
    /** Where Continue is pinned, and its layout there, to put it back after a step showed it in the list. */
    private lateinit var ctaHome: ViewGroup
    private lateinit var ctaParams: ViewGroup.LayoutParams
    private var adapter: ChooseAdapter? = null
    private var shownStep = -1

    /** Home / Back / Recents for the flow; acted on only while this screen is resumed and no exit ad runs. */
    private val systemKeys = Channel<SysShortcut>(Channel.CONFLATED)
    private var keyHandledAt = 0L
    private var sheet: BottomSheetDialog? = null

    // default_home loop: Settings list with the hint → back → role dialog; not set → stay, and Back / Home / the button
    // start it again. Only setting us as home moves on.
    private var askingHome = false
    private var homeGrantedAt = 0L
    private var leftWithHome = false
    private val homeDemo by lazy { RootBootGallery(b.homeDemoVw) }
    private var waitedForLocale = false
    private val finishAnyway = Runnable { finishOnboard() }
    private val reAsk = Runnable { if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) && currentKey() == DEFAULT_HOME_KEY) queryRootRoot() }

    /** Home while we are not the default home yet: the launcher can't see it, so the flow hears it here. */
    private val keyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.getStringExtra("reason") == "homekey" && !Launcher.isDefaultHome(context)) systemKey(SysShortcut.HOME)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        kitEnableDarkEdgeToEdge()
        b = ScreenSetupBinding.inflate(layoutInflater)
        setContentView(b.root)
        b.contentVw.padForSysBars()
        b.glowVw.kitGlowPulse()
        b.ctaShineVw.kitShine(b.ctaVw, 3200)
        b.ctaVw.clipToOutline = true
        ctaHome = b.ctaVw.parent as ViewGroup
        ctaParams = b.ctaVw.layoutParams
        // A key forwarded to a fresh screen was the press that reopened it, not one on our step.
        if (savedInstanceState == null) intent.removeExtra(SysShortcutAim.EXTRA_SYSTEM_KEY)

        b.backVw.setOnClickListener { MetricsTwo.click("back"); viewModel.back() }
        b.skipVw.setOnClickListener { MetricsTwo.click("skip"); advance() }
        b.nextVw.setOnClickListener {
            MetricsTwo.click("continue")
            primaryJob()
        }
        b.favAllVw.setOnClickListener { viewModel.switchEveryStars() }
        onBackPressedDispatcher.addCallback(this) { MetricsTwo.hotkeyPress("back"); onSysShortcut(SysShortcut.BACK) }

        kitCollectWhenStarted(viewModel.state, ::render)
        kitCollectWhenStarted(viewModel.finished) { finishOnboard() }
        lifecycleScope.launch { for (key in systemKeys) lifecycle.withResumed { onSysShortcut(key) } }
        // Back after Recents removed our task with the setup sheet open: as the reference, it opens again.
        if (savedInstanceState == null && viewModel.resumed && OnboardProgress.sheet) b.root.post { presentDock() }
        // The language picked on the last step relaunches this screen on some phones (despite configChanges): the new
        // one, already in that language, goes straight on to Home before it is ever drawn.
        if (viewModel.done) finishOnboard()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        SysShortcut.entries.firstOrNull { it.key == intent.getStringExtra(SysShortcutAim.EXTRA_SYSTEM_KEY) }?.let(::systemKey)
        intent.removeExtra(SysShortcutAim.EXTRA_SYSTEM_KEY)
    }

    override fun systemKey(key: SysShortcut) {
        // Pressed while an exit ad (ad or links) is running: dropped, so it can't act once the ad ends.
        if (!ExitPromoRunner.busy()) systemKeys.trySend(key)
    }

    override fun onStart() {
        super.onStart()
        if (currentKey() == DEFAULT_HOME_KEY) {
            if (Launcher.isDefaultHome(this)) baseGranted() else homeDemo.start()
        }
    }

    override fun onStop() {
        // KitAway in Settings for the request: the step stays blank until its result, so after picking us the next step is
        // the first thing shown (some phones report the role late, after our first frame).
        if (askingHome) b.contentVw.visibility = View.INVISIBLE
        homeDemo.stop()
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        // Our launcher handed HOME back to us and we are on screen: its loop guard starts over, so fast HOME taps
        // during onboarding never make it give up and show the launcher home (the user stays in onboarding).
        Launcher.hostShown()
        live = WeakReference(this)
        if (grantPending) grantPromoOver()
        // The sheet opens on Home / Recents, which pause us for a moment: the guide carries on where it was.
        if (voicePausedByLeaving) {
            voicePausedByLeaving = false
            voice?.start()
        }
        @Suppress("DEPRECATION") // still delivered to apps for Home/Recents
        ContextCompat.registerReceiver(this, keyReceiver, IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS), ContextCompat.RECEIVER_EXPORTED)
        if (leftWithHome) {
            leftWithHome = false
            queryRootRoot()
        }
    }

    override fun onPause() {
        voice?.takeIf { it.isPlaying }?.let { it.pause(); voicePausedByLeaving = true }
        runCatching { unregisterReceiver(keyReceiver) }
        super.onPause()
    }

    override fun onDestroy() {
        releaseSpeech()
        window.decorView.removeCallbacks(reAsk)
        window.decorView.removeCallbacks(finishAnyway) // relaunched: the new screen takes over
        if (isFinishing) RootRootStep.shown = false
        super.onDestroy()
    }

    /** The step's main button: default_home asks to be home, permission_hub asks its permissions first, others go on. */
    private fun primaryJob() = when (currentKey()) {
        DEFAULT_HOME_KEY -> queryRootRoot()
        PERMISSION_HUB_KEY -> { MetricsTwo.click("allow"); Grants.queryFor(this, PERMISSION_HUB_KEY) { advance() } }
        else -> advance()
    }

    /** Continue back in its pinned spot above the bottom ad (it may be in the list from the previous step). */
    private fun kitPinCta() {
        if (b.ctaVw.parent === ctaHome) return
        (b.ctaVw.parent as? ViewGroup)?.removeView(b.ctaVw)
        ctaHome.addView(b.ctaVw, ctaHome.indexOfChild(b.adSlotVw), ctaParams)
    }

    private fun place(step: Phase) = "onboarding.${step.key}"

    private fun currentKey() = viewModel.steps.getOrNull(viewModel.state.value.stepIndex)?.key

    /**
     * The ONE way forward: Onward, Continue, Skip, bottom sheet Continue, and a system key's `next`. Runs the step's exit
     * ad first; a [key] whose button ad is on during onboarding runs that ad instead (one ad per press).
     */
    private fun advance(key: SysShortcut? = null) {
        if (ExitPromoRunner.busy()) return
        dismissPanel()
        val step = currentKey() ?: return viewModel.skip()
        val place = key?.takeIf { Spots.hotkeyPromoDuringOnboard(it.key) }?.let { "launcher.${it.key}_button" } ?: "onboarding.$step"
        Spots.exit(this, place) {
            if (key == null) Views.outBy("next") // a system key already said how the visit ends
            viewModel.next()
        }
    }

    private fun onSysShortcut(key: SysShortcut) {
        // One press at a time: while its action (and the exit ad it runs) is going, other presses are ignored.
        val now = SystemClock.elapsedRealtime()
        if (ExitPromoRunner.busy() || now - keyHandledAt < KEY_REPEAT_MS) return
        keyHandledAt = now
        if (key == SysShortcut.HOME && now - homeGrantedAt < 5_000) return
        val notHome = !Launcher.isDefaultHome(this)
        // Only the grant moves on from default_home.
        if (currentKey() == DEFAULT_HOME_KEY) {
            when (key) {
                SysShortcut.HOME -> if (notHome) leftWithHome = true
                SysShortcut.BACK -> queryRootRoot()
                SysShortcut.RECENTS -> Unit
            }
            return
        }
        if (notHome && key == SysShortcut.HOME) return // Home keys elsewhere work only while we are the default home
        fun hotkeyPromo(then: () -> Unit) {
            if (!Spots.hotkeyPromoDuringOnboard(key.key)) return then()
            if (!ExitPromoRunner.busy()) Spots.exit(this, "launcher.${key.key}_button", then)
        }
        when (val r = OnboardChecks.onKey(config.systemKeys, key, viewModel.state.value.stepIndex, sheetOpen = sheet?.isShowing == true)) {
            ShortcutResult.ShowDock -> presentDock()
            ShortcutResult.Onward -> advance(key)
            is ShortcutResult.PackGoTo -> hotkeyPromo { dismissPanel(); viewModel.kitGoTo(r.index) }
            ShortcutResult.QuitLoan -> hotkeyPromo { finish() }
            // Back like Android's own: the app goes to the background and onboarding waits where it is.
            ShortcutResult.GearRoot -> if (key == SysShortcut.BACK && !moveTaskToBack(true)) finish()
            ShortcutResult.PackNothing -> hotkeyPromo {}
        }
    }

    /**
     * `system_keys.bottom_sheet`: the reference app's "Finish setup" sheet: progress, the step up next, and (from the sheet's
     * second opening) the voice guide, auto-played in the app's language. Continue setup and Go to Home go forward; the close button closes it.
     */
    private fun dismissPanel() {
        OnboardProgress.sheet = false
        sheet?.dismiss()
    }

    private fun presentDock() {
        if (sheet?.isShowing == true) return
        Views.show("onboarding_bottom_sheet")
        val prefs = getSharedPreferences("onboarding_sheet", MODE_PRIVATE)
        val opens = prefs.getInt("opened", 0) + 1
        prefs.edit().putInt("opened", opens).apply()

        // Counts only the OB_ screens, as the step tracker: welcome / permission_hub / default_home sit outside it.
        val all = viewModel.steps
        val at = viewModel.state.value.stepIndex
        val current = all[at]
        val steps = all.filter { it.key !in INFO_STEPS }.ifEmpty { all }
        val index = steps.indexOf(current).takeIf { it >= 0 } ?: steps.count { all.indexOf(it) < at }.coerceAtMost(steps.lastIndex)
        val v = SheetSetupBinding.inflate(layoutInflater)
        val number = String.format(Locale.getDefault(), "%02d", index + 1)
        v.sheetBadgeVw.text = number
        v.sheetEyebrowVw.text = getString(R.string.sh_mission, number, steps.size.toString())
        v.sheetNextVw.setText(current.name)
        wireSegments(v.sheetSegmentsVw, index, steps.size)
        v.sheetSegmentsVw.contentDescription = getString(R.string.sh_done, index, steps.size)
        v.sheetDoneVw.text = getString(R.string.sh_done, index, steps.size)
        v.sheetXpVw.text = getString(R.string.sh_xp, index * 100)
        v.sheetTitleVw.text = getText(R.string.sh_title).kitAccentBold(v.sheetTitleVw)
        val fraction = index.toFloat() / steps.size
        val mood = getString(if (fraction < 1f / 3) R.string.sh_mood_start else if (fraction < 2f / 3) R.string.sh_mood_half else R.string.sh_mood_almost)
        val left = steps.size - index
        v.sheetMoodVw.text = SpannableStringBuilder(mood).apply {
            setSpan(ForegroundColorSpan(ContextCompat.getColor(this@OnboardActivity, R.color.sh_primary)), 0, length, 0)
            setSpan(StyleSpan(Typeface.BOLD), 0, length, 0)
            append(" · ").append(resources.getQuantityString(R.plurals.sh_to_go, left, left))
        }
        v.sheetSavedVw.text = SpannableStringBuilder(getString(R.string.sh_saved)).apply {
            setSpan(ForegroundColorSpan(0xFFC4CEE6.toInt()), 0, length, 0)
            setSpan(StyleSpan(Typeface.BOLD), 0, length, 0)
            append(" ").append(getString(R.string.sh_saved_text))
        }
        v.sheetGlowVw.kitGlowPulse()
        ObjectAnimator.ofFloat(v.sheetRingVw, View.ROTATION, 0f, 360f).apply {
            duration = 9000
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            start()
        }
        v.sheetShineVw.kitShine(v.sheetCtaVw, 2600)
        if (opens >= 2) wireSpeechCoach(v, index)
        v.sheetContinueVw.setOnClickListener { MetricsTwo.click("continue"); advance() }
        // As the reference: "Go to Home" moves on like Continue; only the close button (or Back) closes the sheet.
        v.sheetCloseVw.setOnClickListener { MetricsTwo.click("go_home"); advance() }
        v.sheetXVw.setOnClickListener { MetricsTwo.click("dismiss"); dismissPanel() }
        OnboardProgress.sheet = true
        sheet = BottomSheetDialog(this).apply {
            setContentView(v.root)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.skipCollapsed = true
            // Modal, as the reference: a tap outside or a swipe down doesn't close it.
            setCanceledOnTouchOutside(false)
            behavior.isDraggable = false
            // As the reference: Back over the sheet is the system key's own action (system_keys), not a plain close.
            onBackPressedDispatcher.addCallback { MetricsTwo.hotkeyPress("back"); onSysShortcut(SysShortcut.BACK) }
            // Dismiss arrives posted: a sheet reopened meanwhile owns the voice now, so an old sheet leaves it alone.
            setOnDismissListener { if (sheet === this) releaseSpeech() }
            show()
            window?.maskNavBar()
            (v.root.parent as? View)?.setBackgroundColor(Color.TRANSPARENT) // our sheet draws its own rounded surface
        }
    }

    /** The design's segmented progress: done steps lit, the current one wider and pulsing, the rest dim. */
    private fun wireSegments(row: LinearLayout, index: Int, count: Int) {
        val gap = resources.getDimensionPixelSize(SdpR.dimen._3sdp)
        val radius = 2 * resources.displayMetrics.density
        val accent = ContextCompat.getColor(this, R.color.sh_primary)
        for (k in 0 until count) {
            val seg = View(this).apply {
                background = GradientDrawable().apply {
                    cornerRadius = radius
                    setColor(if (k <= index) accent else 0x1AFFFFFF)
                }
                if (k == index) ObjectAnimator.ofFloat(this, View.ALPHA, 0.35f, 1f).apply {
                    duration = 800
                    repeatMode = ValueAnimator.REVERSE
                    repeatCount = ValueAnimator.INFINITE
                    start()
                }
            }
            row.addView(seg, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, if (k == index) 2.4f else 1f).apply {
                if (k > 0) marginStart = gap
            })
        }
    }

    private var voice: MediaPlayer? = null
    private var voicePausedByLeaving = false

    /** The voice guide card: play / pause / replay the clip in the app's language. */
    private fun wireSpeechCoach(v: SheetSetupBinding, index: Int) {
        val settings = (application as LoanTwo).container.settingsRepository
        releaseSpeech() // an earlier sheet's player, if its dismiss hasn't run yet
        val player = MediaPlayer.create(this, speechClip(settings.languages[settings.currentLanguageIndex()].tag)) ?: return
        voice = player
        v.voiceCardVw.visibility = View.VISIBLE
        v.voiceWaveVw.seed(index)
        val ripple = ObjectAnimator.ofPropertyValuesHolder(
            v.voiceRippleVw,
            PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.7f),
            PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.7f),
            PropertyValuesHolder.ofFloat(View.ALPHA, 0.6f, 0f),
        ).apply { duration = 1300; repeatCount = ValueAnimator.INFINITE }
        fun time(ms: Int) = (ms / 1000).let { "%d:%02d".format(Locale.getDefault(), it / 60, it % 60) }
        // Play head, time and waveform follow the clip while the sheet is open.
        val tick = object : Runnable {
            override fun run() {
                val p = voice ?: return
                v.voiceWaveVw.progress = p.currentPosition.toFloat() / p.duration.coerceAtLeast(1)
                v.voiceTimeVw.text = time(if (p.isPlaying) p.currentPosition else p.duration)
                if (p.isPlaying) v.voiceWaveVw.postDelayed(this, 80)
            }
        }
        fun show(playing: Boolean) {
            v.voicePlayVw.setImageResource(if (playing) R.drawable.glyph_pause else R.drawable.glyph_play)
            v.voicePlayVw.contentDescription = getString(if (playing) R.string.sh_pause else R.string.sh_play)
            v.voiceWaveVw.playing = playing
            if (playing) { ripple.start(); v.voiceWaveVw.post(tick) } else { ripple.cancel(); v.voiceRippleVw.alpha = 0f }
        }
        v.voiceTimeVw.text = time(player.duration)
        player.setOnCompletionListener {
            show(false)
            v.voiceWaveVw.progress = 1f
            v.voicePlayVw.setImageResource(R.drawable.glyph_replay)
            v.voicePlayVw.contentDescription = getString(R.string.sh_replay)
            v.voiceTextVw.setText(R.string.sh_voice_done)
        }
        v.voicePlayVw.setOnClickListener {
            MetricsTwo.click("voice_guide")
            if (player.isPlaying) player.pause() else {
                v.voiceTextVw.setText(R.string.sh_voice_text)
                player.start()
            }
            show(player.isPlaying)
        }
        player.start() // auto-plays, as the reference
        show(true)
    }

    private fun releaseSpeech() {
        voice?.release()
        voice = null
        voicePausedByLeaving = false
    }

    private fun speechClip(tag: String) = when (tag) {
        "hi" -> R.raw.intro_voice_hi
        "id" -> R.raw.intro_voice_in
        "tr" -> R.raw.intro_voice_tr
        "vi" -> R.raw.intro_voice_vi
        "ar" -> R.raw.intro_voice_ar
        "fr" -> R.raw.intro_voice_fr
        "de" -> R.raw.intro_voice_de
        "ja" -> R.raw.intro_voice_ja
        else -> R.raw.intro_voice_en
    }

    private fun onRootRootStep(onIt: Boolean) {
        RootRootStep.shown = onIt
        // The reference's animated demo replaces the option list on this step; its button carries the home icon.
        b.listVw.visibility = if (onIt) View.GONE else View.VISIBLE
        b.homeDemoVw.root.visibility = if (onIt) View.VISIBLE else View.GONE
        b.nextVw.icon = if (onIt) ContextCompat.getDrawable(this, R.drawable.glyph_home) else null
        b.nextVw.iconGravity = com.google.android.material.button.MaterialButton.ICON_GRAVITY_TEXT_START
        if (onIt) homeDemo.start() else homeDemo.stop()
        if (onIt) queryRootRoot() // Settings opens straight away
    }

    private fun queryRootRoot() {
        if (askingHome) return
        askingHome = true
        Launcher.requestDefaultHome(this) { granted ->
            askingHome = false
            b.contentVw.visibility = View.VISIBLE
            if (granted) baseGranted() else kitDeclined()
        }
    }

    /** Cancelled: the step shows for `default_home.auto_open_settings_sec`, then Settings opens again (only while on screen). */
    private fun kitDeclined() {
        val sec = config.onboarding.defaultHomeAutoOpenSettingsSec
        window.decorView.removeCallbacks(reAsk)
        if (sec > 0) window.decorView.postDelayed(reAsk, sec * 1_000L)
    }

    /** We're the default home now: on to the next step, default_home's exit ad over it; then onboarding moves to the home task. */
    private fun baseGranted() {
        // Picking us as default home closes this screen and opens a new one, which (as we are default home now) starts
        // past default_home. The grant then lands on the old, destroyed screen, where no ad can show: the live screen
        // takes it over and runs default_home's exit ad itself.
        if (isDestroyed || isFinishing) {
            live?.get()?.takeIf { !it.isDestroyed && !it.isFinishing && it !== this }?.grantPromoOver() ?: run { grantPending = true }
            return
        }
        grantPending = false
        if (currentKey() != DEFAULT_HOME_KEY) return
        homeGrantedAt = SystemClock.elapsedRealtime()
        window.decorView.removeCallbacks(reAsk)
        RootRootStep.shown = false
        if (viewModel.state.value.stepIndex < viewModel.steps.lastIndex && !ExitPromoRunner.busy()) {
            Views.outBy("next")
            viewModel.next()
            Spots.exit(this, "onboarding.$DEFAULT_HOME_KEY") { ToggleShield.moveToRootJob(this) }
        } else advance()
    }

    /** A grant this screen didn't see (it opened past default_home): default_home's exit ad runs over the current step. */
    private fun grantPromoOver() {
        grantPending = false
        if (currentKey() == DEFAULT_HOME_KEY) return baseGranted()
        if (ExitPromoRunner.busy()) return
        homeGrantedAt = SystemClock.elapsedRealtime()
        Spots.exit(this, "onboarding.$DEFAULT_HOME_KEY") { ToggleShield.moveToRootJob(this) }
    }

    /**
     * Done: always the app home first. With our launcher as the default home, it is started in Android's home stack behind
     * the app, so Back from the app home lands on it.
     */
    private fun finishOnboard() {
        if (isFinishing) return
        // A language picked on the last step reaches us a moment later (onConfigurationChanged): Home opened before then
        // stays in the old language. Wait for it, with a cap so a missed change never strands the user here.
        val wanted = AppCompatDelegate.getApplicationLocales()[0]
        if (!waitedForLocale && wanted != null && wanted.language != resources.configuration.locales[0].language) {
            waitedForLocale = true
            window.decorView.postDelayed(finishAnyway, LOCALE_WAIT_MS)
            return
        }
        window.decorView.removeCallbacks(finishAnyway)
        val home = Intent(this, RootActivity::class.java)
        if (config.launcher.on && Launcher.isDefaultHome(this)) {
            runCatching {
                startActivities(arrayOf(
                    PrimeScreens.deckRootIntent(this).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION),
                    home.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION),
                ))
            }.onFailure { startActivity(home) }
        } else startActivity(home)
        finish()
    }

    /**
     * The step's words. As the reference, a tap on the language step previews that language on this screen (name,
     * title, subtitle, buttons); Continue then applies it app-wide in place (manifest configChanges: no recreate).
     */
    private fun wireTexts(step: Phase, state: OnboardLookStyle) {
        val preview = state.selection.firstOrNull()?.takeIf { step.key == LANG_KEY }
            ?.let { viewModel.languageTag(it) }?.let(::inLanguage) ?: this
        b.stepNameVw.text = preview.getText(step.name)
        b.titleVw.text = preview.getText(step.title).kitAccentBold(b.titleVw)
        b.subVw.text = preview.getText(step.sub)
        b.skipVw.text = preview.getText(R.string.skip)
        b.backVw.contentDescription = preview.getText(R.string.back)
        b.nextVw.text = preview.getText(
            when {
                step.key == DEFAULT_HOME_KEY -> R.string.dh_button
                step.key == PERMISSION_HUB_KEY -> R.string.perm_allow
                step.key == WELCOME_KEY -> R.string.wl_button
                state.stepIndex == viewModel.steps.lastIndex -> R.string.get_started
                else -> R.string.next
            }
        )
    }

    /** This screen's resources in [tag]'s language, without changing the app's language. */
    private fun inLanguage(tag: String): Context =
        createConfigurationContext(Configuration(resources.configuration).apply { setLocale(Locale.forLanguageTag(tag)) })

    /** The language applied app-wide in place (no recreate): every word on screen again in the new language. */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        viewModel.steps.getOrNull(viewModel.state.value.stepIndex)?.let { wireTexts(it, viewModel.state.value) }
        (b.listVw.adapter as? ChooseAdapter)?.let { it.notifyItemRangeChanged(0, it.itemCount) }
        if (waitedForLocale) finishOnboard()
    }

    private fun render(state: OnboardLookStyle) {
        val steps = viewModel.steps
        if (steps.isEmpty()) return // finishing: Remote Config turned every step off
        val step = steps[state.stepIndex]
        wireTexts(step, state)
        if (state.stepIndex != shownStep) {
            val dir = if (state.stepIndex >= shownStep) 1 else -1
            val first = shownStep == -1
            shownStep = state.stepIndex
            val hasBanner = step.banner != 0
            b.bannerBoxVw.visibility = if (hasBanner) View.VISIBLE else View.GONE
            if (hasBanner) {
                b.bannerVw.setImageResource(step.banner)
                b.bannerVw.scaleX = 1.12f; b.bannerVw.scaleY = 1.12f
                b.bannerVw.animate().scaleX(1f).scaleY(1f).setDuration(900).start()
            }
            b.backVw.visibility = if (state.stepIndex == 0) View.INVISIBLE else View.VISIBLE
            // As the reference: the tracker counts only the OB_ screens; welcome / permission_hub / default_home sit outside it.
            val obSteps = steps.filter { it.key !in INFO_STEPS }
            val obNumber = obSteps.indexOf(step) + 1
            b.trackerVw.visibility = if (obNumber > 0) View.VISIBLE else View.INVISIBLE
            b.progFrame.visibility = b.trackerVw.visibility
            b.prog.max = obSteps.size.coerceAtLeast(1)
            b.stepNumVw.text = "%02d".format(obNumber)
            b.stepTotalVw.text = getString(R.string.step_of_total, obSteps.size)
            if (obNumber > 0) b.prog.setProgressCompat(obNumber, !first)
            b.titleVw.updatePadding(top = resources.getDimensionPixelSize(if (hasBanner) SdpR.dimen._30sdp else SdpR.dimen._10sdp))
            b.skipVw.visibility = if (if (step.key == DEFAULT_HOME_KEY) viewModel.defaultHomeSkipButton else viewModel.skipButton) View.VISIBLE else View.GONE
            onRootRootStep(step.key == DEFAULT_HOME_KEY)
            // As the reference: every step asks its `permissions.<step>` on open; permission_hub asks on its Allow button.
            if (step.key != PERMISSION_HUB_KEY) Grants.queryFor(this, step.key)
            b.favRowVw.visibility = if (state.stepIndex == viewModel.favIndex) View.VISIBLE else View.GONE
            val place = place(step)
            // RC decides both slots: `list_ad` sits between the options (big native), `bottom_ad` is pinned at the bottom.
            // With the in-list ad showing, Continue moves into the list right under it; otherwise it stays pinned.
            val inline = Spots.rosterOn(place)
            if (!inline) kitPinCta()
            b.adSlotVw.removeAllViews()
            Spots.bottom(this, b.adSlotVw, place)
            val adapter = ChooseAdapter(
                step, state.selection,
                inlineAd = if (inline) { slot -> Spots.list(this, slot, place) } else null,
                cta = if (inline) b.ctaVw else null,
            ) {
                MetricsTwo.click("option", step.key)
                // Information rows (launcher, welcome, permissions): a tap does what the button does.
                if (step.key in INFO_STEPS) primaryJob() else viewModel.select(it)
            }
            this.adapter = adapter
            b.listVw.layoutManager = GridLayoutManager(this, step.columns).apply {
                spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                    override fun getSpanSize(position: Int) = if (adapter.isPromo(position)) step.columns else 1
                }
            }
            b.listVw.adapter = adapter
            Views.show(step.key) // as the reference: the step's own key
            b.bodyVw.alpha = 0f
            b.bodyVw.translationX = resources.getDimension(SdpR.dimen._23sdp) * dir
            b.bodyVw.animate().alpha(1f).translationX(0f).setDuration(250).setInterpolator(DecelerateInterpolator(2f)).start()
        } else {
            adapter?.kitSubmitSelection(state.selection)
        }

        if (viewModel.favIndex < 0) return
        val favs = state.selections[viewModel.favIndex]
        val favTotal = steps[viewModel.favIndex].opts.size
        b.favCountVw.text = getString(R.string.fav_count, favs.size, favTotal)
        b.favAllVw.setText(if (favs.size == favTotal) R.string.clear_all else R.string.select_all)
    }

    private companion object {
        /** Xiaomi sends each key twice ~20 ms apart: a copy this soon is the same press. */
        const val KEY_REPEAT_MS = 300L
        const val LOCALE_WAIT_MS = 1_500L
        /** A default-home grant that reached a destroyed screen, for the next live one to handle. */
        private var grantPending = false
        /** The onboarding screen last resumed: a grant on a destroyed screen goes to it. */
        private var live: WeakReference<OnboardActivity>? = null
    }
}
