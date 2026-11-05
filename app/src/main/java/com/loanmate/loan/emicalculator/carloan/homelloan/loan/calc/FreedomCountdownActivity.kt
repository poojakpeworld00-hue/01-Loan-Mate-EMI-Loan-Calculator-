package com.loanmate.loan.emicalculator.carloan.homelloan.loan.calc

import android.text.Editable
import android.text.TextWatcher
import android.widget.SeekBar
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.loanmate.loan.emicalculator.carloan.homelloan.screens.shared.kitEnableLightEdgeToEdge
import com.loanmate.loan.emicalculator.carloan.homelloan.screens.shared.padForSysBars
import com.loanmate.loan.emicalculator.carloan.homelloan.R
import com.loanmate.loan.emicalculator.carloan.homelloan.loan.data.CurrencyRepo
import com.loanmate.loan.emicalculator.carloan.homelloan.databinding.LnActivityLoanFreedomBinding
import com.loanmate.loan.emicalculator.carloan.homelloan.loan.data.LoanPrefs
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.pow

/**
 * "Loan Freedom Countdown" — given a borrower's total loan amount, interest
 * rate, original tenure, and how many EMIs they have already paid, this
 * screen reports how close they are to debt-free.
 *
 * Maths is the standard amortisation outstanding-balance formula at month k:
 *   outstanding = P × [(1+r)^n − (1+r)^k] / [(1+r)^n − 1]
 * with r = monthly rate, n = total tenure months. EMI is the usual
 * P × r × (1+r)^n / ((1+r)^n − 1). Both fall back to the r=0 case.
 */
class FreedomCountdownActivity : AppCompatActivity() {

