package com.fffskintools.skin.tools.emotes.presentation.splash

import androidx.lifecycle.ViewModel
import com.fffskintools.skin.tools.emotes.domain.repository.OptionsDepot

class BootViewForm(private val settings: OptionsDepot) : ViewModel() {
    enum class KitDestination { ONBOARDING, HOME }

    fun destination(): KitDestination = if (settings.isOnboarded()) KitDestination.HOME else KitDestination.ONBOARDING
}
