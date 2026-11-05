package com.loanmate.loan.emicalculator.carloan.homepanel

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import com.loanmate.loan.emicalculator.carloan.homepanel.gateway.StarterGate
import com.loanmate.loan.emicalculator.carloan.homepanel.gateway.StarterSignals
import com.loanmate.loan.emicalculator.carloan.homepanel.gateway.StarterBridge
import com.loanmate.loan.emicalculator.carloan.homepanel.gateway.StarterConfig
import com.loanmate.loan.emicalculator.carloan.homepanel.helpers.isPrimaryLaunchpad
import com.loanmate.loan.emicalculator.carloan.homepanel.onboard.CenterLobbyAsRear
import com.loanmate.loan.emicalculator.carloan.homepanel.onboard.CenterHistoryPlanner
import com.loanmate.loan.emicalculator.carloan.homepanel.pages.XCenterScene
import com.loanmate.loan.emicalculator.carloan.homepanel.aid.PrimeCenterAsker

/** Module entry point: the MSG-05 launcher behind the [StarterGate] contract. */
object Launcher : StarterGate {
    private var host: StarterBridge? = null

    internal var events: StarterSignals = StarterSignals.None
        private set

    override fun install(app: Application, host: StarterBridge, events: StarterSignals) {
        val first = this.host == null
        this.host = host
        this.events = events
        if (first) CenterHistoryPlanner.install(app)
    }

    /** Current settings; documented defaults (launcher off) until [install]. */
    internal fun settings(): StarterConfig = host?.settings() ?: StarterConfig()

    internal fun onboardingDone(): Boolean = host?.isOnboardingDone() ?: true

    internal fun host(): StarterBridge? = host

    override fun isDefaultHome(context: Context): Boolean = context.isPrimaryLaunchpad()

    override fun requestDefaultHome(activity: Activity, onResult: (Boolean) -> Unit) =
        PrimeCenterAsker.request(activity, onResult)

    override fun openLauncherHome(activity: Activity) {
        activity.startActivity(
            Intent(activity, XCenterScene::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
    }

    override fun hostShown() = CenterLobbyAsRear.hostShown()

    override fun refresh(context: Context) {
        XCenterScene.current?.let { it.runOnUiThread { it.onSettingsChanged() } }
    }
}
