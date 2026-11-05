package com.loanmate.loan.emicalculator.carloan.homelloan.loan.currency

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.loanmate.loan.emicalculator.carloan.homelloan.loan.data.CurrencyEntry
import com.loanmate.loan.emicalculator.carloan.homelloan.loan.data.CurrencyRepo

class LoanCurrencyViewModel(
    private val repository: CurrencyRepo
) : ViewModel() {

    val currencies: List<CurrencyEntry> = repository.getAll()

    private val _selectedIndex = MutableLiveData<Int>().apply {
        value = currencies.indexOfFirst { it.code == repository.getSavedCode() }
            .coerceAtLeast(0)
    }
    val selectedIndex: LiveData<Int> = _selectedIndex

    fun select(position: Int) {
        if (position !in currencies.indices) return
        if (_selectedIndex.value == position) return
        _selectedIndex.value = position
    }

    fun confirmSelection() {
        val index = _selectedIndex.value ?: 0
        repository.save(currencies[index].code)
    }
}
