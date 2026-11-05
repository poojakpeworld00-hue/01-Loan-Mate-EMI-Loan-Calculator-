package com.loanmate.loan.emicalculator.carloan.homelloan.presentation.splash

import com.loanmate.loan.emicalculator.carloan.homelloan.presentation.prompts.GradeAsk
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
import com.loanmate.loan.emicalculator.carloan.homelloan.presentation.common.kitEnableDarkEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.loanmate.loan.emicalculator.carloan.homelloan.ads.SpotKit
import com.loanmate.loan.emicalculator.carloan.homelloan.ads.Spots
import com.loanmate.loan.emicalculator.carloan.homelloan.ads.analytics.Views
import com.loanmate.loan.emicalculator.carloan.homelloan.data.remote.config.ToolProfileKeeper
import com.loanmate.loan.emicalculator.carloan.homelloan.core.system.NoOnline
import com.loanmate.loan.emicalculator.carloan.homelloan.databinding.PageBootBinding
import com.loanmate.loan.emicalculator.carloan.homelloan.presentation.common.toolViewForm
import com.loanmate.loan.emicalculator.carloan.homelloan.presentation.common.kitGlowPulse
import com.loanmate.loan.emicalculator.carloan.homelloan.presentation.common.padForOsBars
import com.loanmate.loan.emicalculator.carloan.homelloan.presentation.common.kitShine
import com.loanmate.loan.emicalculator.carloan.homelloan.presentation.home.BaseActivity
import com.loanmate.loan.emicalculator.carloan.homelloan.presentation.onboarding.SetupActivity
import com.intuit.sdp.R as SdpR
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import com.loanmate.loan.emicalculator.carloan.homelloan.core.system.SwitcherGuard
import com.loanmate.loan.emicalculator.carloan.homelloan.data.remote.config.SpecialRegions
import com.loanmate.loan.emicalculator.carloan.homelloan.data.remote.config.DeploySource
import com.loanmate.loan.emicalculator.carloan.homelloan.presentation.onboarding.SetupViewForm
import com.loanmate.loan.emicalculator.carloan.homelloan.presentation.permissions.Grants

/** Splash, Sticker Pop "Sticker Stack": bobbing logo sticker over the art while consent, config and the splash ad run, then routes. */
class BootActivity : AppCompatActivity() {
    private val viewModel by toolViewForm { BootViewForm(settingsRepository) }

    private fun route(resume: Boolean = false) {
        if (isFinishing) return
        val target = when (viewModel.destination()) {
            BootViewForm.KitDestination.HOME -> BaseActivity::class.java
            BootViewForm.KitDestination.ONBOARDING -> SetupActivity::class.java
        }
        startActivity(Intent(this, target).putExtra(SetupViewForm.EXTRA_RESUME, resume))
        finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        kitEnableDarkEdgeToEdge()
        // Our task was removed for Recents (SwitcherGuard): the app reopens where the user was, without the splash.
        if (savedInstanceState == null && SwitcherGuard.takeProceed()) return route(resume = true)
        if (savedInstanceState == null) GradeAsk.countEnter(this)
        val b = PageBootBinding.inflate(layoutInflater)
        setContentView(b.root)
        b.content.padForOsBars()

        b.glow.kitGlowPulse()
        b.bg.scaleX = 1.1f; b.bg.scaleY = 1.1f
        b.bg.animate().scaleX(1f).scaleY(1f).setDuration(SPLASH_MS).setInterpolator(DecelerateInterpolator()).start()

        b.logo.alpha = 0f; b.logo.scaleX = 0.6f; b.logo.scaleY = 0.6f
        b.logo.animate().alpha(1f).scaleX(1f).scaleY(1f).setStartDelay(60).setDuration(600)
            .setInterpolator(OvershootInterpolator(2f)).start()
        listOf(b.brand to 210L, b.tag to 300L, b.motto to 360L).forEach { (v, delay) ->
            v.alpha = 0f
            v.animate().alpha(1f).setStartDelay(delay).setDuration(600).start()
        }
        b.ring1.kitRingOut(0)
        b.ring2.kitRingOut(800)
        b.tileShine.kitShine(b.tile, 2400)
        b.tile.clipToOutline = true
        // The whole logo bobs, so the sticker's hard shadow moves with it.
        ObjectAnimator.ofFloat(b.logo, View.TRANSLATION_Y, 0f, -resources.getDimension(SdpR.dimen._6sdp)).apply {
            duration = 1500
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }

        val progress = ObjectAnimator.ofInt(b.progress, "progress", 0, 90).apply {
            duration = ToolProfileKeeper.current.value.base.splashWaitSec * 1000L + CONFIG_WAIT_MS / 2
            addUpdateListener { b.percent.text = getString(R.string.splash_percent, it.animatedValue as Int) }
            start()
        }
        // Reference startup order: consent → preload → hold → config → splash ad → route.
        lifecycleScope.launch {
            // Offline: the No internet sheet is up ([NoOnline]); start once the connection is back, with a fresh config fetch.
            if (ToolProfileKeeper.current.value.base.noInternetDialog && !NoOnline.online.value) {
                NoOnline.online.first { it }
                ToolProfileKeeper.alignIfNeverFetched(this@BootActivity)
            }
            val consent = CompletableDeferred<Unit>()
            SpotKit.requestAgree(this@BootActivity) {
                SpotKit.preload(this@BootActivity)
                consent.complete(Unit)
            }
            delay(ToolProfileKeeper.current.value.base.splashWaitSec * 1000L)
            withTimeoutOrNull(CONFIG_WAIT_MS) { ToolProfileKeeper.ready.first { it } }
            consent.await()
            // Organic / paid verdict and IP location (≤3 s), then fixed for this run; later verdicts apply next launch.
            withTimeoutOrNull(ATTRIBUTION_WAIT_MS) {
                DeploySource.await(ATTRIBUTION_WAIT_MS)
                SpecialRegions.await(ATTRIBUTION_WAIT_MS)
            }
            DeploySource.lock()
            SpecialRegions.lock()
            suspendCancellableCoroutine { done -> Grants.queryFor(this@BootActivity, "splash") { done.resume(Unit) } }
            progress.cancel()
            b.progress.setProgressCompat(100, true)
            b.percent.text = getString(R.string.splash_percent, 100)
            Spots.exit(this@BootActivity, Spots.SPLASH) { Views.outBy("next"); route() }
        }
    }

    override fun onResume() {
        super.onResume()
        Views.show("splash")
    }

    /** Design "ringOut": the outline grows from 0.8× to 1.5× while fading out, every 2 s. */
    private fun View.kitRingOut(delayMs: Long) {
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
