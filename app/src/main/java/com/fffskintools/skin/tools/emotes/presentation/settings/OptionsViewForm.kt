package com.fffskintools.skin.tools.emotes.presentation.settings

import androidx.lifecycle.ViewModel
import com.fffskintools.skin.tools.emotes.domain.model.Tongue
import com.fffskintools.skin.tools.emotes.domain.repository.OptionsDepot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class OptionsUiMode(val language: Tongue)

class OptionsViewForm(private val settings: OptionsDepot) : ViewModel() {
    private val _state = MutableStateFlow(read())
    val state: StateFlow<OptionsUiMode> = _state

    /** Re-reads settings, e.g. after returning from the language screen. */
    fun refresh() {
        _state.value = read()
    }

    private fun read() = OptionsUiMode(settings.languages[settings.currentLanguageIndex()])
}
