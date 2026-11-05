package com.loanmate.loan.emicalculator.carloan.homelloan.launcherhost

import android.app.Activity
import android.content.Context
import androidx.fragment.app.Fragment
import com.loanmate.loan.emicalculator.carloan.homelloan.launcher.api.LauncherHost
import com.loanmate.loan.emicalculator.carloan.homelloan.launcher.api.LauncherSettings
import com.loanmate.loan.emicalculator.carloan.homelloan.core.system.MainViews
import com.loanmate.loan.emicalculator.carloan.homelloan.core.system.SwitcherGuard
import com.loanmate.loan.emicalculator.carloan.homelloan.data.remote.config.ToolProfileKeeper
import com.loanmate.loan.emicalculator.carloan.homelloan.data.remote.config.HotkeyTask
import com.loanmate.loan.emicalculator.carloan.homelloan.data.remote.config.OsHotkey
import com.loanmate.loan.emicalculator.carloan.homelloan.presentation.onboarding.BaseBasePhase

class ToolDeckParent(context: Context) : LauncherHost {
    private val app = context.applicationContext

    // Remote Config decides what exists; hiding our icon turns it off.
    override fun settings(): LauncherSettings = ToolProfileKeeper.current.value.launcher.let { rc ->
        val hidden = SelfBadge.hidden(app)
        rc.copy(
            iconInDrawer = rc.iconInDrawer && !hidden, iconOnHome = rc.iconOnHome && !hidden,
            // uninstall_flow.flow: premium offers Uninstall on our icon; normal offers only Hide.
            hostUninstall = ToolProfileKeeper.current.value.uninstallFlow.premium,
        )
    }

    override fun isOnboardingDone() = MainViews.onboardingDone(app)

    override fun createHostPanel(): Fragment = ParentBoardFragment()

    // The launcher calls this when HOME is pressed before onboarding finished.
    override fun openHostApp(activity: Activity) {
        // Onboarding gets Home only for a real Home press: MIUI also sends Home when its Recents screen closes.
        val home = !isOnboardingDone() && SwitcherGuard.baseHotkeyJustPressed()
        if (!isOnboardingDone() && !home) SwitcherGuard.handPriorWithoutHotkey()
        MainViews.open(activity, OsHotkey.HOME.key.takeIf { home })
    }

    // system_keys.home "" = device default: Home shows the launcher home. The default-home step still gets it (picking us sends Home).
    override fun takesHome() =
        ToolProfileKeeper.current.value.systemKeys.home != HotkeyTask.DEFAULT || BaseBasePhase.shown || SwitcherGuard.resuming
}
