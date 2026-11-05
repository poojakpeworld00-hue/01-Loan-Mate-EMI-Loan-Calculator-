package com.loanmate.loan.emicalculator.carloan.homelloan.screens.intro

import com.loanmate.loan.emicalculator.carloan.homelloan.screens.nudges.RateQuery
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import com.loanmate.loan.emicalculator.carloan.homelloan.R
import com.loanmate.loan.emicalculator.carloan.homelloan.screens.shared.kitEnableLightEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.loanmate.loan.emicalculator.carloan.adkit.PromoPack
import com.loanmate.loan.emicalculator.carloan.adkit.Spots
import com.loanmate.loan.emicalculator.carloan.adkit.metrics.Views
import com.loanmate.loan.emicalculator.carloan.homelloan.store.cloud.flags.LoanSpecHolder
import com.loanmate.loan.emicalculator.carloan.homelloan.kit.device.NoConnected
import com.loanmate.loan.emicalculator.carloan.homelloan.databinding.ScreenBootBinding
import com.loanmate.loan.emicalculator.carloan.homelloan.screens.shared.toolScreenModel
import com.loanmate.loan.emicalculator.carloan.homelloan.screens.shared.kitGlowPulse
import com.loanmate.loan.emicalculator.carloan.homelloan.screens.shared.padForSysBars
import com.loanmate.loan.emicalculator.carloan.homelloan.screens.shared.kitShine
import com.loanmate.loan.emicalculator.carloan.homelloan.screens.dashboard.RootActivity
import com.loanmate.loan.emicalculator.carloan.homelloan.screens.welcome.OnboardActivity
import com.intuit.sdp.R as SdpR
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import com.loanmate.loan.emicalculator.carloan.homelloan.kit.device.ToggleShield
import com.loanmate.loan.emicalculator.carloan.homelloan.store.cloud.flags.UniqueAreas
import com.loanmate.loan.emicalculator.carloan.homelloan.store.cloud.flags.ShipOrigin
import com.loanmate.loan.emicalculator.carloan.homelloan.screens.welcome.OnboardScreenModel
import com.loanmate.loan.emicalculator.carloan.homelloan.screens.access.Grants

/** Splash, "Splash Loader" blue tile: ring loader and status lines while consent, config and the splash ad run, then routes. */
class StartActivity : AppCompatActivity() {
    private val viewModel by toolScreenModel { StartScreenModel(settingsRepository) }

