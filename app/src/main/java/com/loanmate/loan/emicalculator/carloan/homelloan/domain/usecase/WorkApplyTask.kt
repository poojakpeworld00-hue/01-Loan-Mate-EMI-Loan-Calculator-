package com.loanmate.loan.emicalculator.carloan.homelloan.domain.usecase

import com.loanmate.loan.emicalculator.carloan.homelloan.domain.model.ReckonYield
import com.loanmate.loan.emicalculator.carloan.homelloan.domain.model.ReckonerSort

/** Validates the typed amount and runs a diamond calculator. */
class WorkApplyTask {
    sealed interface KitOutcome {
        data object KitEmpty : KitOutcome
        data object KitInvalid : KitOutcome
        data class KitSuccess(val result: ReckonYield) : KitOutcome
    }

    operator fun invoke(type: ReckonerSort, input: String): KitOutcome {
        val text = input.trim()
        if (text.isEmpty()) return KitOutcome.KitEmpty
        val amount = text.toDoubleOrNull()
        if (amount == null || amount.isNaN() || amount.isInfinite() || amount < 0) return KitOutcome.KitInvalid
        return KitOutcome.KitSuccess(compute(type, amount))
    }

    fun compute(type: ReckonerSort, amount: Double): ReckonYield =
        if (type.toDollars) ReckonYield.KitDollars(amount / type.factor)
        else ReckonYield.KitDiamonds((amount * type.factor).toLong())
}
