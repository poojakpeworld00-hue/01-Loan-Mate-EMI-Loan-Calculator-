package com.loanmate.loan.emicalculator.carloan.homelloan.logic.cases

import com.loanmate.loan.emicalculator.carloan.homelloan.logic.entity.ComputeResult
import com.loanmate.loan.emicalculator.carloan.homelloan.logic.entity.CalculatorOrder

/** Validates the typed amount and runs a diamond calculator. */
class LaborCommitJob {
    sealed interface PackResult {
        data object PackBlank : PackResult
        data object PackBroken : PackResult
        data class PackWin(val result: ComputeResult) : PackResult
    }

    operator fun invoke(type: CalculatorOrder, input: String): PackResult {
        val text = input.trim()
        if (text.isEmpty()) return PackResult.PackBlank
        val amount = text.toDoubleOrNull()
        if (amount == null || amount.isNaN() || amount.isInfinite() || amount < 0) return PackResult.PackBroken
        return PackResult.PackWin(compute(type, amount))
    }

    fun compute(type: CalculatorOrder, amount: Double): ComputeResult =
        if (type.toDollars) ComputeResult.PackCoins(amount / type.factor)
        else ComputeResult.PackGems((amount * type.factor).toLong())
}
