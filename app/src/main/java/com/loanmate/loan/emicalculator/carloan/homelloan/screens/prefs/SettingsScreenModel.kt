package com.loanmate.loan.emicalculator.carloan.homelloan.screens.prefs

import androidx.lifecycle.ViewModel
import com.loanmate.loan.emicalculator.carloan.homelloan.logic.entity.Tongue
import com.loanmate.loan.emicalculator.carloan.homelloan.logic.repos.SettingsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class SettingsLookStyle(val language: Tongue)

class SettingsScreenModel(private val settings: SettingsStore) : ViewModel() {
    private val _state = MutableStateFlow(read())
    val state: StateFlow<SettingsLookStyle> = _state

    /** Re-reads settings, e.g. after returning from the language screen. */
    fun refresh() {
        _state.value = read()
    }

    private fun read() = SettingsLookStyle(settings.languages[settings.currentLanguageIndex()])
}
