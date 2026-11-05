package com.loanmate.loan.emicalculator.carloan.homelloan.loan

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.loanmate.loan.emicalculator.carloan.homelloan.LoanTwo
import com.loanmate.loan.emicalculator.carloan.homelloan.loan.currency.LoanCurrencyViewModel
import com.loanmate.loan.emicalculator.carloan.homelloan.loan.data.CurrencyRepo
import com.loanmate.loan.emicalculator.carloan.homelloan.loan.data.HistoryRepo
import com.loanmate.loan.emicalculator.carloan.homelloan.loan.data.HomeRepo
import com.loanmate.loan.emicalculator.carloan.homelloan.loan.data.LoanPrefs
import com.loanmate.loan.emicalculator.carloan.homelloan.loan.data.TermsRepo
import com.loanmate.loan.emicalculator.carloan.homelloan.loan.data.ToolsRepo
import com.loanmate.loan.emicalculator.carloan.homelloan.loan.tabs.LoanHistoryViewModel
import com.loanmate.loan.emicalculator.carloan.homelloan.loan.tabs.LoanHomeViewModel
import com.loanmate.loan.emicalculator.carloan.homelloan.loan.tabs.LoanSettingsViewModel
import com.loanmate.loan.emicalculator.carloan.homelloan.loan.tabs.LoanToolsViewModel
import com.loanmate.loan.emicalculator.carloan.homelloan.loan.terms.LoanTermsViewModel

/**
 * Single factory that resolves repositories from a [LoanPrefs] derived from
 * the application context, so no Activity reference leaks into a ViewModel.
 */
class LoanViewModelFactory(context: Context) : ViewModelProvider.Factory {

    private val appContext = context.applicationContext
    private val pref = LoanPrefs(appContext)

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(LoanCurrencyViewModel::class.java) ->
            LoanCurrencyViewModel(CurrencyRepo(pref)) as T

        modelClass.isAssignableFrom(LoanTermsViewModel::class.java) ->
            LoanTermsViewModel(TermsRepo(pref)) as T

        modelClass.isAssignableFrom(LoanHomeViewModel::class.java) ->
            LoanHomeViewModel(HomeRepo()) as T

        modelClass.isAssignableFrom(LoanToolsViewModel::class.java) ->
            LoanToolsViewModel(ToolsRepo()) as T

        modelClass.isAssignableFrom(LoanSettingsViewModel::class.java) ->
            LoanSettingsViewModel((appContext as LoanTwo).container.settingsRepository, CurrencyRepo(pref)) as T

        modelClass.isAssignableFrom(LoanHistoryViewModel::class.java) ->
            LoanHistoryViewModel(HistoryRepo(pref)) as T

        else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
    }
}
