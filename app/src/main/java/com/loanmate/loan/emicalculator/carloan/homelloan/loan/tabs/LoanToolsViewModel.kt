package com.loanmate.loan.emicalculator.carloan.homelloan.loan.tabs

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.loanmate.loan.emicalculator.carloan.homelloan.loan.data.ToolEntry
import com.loanmate.loan.emicalculator.carloan.homelloan.loan.data.ToolsRepo

class LoanToolsViewModel(
    private val repository: ToolsRepo
) : ViewModel() {

    val tools: LiveData<List<ToolEntry>> = MutableLiveData(repository.getAll())
}
