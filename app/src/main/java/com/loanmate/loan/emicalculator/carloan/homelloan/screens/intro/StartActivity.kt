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
import com.loanmate.loan.emicalculator.carloan.homelloan.screens.shared.kitEnableDarkEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.loanmate.loan.emicalculator.carloan.adkit.PromoPack
import com.loanmate.loan.emicalculator.carloan.adkit.Spots
import com.loanmate.loan.emicalculator.carloan.adkit.metrics.Views
import com.loanmate.loan.emicalculator.carloan.homelloan.store.cloud.flags.LoanSpecHolder
import com.loanmate.loan.emicalculator.carloan.homelloan.kit.device.NoConnected
import com.loanmate.loan.emicalculator.carloan.homelloan.databinding.ScreenBootBinding
import com.loanmate.loan.emicalculator.carloan.homelloan.screens.shared.toolViewForm
import com.loanmate.loan.emicalculator.carloan.homelloan.screens.shared.kitGlowPulse
import com.loanmate.loan.emicalculator.carloan.homelloan.screens.shared.padForOsBars
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

/** Splash, Sticker Pop "Sticker Stack": bobbing logo sticker over the art while consent, config and the splash ad run, then routes. */
class StartActivity : AppCompatActivity() {
    private val viewModel by toolViewForm { StartScreenModel(settingsRepository) }

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
        kitEnableDarkEdgeToEdge()
        // Our task was removed for Recents (ToggleShield): the app reopens where the user was, without the splash.
        if (savedInstanceState == null && ToggleShield.takeProceed()) return route(resume = true)
        if (savedInstanceState == null) RateQuery.countEnter(this)
        val b = ScreenBootBinding.inflate(layoutInflater)
        setContentView(b.root)
        b.contentVw.padForOsBars()

        b.glowVw.kitGlowPulse()
        b.bgVw.scaleX = 1.1f; b.bgVw.scaleY = 1.1f
        b.bgVw.animate().scaleX(1f).scaleY(1f).setDuration(SPLASH_MS).setInterpolator(DecelerateInterpolator()).start()

        b.logoVw.alpha = 0f; b.logoVw.scaleX = 0.6f; b.logoVw.scaleY = 0.6f
        b.logoVw.animate().alpha(1f).scaleX(1f).scaleY(1f).setStartDelay(60).setDuration(600)
            .setInterpolator(OvershootInterpolator(2f)).start()
        listOf(b.brandVw to 210L, b.tagVw to 300L, b.mottoVw to 360L).forEach { (v, delay) ->
            v.alpha = 0f
            v.animate().alpha(1f).setStartDelay(delay).setDuration(600).start()
        }
        b.ring1Vw.kitRingOut(0)
        b.ring2Vw.kitRingOut(800)
        b.tileShineVw.kitShine(b.tileVw, 2400)
        b.tileVw.clipToOutline = true
        // The whole logo bobs, so the sticker's hard shadow moves with it.
        ObjectAnimator.ofFloat(b.logoVw, View.TRANSLATION_Y, 0f, -resources.getDimension(SdpR.dimen._6sdp)).apply {
            duration = 1500
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }

        val progress = ObjectAnimator.ofInt(b.prog, "progress", 0, 90).apply {
            duration = LoanSpecHolder.current.value.base.splashWaitSec * 1000L + CONFIG_WAIT_MS / 2
            addUpdateListener { b.percentVw.text = getString(R.string.splash_percent, it.animatedValue as Int) }
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
            PromoPack.requestAgree(this@StartActivity) {
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
            b.percentVw.text = getString(R.string.splash_percent, 100)
            Spots.exit(this@StartActivity, Spots.SPLASH) { Views.outBy("next"); route() }
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
