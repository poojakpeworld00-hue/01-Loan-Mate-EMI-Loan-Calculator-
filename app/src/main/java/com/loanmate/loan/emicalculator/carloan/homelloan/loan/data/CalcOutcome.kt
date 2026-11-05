package com.loanmate.loan.emicalculator.carloan.homelloan.loan.data

/**
 * Output of a single calculator run. [interestPct] is in 0..100 (or 0..0 if N/A).
 * The headline value is what shows in the big number under the Summary card —
 * e.g. monthly EMI for loans, maturity amount for deposits.
 */
data class CalcOutcome(
    val headlineValue: Double,
    val principal: Double,
    val interest: Double,
    val total: Double,
    val interestPct: Float
) {
    companion object {
        val EMPTY = CalcOutcome(0.0, 0.0, 0.0, 0.0, 0f)
    }
}
