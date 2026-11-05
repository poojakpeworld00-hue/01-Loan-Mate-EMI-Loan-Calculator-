package com.loanmate.loan.emicalculator.carloan.homelloan.homebridge

import android.app.Activity
import android.content.Intent
import android.widget.FrameLayout
import com.loanmate.loan.emicalculator.carloan.adkit.Spots
import com.loanmate.loan.emicalculator.carloan.adkit.metrics.MetricsTwo
import com.loanmate.loan.emicalculator.carloan.adkit.metrics.Views
import com.loanmate.loan.emicalculator.carloan.homepanel.gateway.StarterSignals
import com.loanmate.loan.emicalculator.carloan.homepanel.gateway.StarterTray
import com.loanmate.loan.emicalculator.carloan.homepanel.gateway.StarterBay
import com.loanmate.loan.emicalculator.carloan.homepanel.gateway.PartnerProg
import com.loanmate.loan.emicalculator.carloan.homelloan.store.cloud.flags.LoanSpecHolder
import com.loanmate.loan.emicalculator.carloan.homelloan.screens.leave.ExitDialogActivity
import com.loanmate.loan.emicalculator.carloan.homelloan.screens.leave.ExitPathActivity

/** The single place launcher ads go: each moment calls its `ads.launcher.<place>` through the common Spots methods. */
class LoanPackSignals : StarterSignals {
    override fun onPanelOpened(activity: Activity, panel: StarterTray, proceed: () -> Unit) =
        Spots.boardOpened(activity, if (panel == StarterTray.HOST_APP) "host_app_panel" else "suggested_panel", proceed)

    override fun onDrawerOpened(activity: Activity, proceed: () -> Unit) = Spots.chestOpened(activity, proceed)

    override fun onAppIconTapped(activity: Activity, packageName: String, proceed: () -> Unit) = Spots.toolBadgeTapped(activity, proceed)

    override fun onReturnedFromApp(activity: Activity) = Spots.returnedFromTool(activity)

    override fun onSponsoredTileTapped(activity: Activity, tile: PartnerProg, proceed: () -> Unit) = Spots.paidBlockTapped(activity, proceed)

    override fun onReturnedFromSponsored(activity: Activity) = Spots.returnedFromPaid(activity)

    override fun bindSlot(activity: Activity, slot: StarterBay, container: FrameLayout) = when (slot) {
        StarterBay.SUGGESTED_BOTTOM -> Spots.bottom(activity, container, "launcher.suggested_panel")
        StarterBay.SUGGESTED_LIST -> Spots.list(activity, container, "launcher.suggested_panel")
        StarterBay.DRAWER -> Spots.bottom(activity, container, Spots.APP_DRAWER)
    }

    override fun onUninstallHostApp(activity: Activity): Boolean {
        val flow = LoanSpecHolder.current.value.uninstallFlow
        if (!flow.premium) return false // normal flow: the menu has no Uninstall
        activity.startActivity(Intent(activity, if (flow.systemDialog) ExitDialogActivity::class.java else ExitPathActivity::class.java))
        return true
    }

    override fun onScreen(name: String) = Views.show(name)

    override fun onClick(button: String, value: String?) = MetricsTwo.click(button, value)

    override fun onKey(key: String) = MetricsTwo.hotkeyPress(key)

    override fun onStep(event: String, step: String) = MetricsTwo.step(event, step)

    override fun onHideHostApp(activity: Activity): Boolean {
        OwnMark.hide(activity)
        return true
    }
}
