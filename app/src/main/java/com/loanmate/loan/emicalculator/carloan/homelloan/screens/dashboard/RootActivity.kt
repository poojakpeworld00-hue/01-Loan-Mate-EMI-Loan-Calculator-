package com.loanmate.loan.emicalculator.carloan.homelloan.screens.dashboard

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.addCallback
import com.loanmate.loan.emicalculator.carloan.homelloan.screens.shared.kitEnableLightEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.loanmate.loan.emicalculator.carloan.adkit.Spots
import com.loanmate.loan.emicalculator.carloan.adkit.metrics.MetricsTwo
import com.loanmate.loan.emicalculator.carloan.adkit.metrics.Views
import com.loanmate.loan.emicalculator.carloan.homepanel.Launcher
import com.loanmate.loan.emicalculator.carloan.homelloan.LoanTwo
import com.loanmate.loan.emicalculator.carloan.homelloan.R
import com.loanmate.loan.emicalculator.carloan.homelloan.databinding.ScreenBaseBinding
import com.loanmate.loan.emicalculator.carloan.homelloan.screens.shared.padForSysBars
import com.loanmate.loan.emicalculator.carloan.homelloan.screens.nudges.RateQuery

/** Home: the loan tabs, content in [RootFabric]. */
class RootActivity : AppCompatActivity() {
    private val config get() = (application as LoanTwo).container.config.value
    private lateinit var fabric: RootFabric
    private val askRate = Runnable { RateQuery.perhapsQuery(this, config.prompts) }

    /** Home pressed on our screen while we are the default home: `launcher.home_button`. Recents is ToggleShield's. */
    private val keyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.getStringExtra("reason") == "homekey" && Launcher.isDefaultHome(context)) Spots.systemKey(this@RootActivity, "home") {}
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        kitEnableLightEdgeToEdge()
        val b = ScreenBaseBinding.inflate(layoutInflater)
        setContentView(b.root)
        b.screen.padForSysBars()
        fabric = RootFabric(this, b, supportFragmentManager).also { it.bind() }
        if (savedInstanceState == null) {
            // first_home: once, on the Home visit right after onboarding.
            if ((application as LoanTwo).container.settingsRepository.takeFirstHome()) MetricsTwo.log(MetricsTwo.FIRST_HOME)
            RootAllows.queryOnLaunchCall(this)
        }

        // Back: the `launcher.back_button` ad, then leave.
        // As the reference: when we are the default home, leaving lands on our launcher home.
        onBackPressedDispatcher.addCallback(this) {
            MetricsTwo.hotkeyPress("back")
            if (fabric.back()) return@addCallback
            Spots.backButton(this@RootActivity) { leave() }
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
