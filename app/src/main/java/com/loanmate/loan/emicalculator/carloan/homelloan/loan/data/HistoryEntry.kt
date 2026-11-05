package com.loanmate.loan.emicalculator.carloan.homelloan.loan.data

import java.util.UUID

/**
 * One persisted calculation. Stored as JSON inside LoanPrefs via [HistoryRepo].
 * `calculatorTypeName` matches a [CalcKind.name] so we can resolve the title
 * + reopen the calculator with the same inputs from the History list.
 */
data class HistoryEntry(
    val id: String = UUID.randomUUID().toString(),
    val calculatorTypeName: String,
    val createdAtMillis: Long,
    val amount: Double,
    val ratePct: Double,
    val tenureMonths: Int,
    val tenureWasYears: Boolean,
    val startDateMillis: Long?,
    val headlineValue: Double,
    val principal: Double,
    val interest: Double,
    val total: Double,
    val interestPct: Float
)
