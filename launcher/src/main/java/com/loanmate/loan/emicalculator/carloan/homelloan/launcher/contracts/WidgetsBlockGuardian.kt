package com.loanmate.loan.emicalculator.carloan.homelloan.launcher.contracts

import com.loanmate.loan.emicalculator.carloan.homelloan.launcher.entities.GadgetWidget

interface WidgetsBlockGuardian {
    fun onWidgetLongPressed(appWidget: GadgetWidget)
}
