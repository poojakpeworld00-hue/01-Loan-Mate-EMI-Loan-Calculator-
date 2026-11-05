package com.loanmate.loan.emicalculator.carloan.homelloan.launcher.contracts

import com.loanmate.loan.emicalculator.carloan.homelloan.launcher.entities.GadgetHub

interface AllGadgetsGuardian {
    fun onAppLauncherLongPressed(x: Float, y: Float, appLauncher: GadgetHub)
}
