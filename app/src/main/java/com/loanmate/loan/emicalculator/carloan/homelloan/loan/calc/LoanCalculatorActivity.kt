package com.loanmate.loan.emicalculator.carloan.homelloan.loan.calc

import android.content.Context
import android.content.Intent
import android.view.View
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.loanmate.loan.emicalculator.carloan.homelloan.screens.shared.kitEnableLightEdgeToEdge
import com.loanmate.loan.emicalculator.carloan.homelloan.screens.shared.padForSysBars
import com.loanmate.loan.emicalculator.carloan.homelloan.loan.data.CalcOutcome
import com.loanmate.loan.emicalculator.carloan.homelloan.loan.data.CalcKind
import com.loanmate.loan.emicalculator.carloan.homelloan.loan.data.HistoryEntry
import com.loanmate.loan.emicalculator.carloan.homelloan.loan.data.CurrencyRepo
import com.loanmate.loan.emicalculator.carloan.homelloan.loan.data.HistoryRepo
import com.loanmate.loan.emicalculator.carloan.homelloan.databinding.LnActivityCalculatorBinding
import com.loanmate.loan.emicalculator.carloan.homelloan.loan.data.LoanPrefs
import java.text.NumberFormat
import java.util.Locale

/**
 * Shared host activity for every numeric calculator. The top bar (back +
 * title) and the summary card are owned by this activity; the input card
 * itself is delegated to a [CalcInputPanel] controller selected per
 * [CalcKind] — each controller owns its own typed binding, reads its
 * fields, and produces a [CalcOutcome].
 */
class LoanCalculatorActivity : AppCompatActivity() {

    private lateinit var binding: LnActivityCalculatorBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        kitEnableLightEdgeToEdge()
        binding = LnActivityCalculatorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.padForSysBars()
        initView()
        initListeners()
    }

    private lateinit var type: CalcKind
    private lateinit var currencySymbol: String
    private lateinit var inputCard: CalcInputPanel

    private val moneyFormat = NumberFormat.getNumberInstance(Locale.US).apply {
        maximumFractionDigits = 0
    }
    private val moneyFormat2 = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }

    private fun initView() {
        type = resolveType()
        currencySymbol = resolveCurrencySymbol()

        binding.tvTitle.setText(type.titleRes)
        binding.tvCurrency.text = currencySymbol
        binding.tvCurrency.visibility = View.VISIBLE
        binding.tvHeadlineSymbol.text = currencySymbol
        binding.lblHeadline.setText(type.headlineRes)

        // Inflate the per-type input card into its container
        inputCard = CalcInputPanel.create(this, type, binding.inputCardContainer)
        inputCard.applyCurrency(currencySymbol)

        // Apply optional pre-fill from history (only meaningful for the simple
        // amount/rate/tenure binders; the richer cards just render defaults).
        applyPrefillIfPresent()
    }

    private fun initListeners() {
        binding.btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        inputCard.calculateButton.setOnClickListener {
            val result = inputCard.compute()
            renderSummary(result)
            saveToHistory(result)
        }
        inputCard.resetButton.setOnClickListener {
            inputCard.reset()
            binding.summarySection.visibility = View.GONE
        }
    }

    /* ----- summary ----- */

    private fun renderSummary(result: CalcOutcome) {
        binding.summarySection.visibility = View.VISIBLE
        binding.tvHeadlineValue.text = moneyFormat2.format(result.headlineValue)
        binding.tvPrincipal.text = currencySymbol + moneyFormat.format(result.principal)
        binding.tvInterest.text = currencySymbol + moneyFormat.format(result.interest)
        binding.tvTotal.text = currencySymbol + moneyFormat.format(result.total)
        binding.donut.percentage = result.interestPct
        binding.tvDonutPct.text = "${result.interestPct.toInt()}%"
    }

    /* ----- history persistence ----- */

    private fun saveToHistory(result: CalcOutcome) {
        if (result == CalcOutcome.EMPTY) return
        val pref = LoanPrefs(this)
        HistoryRepo(pref).add(
            HistoryEntry(
                calculatorTypeName = type.name,
                createdAtMillis = System.currentTimeMillis(),
                amount = result.principal,
                ratePct = 0.0,
                tenureMonths = 0,
                tenureWasYears = true,
                startDateMillis = null,
                headlineValue = result.headlineValue,
                principal = result.principal,
                interest = result.interest,
                total = result.total,
                interestPct = result.interestPct
            )
        )
    }

    private fun applyPrefillIfPresent() {
        // The richer per-type cards have their own field set, so we only
        // attempt prefill for the simple Loan-style binder where the fields
        // line up cleanly. Safer to no-op here than to mis-bind.
        // Future: add a typed prefill hook on CalcInputPanel if needed.
    }

    /* ----- intent + dependencies ----- */

    private fun resolveType(): CalcKind {
        val raw = intent.getStringExtra(EXTRA_TYPE)
        return CalcKind.fromIdOrNull(raw) ?: CalcKind.LOAN_EMI
    }

    private fun resolveCurrencySymbol(): String {
        val pref = LoanPrefs(this)
        val code = pref.selectedCurrency.ifEmpty { "USD" }
        val item = CurrencyRepo(pref).getAll().firstOrNull { it.code == code }
        return item?.symbolRes?.let { getString(it) } ?: "$"
    }

    companion object {
        private const val EXTRA_TYPE = "extra_calc_type"

        fun newIntent(context: Context, type: CalcKind): Intent =
            Intent(context, LoanCalculatorActivity::class.java).apply {
                putExtra(EXTRA_TYPE, type.name)
            }

        /**
         * Accepted for source compatibility with [LoanHistoryFragment]; the
         * per-type input cards now ignore the prefill payload because the
         * field shape varies. Reopens the calculator at its defaults.
         */
        @Suppress("UNUSED_PARAMETER")
        fun newIntent(
            context: Context,
            type: CalcKind,
            item: HistoryEntry
        ): Intent = newIntent(context, type)
    }
}