    private fun route(resume: Boolean = false) {
        if (isFinishing) return
        val target = when (viewModel.destination()) {
            StartScreenModel.PackTarget.HOME -> RootActivity::class.java
            StartScreenModel.PackTarget.ONBOARDING -> OnboardActivity::class.java
        }
        startActivity(Intent(this, target).putExtra(OnboardScreenModel.EXTRA_RESUME, resume))
        finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        kitEnableLightEdgeToEdge()
        // Our task was removed for Recents (ToggleShield): the app reopens where the user was, without the splash.
        if (savedInstanceState == null && ToggleShield.takeAdvance()) return route(resume = true)
        if (savedInstanceState == null) RateQuery.countJoin(this)
        val b = ScreenBootBinding.inflate(layoutInflater)
        setContentView(b.root)
        b.contentVw.padForSysBars()

        b.glowVw.kitGlowPulse()
        b.logoVw.alpha = 0f; b.logoVw.scaleX = 0.82f; b.logoVw.scaleY = 0.82f; b.logoVw.translationY = 10f
        b.logoVw.animate().alpha(1f).scaleX(1f).scaleY(1f).translationY(0f).setStartDelay(100).setDuration(650)
            .setInterpolator(OvershootInterpolator(1.4f)).start()
        listOf(b.nameVw to 450L, b.tagVw to 600L, b.bottomVw to 700L).forEach { (v, delay) ->
            v.alpha = 0f; v.translationY = 12f
            v.animate().alpha(1f).translationY(0f).setStartDelay(delay).setDuration(500).setInterpolator(DecelerateInterpolator(2f)).start()
        }
        b.ring1Vw.kitRingExit(1050)
        b.ring2Vw.kitRingExit(2150)
        kitCycleMessages(b)

        val progress = ObjectAnimator.ofInt(b.prog, "progress", 0, 90).apply {
            duration = LoanSpecHolder.current.value.base.splashWaitSec * 1000L + CONFIG_WAIT_MS / 2
            addUpdateListener { b.ringVw.progress = it.animatedValue as Int }
            start()
        }
        // Reference startup order: consent → preload → hold → config → splash ad → route.
        lifecycleScope.launch {
            // Offline: the No internet sheet is up ([NoConnected]); start once the connection is back, with a fresh config fetch.
            if (LoanSpecHolder.current.value.base.noInternetDialog && !NoConnected.online.value) {
                NoConnected.online.first { it }
                LoanSpecHolder.alignIfNeverFetched(this@StartActivity)
            }
            val consent = CompletableDeferred<Unit>()
            PromoPack.askAccept(this@StartActivity) {
                PromoPack.preload(this@StartActivity)
                consent.complete(Unit)
            }
            delay(LoanSpecHolder.current.value.base.splashWaitSec * 1000L)
            withTimeoutOrNull(CONFIG_WAIT_MS) { LoanSpecHolder.ready.first { it } }
            consent.await()
            // Organic / paid verdict and IP location (≤3 s), then fixed for this run; later verdicts apply next launch.
            withTimeoutOrNull(ATTRIBUTION_WAIT_MS) {
                ShipOrigin.await(ATTRIBUTION_WAIT_MS)
                UniqueAreas.await(ATTRIBUTION_WAIT_MS)
            }
            ShipOrigin.lock()
            UniqueAreas.lock()
            suspendCancellableCoroutine { done -> Grants.queryFor(this@StartActivity, "splash") { done.resume(Unit) } }
            progress.cancel()
            b.prog.setProgressCompat(100, true)
            b.ringVw.setProgressCompat(100, true)
            Spots.exit(this@StartActivity, Spots.SPLASH) { Views.outBy("next"); route() }
        }
    }

    override fun onResume() {
        super.onResume()
        Views.show("splash")
    }

    /** The design's four status lines, 0.9 s each with pulsing dots; the last one stays until the splash routes. */
    private fun kitCycleMessages(b: ScreenBootBinding) {
        val lines = listOf(R.string.splash_msg_1, R.string.splash_msg_2, R.string.splash_msg_3, R.string.splash_msg_4)
        lifecycleScope.launch {
            var tick = 0
            while (true) {
                val line = (tick / 3).coerceAtMost(lines.lastIndex)
                if (tick % 3 == 0 && tick / 3 <= lines.lastIndex) {
                    b.msgVw.alpha = 0f; b.msgVw.translationY = 8f
                    b.msgVw.animate().alpha(1f).translationY(0f).setDuration(320).start()
                }
                b.msgVw.text = getString(lines[line]) + ".".repeat(tick % 3 + 1)
                tick++
                delay(300)
            }
        }
    }

    /** Design "ringOut": the outline grows from 0.8× to 1.5× while fading out, every 2 s. */
    private fun View.kitRingExit(delayMs: Long) {
        ObjectAnimator.ofPropertyValuesHolder(
            this,
            PropertyValuesHolder.ofFloat(View.SCALE_X, 0.8f, 1.5f),
            PropertyValuesHolder.ofFloat(View.SCALE_Y, 0.8f, 1.5f),
            PropertyValuesHolder.ofFloat(View.ALPHA, 0.7f, 0f),
        ).apply {
            duration = 2000
            startDelay = delayMs
            repeatCount = ValueAnimator.INFINITE
            interpolator = DecelerateInterpolator()
            start()
        }
    }

    private companion object {
        const val SPLASH_MS = 2800L
        /** Longest wait for the first Remote Config fetch on a first launch. */
        const val CONFIG_WAIT_MS = 6_000L
        const val ATTRIBUTION_WAIT_MS = 3_000L
    }
}
