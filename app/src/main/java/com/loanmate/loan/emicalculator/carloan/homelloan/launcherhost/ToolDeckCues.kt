package com.loanmate.loan.emicalculator.carloan.homelloan.launcherhost

import android.app.Activity
import android.content.Intent
import android.widget.FrameLayout
import com.loanmate.loan.emicalculator.carloan.homelloan.ads.Spots
import com.loanmate.loan.emicalculator.carloan.homelloan.ads.analytics.Stats2
import com.loanmate.loan.emicalculator.carloan.homelloan.ads.analytics.Views
import com.loanmate.loan.emicalculator.carloan.homelloan.launcher.api.LauncherEvents
import com.loanmate.loan.emicalculator.carloan.homelloan.launcher.api.LauncherPanel
import com.loanmate.loan.emicalculator.carloan.homelloan.launcher.api.LauncherSlot
import com.loanmate.loan.emicalculator.carloan.homelloan.launcher.api.SponsoredApp
import com.loanmate.loan.emicalculator.carloan.homelloan.data.remote.config.ToolProfileKeeper
import com.loanmate.loan.emicalculator.carloan.homelloan.presentation.uninstall.FarewellModalActivity
import com.loanmate.loan.emicalculator.carloan.homelloan.presentation.uninstall.FarewellRouteActivity

/** The single place launcher ads go: each moment calls its `ads.launcher.<place>` through the common Spots methods. */
class ToolDeckCues : LauncherEvents {
    override fun onPanelOpened(activity: Activity, panel: LauncherPanel, proceed: () -> Unit) =
        Spots.boardOpened(activity, if (panel == LauncherPanel.HOST_APP) "host_app_panel" else "suggested_panel", proceed)

    override fun onDrawerOpened(activity: Activity, proceed: () -> Unit) = Spots.chestOpened(activity, proceed)

    override fun onAppIconTapped(activity: Activity, packageName: String, proceed: () -> Unit) = Spots.toolBadgeTapped(activity, proceed)

    override fun onReturnedFromApp(activity: Activity) = Spots.returnedFromTool(activity)

    override fun onSponsoredTileTapped(activity: Activity, tile: SponsoredApp, proceed: () -> Unit) = Spots.paidBlockTapped(activity, proceed)

    override fun onReturnedFromSponsored(activity: Activity) = Spots.returnedFromPaid(activity)

    override fun bindSlot(activity: Activity, slot: LauncherSlot, container: FrameLayout) = when (slot) {
        LauncherSlot.SUGGESTED_BOTTOM -> Spots.bottom(activity, container, "launcher.suggested_panel")
        LauncherSlot.SUGGESTED_LIST -> Spots.list(activity, container, "launcher.suggested_panel")
        LauncherSlot.DRAWER -> Spots.bottom(activity, container, Spots.APP_DRAWER)
    }

    override fun onUninstallHostApp(activity: Activity): Boolean {
        val flow = ToolProfileKeeper.current.value.uninstallFlow
        if (!flow.premium) return false // normal flow: the menu has no Uninstall
        activity.startActivity(Intent(activity, if (flow.systemDialog) FarewellModalActivity::class.java else FarewellRouteActivity::class.java))
        return true
    }

    override fun onScreen(name: String) = Views.show(name)

    override fun onClick(button: String, value: String?) = Stats2.click(button, value)

    override fun onKey(key: String) = Stats2.hotkeyPress(key)

    override fun onStep(event: String, step: String) = Stats2.step(event, step)

    override fun onHideHostApp(activity: Activity): Boolean {
        SelfBadge.hide(activity)
        return true
    }
}
