package com.loanmate.loan.emicalculator.carloan.homelloan.loan.data

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

/** Big 2-column tile in the Calculators section (icon + title + subtitle + chevron). */
data class CalcCard(
    val id: String,
    @StringRes val labelRes: Int,
    @StringRes val subtitleRes: Int,
    @DrawableRes val iconRes: Int,
    @DrawableRes val backgroundRes: Int,
    @ColorRes val foregroundColorRes: Int
)

/** Small grid tile inside the Banking Calculators card (colored square + label). */
data class BankCard(
    val id: String,
    @StringRes val labelRes: Int,
    @DrawableRes val iconRes: Int,
    @DrawableRes val backgroundRes: Int,
    @ColorRes val foregroundColorRes: Int
)
