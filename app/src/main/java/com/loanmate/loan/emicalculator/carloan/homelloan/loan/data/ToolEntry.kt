package com.loanmate.loan.emicalculator.carloan.homelloan.loan.data

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

data class ToolEntry(
    val id: String,
    @StringRes val titleRes: Int,
    @StringRes val subtitleRes: Int,
    @DrawableRes val iconRes: Int,
    @DrawableRes val backgroundRes: Int,
    @ColorRes val foregroundColorRes: Int
)
