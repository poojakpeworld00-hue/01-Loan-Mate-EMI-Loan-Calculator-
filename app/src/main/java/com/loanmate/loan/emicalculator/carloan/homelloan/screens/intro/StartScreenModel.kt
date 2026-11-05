package com.loanmate.loan.emicalculator.carloan.homelloan.screens.intro

import androidx.lifecycle.ViewModel
import com.loanmate.loan.emicalculator.carloan.homelloan.logic.repos.SettingsStore

class StartScreenModel(private val settings: SettingsStore) : ViewModel() {
    enum class PackTarget { ONBOARDING, HOME }

    fun destination(): PackTarget = if (settings.isOnboarded()) PackTarget.HOME else PackTarget.ONBOARDING
}