    private lateinit var binding: LnActivityLoanFreedomBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        kitEnableLightEdgeToEdge()
        binding = LnActivityLoanFreedomBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.padForSysBars()
        initView()
        initListeners()
    }

    private val moneyFormat = NumberFormat.getNumberInstance(Locale.US).apply {
        maximumFractionDigits = 0
    }
    private val dateFormat = SimpleDateFormat("MMM yyyy", Locale.US)

    private var amount = DEFAULT_AMOUNT
    private var rate = DEFAULT_RATE
    private var tenureYears = DEFAULT_TENURE_YEARS
    private var monthsPaid = DEFAULT_MONTHS_PAID

    private lateinit var currencySymbol: String
    private var suppressTextWatcher = false

    private fun initView() {
        currencySymbol = resolveCurrencySymbol()
        binding.tvCurrency.text = getString(R.string.ln_calc_currency_label, currencySymbol)
        binding.tvAmountSymbol.text = currencySymbol

        renderInputs()
        render()
    }

    private fun initListeners() {
        binding.btnBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

        binding.sliderAmount.setOnSeekBarChangeListener(progressListener { p ->
            amount = (p.toDouble() / AMOUNT_SLIDER_MAX) * MAX_AMOUNT
            updateAmountField()
        })
        binding.sliderRate.setOnSeekBarChangeListener(progressListener { p ->
            rate = p / 10.0
            updateRateField()
        })
        binding.sliderTotalTenure.setOnSeekBarChangeListener(progressListener { p ->
            tenureYears = p.coerceIn(1, MAX_TENURE_YEARS)
            clampMonthsPaid()
            updateTenureField()
            updateMonthsPaidSlider()
        })
        binding.sliderMonthsPaid.setOnSeekBarChangeListener(progressListener { p ->
            monthsPaid = p.coerceIn(0, tenureYears * 12)
            updateMonthsPaidField()
        })

        binding.etAmount.addTextChangedListener(textWatcher {
            amount = (it.toString().filter(Char::isDigit).toDoubleOrNull() ?: 0.0)
                .coerceIn(0.0, MAX_AMOUNT)
            binding.sliderAmount.progress =
                ((amount / MAX_AMOUNT) * AMOUNT_SLIDER_MAX).toInt()
                    .coerceIn(0, AMOUNT_SLIDER_MAX)
        })
        binding.etRate.addTextChangedListener(textWatcher {
            rate = (it.toString().toDoubleOrNull() ?: 0.0).coerceIn(0.0, RATE_MAX_PCT)
            binding.sliderRate.progress = (rate * 10).toInt().coerceIn(0, RATE_SLIDER_MAX)
        })
        binding.etTotalTenure.addTextChangedListener(textWatcher {
            tenureYears = (it.toString().toIntOrNull() ?: 0).coerceIn(0, MAX_TENURE_YEARS)
            clampMonthsPaid()
            binding.sliderTotalTenure.progress = tenureYears
            updateMonthsPaidSlider()
        })
        binding.etMonthsPaid.addTextChangedListener(textWatcher {
            monthsPaid = (it.toString().toIntOrNull() ?: 0).coerceIn(0, tenureYears * 12)
            binding.sliderMonthsPaid.progress = monthsPaid
        })

        binding.btnCalculate.setOnClickListener { render() }
        binding.btnReset.setOnClickListener {
            amount = DEFAULT_AMOUNT
            rate = DEFAULT_RATE
            tenureYears = DEFAULT_TENURE_YEARS
            monthsPaid = DEFAULT_MONTHS_PAID
            renderInputs()
            render()
        }
    }

    /* ----- input rendering ----- */

    private fun renderInputs() {
        binding.sliderAmount.max = AMOUNT_SLIDER_MAX
        binding.sliderRate.max = RATE_SLIDER_MAX
        binding.sliderTotalTenure.max = MAX_TENURE_YEARS

        binding.sliderAmount.progress =
            ((amount / MAX_AMOUNT) * AMOUNT_SLIDER_MAX).toInt()
                .coerceIn(0, AMOUNT_SLIDER_MAX)
        binding.sliderRate.progress = (rate * 10).toInt().coerceIn(0, RATE_SLIDER_MAX)
        binding.sliderTotalTenure.progress = tenureYears

        updateAmountField()
        updateRateField()
        updateTenureField()
        updateMonthsPaidSlider()
    }

    private fun updateAmountField() {
        suppressTextWatcher = true
        binding.etAmount.setText(moneyFormat.format(amount))
        binding.etAmount.setSelection(binding.etAmount.text.length)
        suppressTextWatcher = false
    }

    private fun updateRateField() {
        suppressTextWatcher = true
        binding.etRate.setText(formatRate(rate))
        binding.etRate.setSelection(binding.etRate.text.length)
        suppressTextWatcher = false
    }

    private fun updateTenureField() {
        suppressTextWatcher = true
        binding.etTotalTenure.setText(tenureYears.toString())
        binding.etTotalTenure.setSelection(binding.etTotalTenure.text.length)
        suppressTextWatcher = false
    }

    private fun updateMonthsPaidField() {
        suppressTextWatcher = true
        binding.etMonthsPaid.setText(monthsPaid.toString())
        binding.etMonthsPaid.setSelection(binding.etMonthsPaid.text.length)
        suppressTextWatcher = false
    }

    private fun updateMonthsPaidSlider() {
        binding.sliderMonthsPaid.max = (tenureYears * 12).coerceAtLeast(0)
        binding.sliderMonthsPaid.progress = monthsPaid
        updateMonthsPaidField()
    }

    private fun clampMonthsPaid() {
        monthsPaid = monthsPaid.coerceIn(0, tenureYears * 12)
    }

    /* ----- compute + render ----- */

    private fun render() {
        val totalMonths = tenureYears * 12
        if (amount <= 0.0 || totalMonths <= 0) {
            renderSummary(emi = 0.0, principalPaid = 0.0, interestPaid = 0.0,
                outstanding = amount, monthsRemaining = 0, pctPaid = 0f)
            return
        }
        val r = rate / 12.0 / 100.0
        val emi: Double
        val outstanding: Double
        val principalPaid: Double
        val interestPaid: Double
        if (r == 0.0) {
            emi = amount / totalMonths
            outstanding = amount * (1.0 - monthsPaid.toDouble() / totalMonths)
            principalPaid = amount - outstanding
            interestPaid = 0.0
        } else {
            val pow = (1 + r).pow(totalMonths)
            emi = amount * r * pow / (pow - 1)
            val powK = (1 + r).pow(monthsPaid)
            outstanding = (amount * (pow - powK) / (pow - 1)).coerceAtLeast(0.0)
            principalPaid = (amount - outstanding).coerceAtLeast(0.0)
            interestPaid = (emi * monthsPaid - principalPaid).coerceAtLeast(0.0)
        }

        val monthsRemaining = (totalMonths - monthsPaid).coerceAtLeast(0)
        val pctPaid = if (amount > 0) (principalPaid / amount * 100.0).toFloat() else 0f
        renderSummary(emi, principalPaid, interestPaid, outstanding, monthsRemaining, pctPaid)
    }

    private fun renderSummary(
        emi: Double,
        principalPaid: Double,
        interestPaid: Double,
        outstanding: Double,
        monthsRemaining: Int,
        pctPaid: Float
    ) {
        binding.tvMonthsRemaining.text = when (monthsRemaining) {
            0 -> getString(R.string.ln_freedom_value_zero_months)
            1 -> getString(R.string.ln_freedom_value_one_month)
            else -> getString(R.string.ln_freedom_value_months, monthsRemaining)
        }

        val freedomDate = Calendar.getInstance().apply {
            add(Calendar.MONTH, monthsRemaining)
        }
        binding.tvDebtFreeDate.text = getString(
            R.string.ln_freedom_debt_free_on,
            dateFormat.format(freedomDate.time)
        )

        binding.donut.percentage = pctPaid
        binding.tvDonutPct.text = "${pctPaid.toInt()}%"

        binding.tvEmi.text = currencySymbol + moneyFormat.format(emi)
        binding.tvPrincipalPaid.text = currencySymbol + moneyFormat.format(principalPaid)
        binding.tvInterestPaid.text = currencySymbol + moneyFormat.format(interestPaid)
        binding.tvOutstanding.text = currencySymbol + moneyFormat.format(outstanding)
    }

    /* ----- helpers ----- */

    private fun formatRate(value: Double): String =
        if (value == value.toInt().toDouble()) value.toInt().toString()
        else String.format(Locale.US, "%.1f", value)

    private fun resolveCurrencySymbol(): String {
        val pref = LoanPrefs(this)
        val code = pref.selectedCurrency.ifEmpty { "USD" }
        val item = CurrencyRepo(pref).getAll().firstOrNull { it.code == code }
        return item?.symbolRes?.let { getString(it) } ?: "$"
    }

    private fun progressListener(onChanged: (Int) -> Unit) =
        object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) onChanged(progress)
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) { render() }
        }

    private fun textWatcher(onChanged: (CharSequence) -> Unit) =
        object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (suppressTextWatcher) return
                onChanged(s ?: "")
                render()
            }
        }

    companion object {
        private const val DEFAULT_AMOUNT = 2_500_000.0
        private const val DEFAULT_RATE = 8.5
        private const val DEFAULT_TENURE_YEARS = 20
        private const val DEFAULT_MONTHS_PAID = 60

        private const val MAX_AMOUNT = 100_000_000.0
        private const val MAX_TENURE_YEARS = 30
        private const val AMOUNT_SLIDER_MAX = 1000
        private const val RATE_SLIDER_MAX = 500
        private const val RATE_MAX_PCT = 50.0
    }
}
