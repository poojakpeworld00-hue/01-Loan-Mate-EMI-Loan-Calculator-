package com.loanmate.loan.emicalculator.carloan.homelloan.loan.data

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.loanmate.loan.emicalculator.carloan.homelloan.R
import kotlin.math.pow

/**
 * Drives the shared calculator screen. Each value chooses its labels (so the
 * card reads naturally for loans vs. deposits vs. investments) and supplies
 * the actual maths via [compute]. Keep one entry per Home tile that should
 * land on a real calculator.
 */
enum class CalcKind(
    @StringRes val titleRes: Int,
    @DrawableRes val iconRes: Int,
    @StringRes val amountLabelRes: Int,
    @StringRes val tenureLabelRes: Int,
    val showStartDate: Boolean,
    @StringRes val headlineRes: Int,
    val maxAmount: Double,
    val defaultAmount: Double,
    val defaultRate: Double,
    val defaultTenureMonths: Int
) {
    /* Loan-style calculators — standard amortising EMI */
    LOAN_EMI(
        R.string.ln_home_calc_emi,
        R.drawable.ln_ic_calc_emi,
        R.string.ln_calc_field_loan_amount,
        R.string.ln_calc_field_tenure,
        true,
        R.string.ln_calc_headline_emi,
        maxAmount = 10_000_000.0,
        defaultAmount = 25_000.0,
        defaultRate = 7.5,
        defaultTenureMonths = 60
    ),
    BUSINESS_LOAN(
        R.string.ln_home_calc_business,
        R.drawable.ln_ic_calc_business,
        R.string.ln_calc_field_loan_amount,
        R.string.ln_calc_field_tenure,
        true,
        R.string.ln_calc_headline_emi,
        maxAmount = 50_000_000.0,
        defaultAmount = 500_000.0,
        defaultRate = 11.0,
        defaultTenureMonths = 36
    ),
    AUTO_LOAN(
        R.string.ln_home_calc_auto,
        R.drawable.ln_ic_calc_auto,
        R.string.ln_calc_field_loan_amount,
        R.string.ln_calc_field_tenure,
        true,
        R.string.ln_calc_headline_emi,
        maxAmount = 5_000_000.0,
        defaultAmount = 800_000.0,
        defaultRate = 9.0,
        defaultTenureMonths = 60
    ),
    HOME_LOAN(
        R.string.ln_home_calc_home_loan,
        R.drawable.ln_ic_calc_home_loan,
        R.string.ln_calc_field_loan_amount,
        R.string.ln_calc_field_tenure,
        true,
        R.string.ln_calc_headline_emi,
        maxAmount = 100_000_000.0,
        defaultAmount = 2_500_000.0,
        defaultRate = 8.5,
        defaultTenureMonths = 240
    ),

    /* Banking / deposit-style calculators — compound growth */
    RD(
        R.string.ln_home_grid_rd,
        R.drawable.ln_ic_grid_rd,
        R.string.ln_calc_field_monthly_deposit,
        R.string.ln_calc_field_tenure_simple,
        false,
        R.string.ln_calc_headline_maturity,
        maxAmount = 200_000.0,
        defaultAmount = 5_000.0,
        defaultRate = 6.5,
        defaultTenureMonths = 60
    ),
    FD(
        R.string.ln_home_grid_fd,
        R.drawable.ln_ic_grid_fd,
        R.string.ln_calc_field_principal,
        R.string.ln_calc_field_tenure_simple,
        false,
        R.string.ln_calc_headline_maturity,
        maxAmount = 10_000_000.0,
        defaultAmount = 100_000.0,
        defaultRate = 7.0,
        defaultTenureMonths = 36
    ),
    PPF(
        R.string.ln_home_grid_ppf,
        R.drawable.ln_ic_grid_ppf,
        R.string.ln_calc_field_monthly_deposit,
        R.string.ln_calc_field_tenure_simple,
        false,
        R.string.ln_calc_headline_maturity,
        maxAmount = 150_000.0,
        defaultAmount = 12_500.0,
        defaultRate = 7.1,
        defaultTenureMonths = 180
    ),
    COMPOUND(
        R.string.ln_home_grid_compound,
        R.drawable.ln_ic_grid_compound,
        R.string.ln_calc_field_principal,
        R.string.ln_calc_field_tenure_simple,
        false,
        R.string.ln_calc_headline_maturity,
        maxAmount = 10_000_000.0,
        defaultAmount = 100_000.0,
        defaultRate = 8.0,
        defaultTenureMonths = 60
    ),
    ROI(
        R.string.ln_home_grid_roi,
        R.drawable.ln_ic_grid_roi,
        R.string.ln_calc_field_initial_investment,
        R.string.ln_calc_field_tenure_simple,
        false,
        R.string.ln_calc_headline_return,
        maxAmount = 10_000_000.0,
        defaultAmount = 100_000.0,
        defaultRate = 12.0,
        defaultTenureMonths = 60
    ),
    CASH(
        R.string.ln_home_grid_cash,
        R.drawable.ln_ic_grid_cash,
        R.string.ln_ci_total_amount,
        R.string.ln_calc_field_tenure_simple,
        false,
        R.string.ln_calc_headline_maturity,
        maxAmount = 0.0,
        defaultAmount = 0.0,
        defaultRate = 0.0,
        defaultTenureMonths = 0
    );

    /**
     * @param amount principal / monthly deposit / initial investment
     * @param annualRatePct interest rate as a percentage (e.g. 7.5)
     * @param tenureMonths total tenure in months
     */
    fun compute(amount: Double, annualRatePct: Double, tenureMonths: Int): CalcOutcome {
        if (amount <= 0 || tenureMonths <= 0) return CalcOutcome.EMPTY

        return when (this) {
            LOAN_EMI, BUSINESS_LOAN, AUTO_LOAN, HOME_LOAN ->
                computeEmi(amount, annualRatePct, tenureMonths)
            RD, PPF -> computeRecurringDeposit(amount, annualRatePct, tenureMonths)
            FD, COMPOUND -> computeCompound(amount, annualRatePct, tenureMonths)
            ROI -> computeRoi(amount, annualRatePct, tenureMonths)
            // CASH uses its own bespoke compute path inside CalcInputPanel.Cash
            CASH -> CalcOutcome.EMPTY
        }
    }

    private fun computeEmi(p: Double, rate: Double, n: Int): CalcOutcome {
        val r = rate / 12.0 / 100.0
        val emi = if (r == 0.0) p / n
        else p * r * (1 + r).pow(n) / ((1 + r).pow(n) - 1)
        val total = emi * n
        val interest = total - p
        val pct = if (total > 0) (interest / total * 100.0).toFloat() else 0f
        return CalcOutcome(emi, p, interest, total, pct)
    }

    /**
     * Standard recurring deposit / SIP-style maturity:
     *   M = P × ((1 + r)^n - 1) / r × (1 + r)
     * where r = monthly rate, n = months, P = monthly deposit.
     */
    private fun computeRecurringDeposit(p: Double, rate: Double, n: Int): CalcOutcome {
        val r = rate / 12.0 / 100.0
        val maturity = if (r == 0.0) p * n
        else p * (((1 + r).pow(n) - 1) / r) * (1 + r)
        val principal = p * n
        val interest = maturity - principal
        val pct = if (maturity > 0) (interest / maturity * 100.0).toFloat() else 0f
        return CalcOutcome(maturity, principal, interest, maturity, pct)
    }

    /**
     * Lump-sum compound growth (monthly compounded):
     *   A = P × (1 + r)^n
     */
    private fun computeCompound(p: Double, rate: Double, n: Int): CalcOutcome {
        val r = rate / 12.0 / 100.0
        val maturity = p * (1 + r).pow(n)
        val interest = maturity - p
        val pct = if (maturity > 0) (interest / maturity * 100.0).toFloat() else 0f
        return CalcOutcome(maturity, p, interest, maturity, pct)
    }

    /** ROI = compound return over the period; the headline is the final value. */
    private fun computeRoi(p: Double, rate: Double, n: Int): CalcOutcome =
        computeCompound(p, rate, n)

    companion object {
        fun fromIdOrNull(id: String?): CalcKind? =
            values().firstOrNull { it.name == id }
    }
}
