package com.loanmate.loan.emicalculator.carloan.homelloan.presentation.splash

import androidx.lifecycle.ViewModel
import com.loanmate.loan.emicalculator.carloan.homelloan.domain.repository.OptionsDepot

class BootViewForm(private val settings: OptionsDepot) : ViewModel() {
    enum class KitDestination { ONBOARDING, HOME }

    fun destination(): KitDestination = if (settings.isOnboarded()) KitDestination.HOME else KitDestination.ONBOARDING
}
