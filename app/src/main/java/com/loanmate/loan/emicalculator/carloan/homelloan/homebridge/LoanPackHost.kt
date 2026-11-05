package com.loanmate.loan.emicalculator.carloan.homelloan.homebridge

import android.app.Activity
import android.content.Context
import androidx.fragment.app.Fragment
import com.loanmate.loan.emicalculator.carloan.homepanel.gateway.StarterBridge
import com.loanmate.loan.emicalculator.carloan.homepanel.gateway.StarterConfig
import com.loanmate.loan.emicalculator.carloan.homelloan.kit.device.PrimeScreens
import com.loanmate.loan.emicalculator.carloan.homelloan.kit.device.ToggleShield
import com.loanmate.loan.emicalculator.carloan.homelloan.store.cloud.flags.LoanSpecHolder
import com.loanmate.loan.emicalculator.carloan.homelloan.store.cloud.flags.ShortcutJob
import com.loanmate.loan.emicalculator.carloan.homelloan.store.cloud.flags.SysShortcut
import com.loanmate.loan.emicalculator.carloan.homelloan.screens.welcome.RootRootStep

class LoanPackHost(context: Context) : StarterBridge {
    private val app = context.applicationContext

    // Remote Config decides what exists; hiding our icon turns it off.
    override fun settings(): StarterConfig = LoanSpecHolder.current.value.launcher.let { rc ->
        val hidden = OwnMark.hidden(app)
        rc.copy(
            iconInDrawer = rc.iconInDrawer && !hidden, iconOnHome = rc.iconOnHome && !hidden,
            // uninstall_flow.flow: premium offers Uninstall on our icon; normal offers only Hide.
            hostUninstall = LoanSpecHolder.current.value.uninstallFlow.premium,
        )
    }

    override fun isOnboardingDone() = PrimeScreens.onboardingDone(app)

    override fun createHostPanel(): Fragment = HostSheetFragment()

    // The launcher calls this when HOME is pressed before onboarding finished.
    override fun openHostApp(activity: Activity) {
        // Onboarding gets Home only for a real Home press: MIUI also sends Home when its Recents screen closes.
        val home = !isOnboardingDone() && ToggleShield.baseShortcutJustPressed()
        if (!isOnboardingDone() && !home) ToggleShield.handPriorWithoutShortcut()
        PrimeScreens.open(activity, SysShortcut.HOME.key.takeIf { home })
    }

    // system_keys.home "" = device default: Home shows the launcher home. The default-home step still gets it (picking us sends Home).
    override fun takesHome() =
        LoanSpecHolder.current.value.systemKeys.home != ShortcutJob.DEFAULT || RootRootStep.shown || ToggleShield.resuming
}
