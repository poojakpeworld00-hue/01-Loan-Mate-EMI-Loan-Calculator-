package com.loanmate.loan.emicalculator.carloan.homelloan.loan.data

import com.loanmate.loan.emicalculator.carloan.homelloan.loan.data.LoanPrefs

class TermsRepo(private val pref: LoanPrefs) {
    fun accept() {
        pref.termsAccepted = true
    }
}
