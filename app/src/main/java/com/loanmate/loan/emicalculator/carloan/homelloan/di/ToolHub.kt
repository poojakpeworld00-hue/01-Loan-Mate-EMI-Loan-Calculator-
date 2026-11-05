package com.loanmate.loan.emicalculator.carloan.homelloan.di

import android.content.Context
import com.loanmate.loan.emicalculator.carloan.homelloan.data.local.ChoicesFactsFeed
import com.loanmate.loan.emicalculator.carloan.homelloan.data.repository.LibraryDepotImpl
import com.loanmate.loan.emicalculator.carloan.homelloan.data.repository.FilesDepotImpl
import com.loanmate.loan.emicalculator.carloan.homelloan.data.repository.OptionsDepotImpl
import com.loanmate.loan.emicalculator.carloan.homelloan.domain.repository.LibraryDepot
import com.loanmate.loan.emicalculator.carloan.homelloan.domain.repository.FilesDepot
import com.loanmate.loan.emicalculator.carloan.homelloan.domain.repository.OptionsDepot
import com.loanmate.loan.emicalculator.carloan.homelloan.domain.usecase.WorkApplyTask
import com.loanmate.loan.emicalculator.carloan.homelloan.data.remote.config.ToolProfile
import com.loanmate.loan.emicalculator.carloan.homelloan.data.remote.config.ToolProfileKeeper
import kotlinx.coroutines.flow.StateFlow

/** Manual dependency wiring: one instance per app, created in [com.loanmate.loan.emicalculator.carloan.homelloan.Tool2]. */
class ToolHub(context: Context) {
    private val preferences = ChoicesFactsFeed(context)

    val catalogRepository: LibraryDepot = LibraryDepotImpl()
    val settingsRepository: OptionsDepot = OptionsDepotImpl(preferences)
    val mediaRepository: FilesDepot = FilesDepotImpl(context.applicationContext)
    val calculate = WorkApplyTask()

    /** The live Remote Config ([ToolProfileKeeper] is started in Tool2.onCreate). */
    val config: StateFlow<ToolProfile> = ToolProfileKeeper.current
}
