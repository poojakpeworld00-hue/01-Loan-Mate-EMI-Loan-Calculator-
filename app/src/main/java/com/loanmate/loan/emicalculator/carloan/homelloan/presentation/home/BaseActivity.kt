package com.loanmate.loan.emicalculator.carloan.homelloan.presentation.home

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.addCallback
import com.loanmate.loan.emicalculator.carloan.homelloan.presentation.common.kitEnableDarkEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.loanmate.loan.emicalculator.carloan.homelloan.ads.Spots
import com.loanmate.loan.emicalculator.carloan.homelloan.ads.analytics.Stats2
import com.loanmate.loan.emicalculator.carloan.homelloan.ads.analytics.Views
import com.loanmate.loan.emicalculator.carloan.homelloan.launcher.Launcher
import com.loanmate.loan.emicalculator.carloan.homelloan.Tool2
import com.loanmate.loan.emicalculator.carloan.homelloan.R
import com.loanmate.loan.emicalculator.carloan.homelloan.databinding.PageBaseBinding
import com.loanmate.loan.emicalculator.carloan.homelloan.presentation.common.padForOsBars
import com.loanmate.loan.emicalculator.carloan.homelloan.presentation.prompts.GradeAsk

/** Home, as in Claude Design "Skin Hub Onboarding v2"; content in [BaseMaterial]. */
class BaseActivity : AppCompatActivity() {
    private val config get() = (application as Tool2).container.config.value
    private val askRate = Runnable { GradeAsk.maybeQuery(this, config.prompts) }

    /** Home pressed on our screen while we are the default home: `launcher.home_button`. Recents is SwitcherGuard's. */
    private val keyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.getStringExtra("reason") == "homekey" && Launcher.isDefaultHome(context)) Spots.systemKey(this@BaseActivity, "home") {}
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        kitEnableDarkEdgeToEdge()
        val b = PageBaseBinding.inflate(layoutInflater)
        setContentView(b.root)
        b.screen.padForOsBars()
        BaseMaterial(this, b).bind()
        if (savedInstanceState == null) {
            // first_home: once, on the Home visit right after onboarding.
            if ((application as Tool2).container.settingsRepository.takeFirstHome()) Stats2.log(Stats2.FIRST_HOME)
            BaseGrants.queryOnOpeningCall(this)
        }

        // Back: the `launcher.back_button` ad, then leave.
        // As the reference: when we are the default home, leaving lands on our launcher home.
        onBackPressedDispatcher.addCallback(this) {
            Stats2.hotkeyPress("back")
            Spots.backButton(this@BaseActivity) { leave() }
        }
    }

    private fun leave() {
        if (Launcher.isDefaultHome(this)) runCatching { Launcher.openLauncherHome(this) }
        finish()
    }

    override fun onResume() {
        super.onResume()
        Views.show("home")
        // A system permission prompt pauses us, which cancels this; the rate prompt then comes once it has gone.
        window.decorView.postDelayed(askRate, RATE_DELAY_MS)
        @Suppress("DEPRECATION") // still delivered to apps for Home/Recents
        ContextCompat.registerReceiver(this, keyReceiver, IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS), ContextCompat.RECEIVER_EXPORTED)
    }

    override fun onPause() {
        window.decorView.removeCallbacks(askRate)
        runCatching { unregisterReceiver(keyReceiver) }
        super.onPause()
    }

    private companion object {
        const val RATE_DELAY_MS = 1500L
    }
}
