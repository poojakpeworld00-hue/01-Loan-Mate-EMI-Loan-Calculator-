package com.loanmate.loan.emicalculator.carloan.homelloan.loan.tabs

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.loanmate.loan.emicalculator.carloan.homelloan.loan.data.BankCard
import com.loanmate.loan.emicalculator.carloan.homelloan.loan.data.CalcCard
import com.loanmate.loan.emicalculator.carloan.homelloan.loan.data.HomeRepo

class LoanHomeViewModel(
    private val repository: HomeRepo
) : ViewModel() {

    val calcTiles: LiveData<List<CalcCard>> = MutableLiveData(repository.getCalcTiles())
    val bankingTiles: LiveData<List<BankCard>> = MutableLiveData(repository.getBankingTiles())
}
