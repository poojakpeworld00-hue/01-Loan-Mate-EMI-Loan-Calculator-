package com.loanmate.loan.emicalculator.carloan.homelloan.logic.entity

/** The reference app's four diamond calculators: result = amount / factor (dollars) or amount * factor (diamonds). */
enum class CalculatorOrder(val toDollars: Boolean, val factor: Int) {
    DIAMONDS_TO_DOLLARS(true, DIAMONDS_PER_DOLLAR),
    ELITE_PASS(false, 499),
    DOLLARS_TO_DIAMONDS(false, DIAMONDS_PER_DOLLAR),
    DAILY(false, 10),
}

private const val DIAMONDS_PER_DOLLAR = 101

sealed interface ComputeResult {
    data class PackCoins(val value: Double) : ComputeResult
    data class PackGems(val value: Long) : ComputeResult
}
