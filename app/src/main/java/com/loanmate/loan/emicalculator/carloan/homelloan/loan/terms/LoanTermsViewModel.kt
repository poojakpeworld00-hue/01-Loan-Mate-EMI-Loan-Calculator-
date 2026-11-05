package com.loanmate.loan.emicalculator.carloan.homelloan.loan.terms

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.loanmate.loan.emicalculator.carloan.homelloan.loan.data.TermsRepo

class LoanTermsViewModel(
    private val repository: TermsRepo
) : ViewModel() {

    private val _agreed = MutableLiveData(false)
    val agreed: LiveData<Boolean> = _agreed

    fun setAgreed(checked: Boolean) {
        if (_agreed.value == checked) return
        _agreed.value = checked
    }

    /** Returns true when the user has agreed and the change has been persisted. */
    fun confirmAcceptance(): Boolean {
        if (_agreed.value != true) return false
        repository.accept()
        return true
    }
}
