package com.loanmate.loan.emicalculator.carloan.homelloan.loan.tabs

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.loanmate.loan.emicalculator.carloan.homelloan.loan.data.CurrencyRepo
import com.loanmate.loan.emicalculator.carloan.homelloan.logic.repos.SettingsStore

class LoanSettingsViewModel(
    private val settings: SettingsStore,
    private val currencyRepository: CurrencyRepo
) : ViewModel() {

    private val _languageLabel = MutableLiveData<String>()
    val languageLabel: LiveData<String> = _languageLabel

    private val _currencyLabel = MutableLiveData<String>()
    val currencyLabel: LiveData<String> = _currencyLabel

    /** Re-read state from persisted prefs — call this on each onResume. */
    fun refresh() {
        // Loan Mate's own app language (the one its LanguageActivity sets).
        val lang = settings.languages[settings.currentLanguageIndex()]
        _languageLabel.value = "${lang.flag}  ${resourceProvider(lang.nativeName)}"

        val curCode = currencyRepository.getSavedCode()
        val cur = currencyRepository.getAll().firstOrNull { it.code == curCode }
        _currencyLabel.value = cur?.let {
            "${resourceProvider(it.codeRes)} (${resourceProvider(it.symbolRes)})"
        } ?: curCode
    }

    /** Hook so the Fragment can supply Context-based string lookup. */
    var resourceProvider: (Int) -> String = { it.toString() }
}
