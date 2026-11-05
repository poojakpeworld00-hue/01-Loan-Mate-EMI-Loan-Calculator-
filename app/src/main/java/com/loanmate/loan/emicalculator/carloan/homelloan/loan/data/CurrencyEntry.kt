package com.loanmate.loan.emicalculator.carloan.homelloan.loan.data

import androidx.annotation.StringRes

data class CurrencyEntry(
    val code: String,
    @StringRes val codeRes: Int,
    @StringRes val symbolRes: Int
)
