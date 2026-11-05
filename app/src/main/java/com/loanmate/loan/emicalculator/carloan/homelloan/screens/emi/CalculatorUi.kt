package com.loanmate.loan.emicalculator.carloan.homelloan.screens.emi

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.loanmate.loan.emicalculator.carloan.homelloan.R
import com.loanmate.loan.emicalculator.carloan.homelloan.logic.entity.ComputeResult
import com.loanmate.loan.emicalculator.carloan.homelloan.logic.entity.CalculatorOrder
import java.util.Locale

@get:StringRes
val CalculatorOrder.title: Int
    get() = when (this) {
        CalculatorOrder.DIAMONDS_TO_DOLLARS -> R.string.calc_dollars
        CalculatorOrder.ELITE_PASS -> R.string.calc_elite_pass
        CalculatorOrder.DOLLARS_TO_DIAMONDS -> R.string.calc_diamonds
        CalculatorOrder.DAILY -> R.string.calc_daily
    }

@get:StringRes
val CalculatorOrder.inputHint: Int
    get() = when (this) {
        CalculatorOrder.DIAMONDS_TO_DOLLARS -> R.string.calc_input_diamonds
        CalculatorOrder.ELITE_PASS -> R.string.calc_input_passes
        CalculatorOrder.DOLLARS_TO_DIAMONDS -> R.string.calc_input_dollars
        CalculatorOrder.DAILY -> R.string.calc_input_days
    }

@get:DrawableRes
val CalculatorOrder.image: Int
    get() = when (this) {
        CalculatorOrder.DIAMONDS_TO_DOLLARS -> R.drawable.cal1
        CalculatorOrder.ELITE_PASS -> R.drawable.cal5
        CalculatorOrder.DOLLARS_TO_DIAMONDS -> R.drawable.cal7
        CalculatorOrder.DAILY -> R.drawable.cal6
    }

fun ComputeResult.format(context: Context): String = when (this) {
    is ComputeResult.PackCoins -> String.format(Locale.US, "$%.2f", value)
    is ComputeResult.PackGems -> context.getString(R.string.calc_diamonds_result, value)
}
