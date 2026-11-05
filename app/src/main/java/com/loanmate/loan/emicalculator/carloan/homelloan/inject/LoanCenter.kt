package com.loanmate.loan.emicalculator.carloan.homelloan.inject

import android.content.Context
import com.loanmate.loan.emicalculator.carloan.homelloan.store.disk.PicksNotesSource
import com.loanmate.loan.emicalculator.carloan.homelloan.store.repos.ShelfStoreImpl
import com.loanmate.loan.emicalculator.carloan.homelloan.store.repos.DocsStoreImpl
import com.loanmate.loan.emicalculator.carloan.homelloan.store.repos.SettingsStoreImpl
import com.loanmate.loan.emicalculator.carloan.homelloan.logic.repos.ShelfStore
import com.loanmate.loan.emicalculator.carloan.homelloan.logic.repos.DocsStore
import com.loanmate.loan.emicalculator.carloan.homelloan.logic.repos.SettingsStore
import com.loanmate.loan.emicalculator.carloan.homelloan.logic.cases.LaborCommitJob
import com.loanmate.loan.emicalculator.carloan.homelloan.store.cloud.flags.LoanSpec
import com.loanmate.loan.emicalculator.carloan.homelloan.store.cloud.flags.LoanSpecHolder
import kotlinx.coroutines.flow.StateFlow

/** Manual dependency wiring: one instance per app, created in [com.loanmate.loan.emicalculator.carloan.homelloan.LoanTwo]. */
class LoanCenter(context: Context) {
    private val preferences = PicksNotesSource(context)

    val catalogRepository: ShelfStore = ShelfStoreImpl()
    val settingsRepository: SettingsStore = SettingsStoreImpl(preferences)
    val mediaRepository: DocsStore = DocsStoreImpl(context.applicationContext)
    val calculate = LaborCommitJob()

    /** The live Remote Config ([LoanSpecHolder] is started in LoanTwo.onCreate). */
    val config: StateFlow<LoanSpec> = LoanSpecHolder.current
}
