package com.loanmate.loan.emicalculator.carloan.homelloan.loan.tabs

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.loanmate.loan.emicalculator.carloan.homelloan.loan.data.HistoryEntry
import com.loanmate.loan.emicalculator.carloan.homelloan.loan.data.HistoryRepo

class LoanHistoryViewModel(
    private val repository: HistoryRepo
) : ViewModel() {

    private val _items = MutableLiveData<List<HistoryEntry>>(emptyList())
    val items: LiveData<List<HistoryEntry>> = _items

    /** Re-read from prefs on each onResume (other screens may have added entries). */
    fun refresh() {
        _items.value = repository.getAll()
    }

    fun delete(id: String) {
        repository.delete(id)
        refresh()
    }
}
