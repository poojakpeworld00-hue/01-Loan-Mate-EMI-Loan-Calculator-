package com.loanmate.loan.emicalculator.carloan.homelloan.homebridge

import android.app.Application
import com.loanmate.loan.emicalculator.carloan.homepanel.Launcher
import com.loanmate.loan.emicalculator.carloan.homelloan.store.cloud.flags.LoanSpecHolder
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Hands the app's host + events to the launcher module. Call once from LoanTwo.onCreate. */
object PackBoot {
    private var installed = false

    fun install(app: Application) {
        if (installed) return
        installed = true
        Launcher.install(app, LoanPackHost(app), LoanPackSignals())
        MainScope().launch {
            // drop(1): install() already read the current values.
            LoanSpecHolder.current.map { it.launcher }.distinctUntilChanged().drop(1).collect { Launcher.refresh(app) }
        }
    }
}
