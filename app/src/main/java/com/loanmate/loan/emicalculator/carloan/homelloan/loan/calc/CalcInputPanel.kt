package com.loanmate.loan.emicalculator.carloan.homelloan.loan.calc

import android.app.DatePickerDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.loanmate.loan.emicalculator.carloan.homelloan.R
import com.loanmate.loan.emicalculator.carloan.homelloan.loan.data.CalcOutcome
import com.loanmate.loan.emicalculator.carloan.homelloan.loan.data.CalcKind
import com.loanmate.loan.emicalculator.carloan.homelloan.databinding.LnCardInputsBusinessBinding
import com.loanmate.loan.emicalculator.carloan.homelloan.databinding.LnCardInputsCashBinding
import com.loanmate.loan.emicalculator.carloan.homelloan.databinding.LnCardInputsCompoundBinding
import com.loanmate.loan.emicalculator.carloan.homelloan.databinding.LnCardInputsFdBinding
import com.loanmate.loan.emicalculator.carloan.homelloan.databinding.LnCardInputsHomeBinding
import com.loanmate.loan.emicalculator.carloan.homelloan.databinding.LnCardInputsLoanBinding
import com.loanmate.loan.emicalculator.carloan.homelloan.databinding.LnCardInputsPpfBinding
import com.loanmate.loan.emicalculator.carloan.homelloan.databinding.LnCardInputsRdBinding
import com.loanmate.loan.emicalculator.carloan.homelloan.databinding.LnCardInputsRoiBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.pow

/**
 * Per-calculator input card. Each subclass owns its own typed ViewBinding and
 * knows how to read its inputs into a [CalcOutcome]. The shared
 * [LoanCalculatorActivity] inflates one of these into its container, then forwards
 * Calculate / Reset clicks to the controller. CASH produces its own
 * non-amortising result (sum of denominations × counts); everything else
 * delegates to [CalcKind.compute].
 */
sealed class CalcInputPanel(val type: CalcKind) {

    abstract val root: View

    /** Buttons exposed to the activity for click wiring. */
    abstract val calculateButton: View
    abstract val resetButton: View

    /** Read current input values and produce a [CalcOutcome]. */
    abstract fun compute(): CalcOutcome

    /** Restore default values into the fields. */
    abstract fun reset()

    /** Update the currency-symbol prefix on every relevant field. */
    open fun applyCurrency(symbol: String) {}

    companion object {
        fun create(
            context: Context,
            type: CalcKind,
            container: ViewGroup
        ): CalcInputPanel {
            val inflater = LayoutInflater.from(context)
            return when (type) {
                CalcKind.LOAN_EMI, CalcKind.AUTO_LOAN ->
                    Loan(type, LnCardInputsLoanBinding.inflate(inflater, container, true))
                CalcKind.BUSINESS_LOAN ->
                    Business(type, LnCardInputsBusinessBinding.inflate(inflater, container, true))
                CalcKind.HOME_LOAN ->
                    Home(type, LnCardInputsHomeBinding.inflate(inflater, container, true))
                CalcKind.RD ->
                    Rd(type, LnCardInputsRdBinding.inflate(inflater, container, true))
                CalcKind.FD ->
                    Fd(type, LnCardInputsFdBinding.inflate(inflater, container, true))
                CalcKind.ROI ->
                    Roi(type, LnCardInputsRoiBinding.inflate(inflater, container, true))
                CalcKind.PPF ->
                    Ppf(type, LnCardInputsPpfBinding.inflate(inflater, container, true))
                CalcKind.COMPOUND ->
                    Compound(type, LnCardInputsCompoundBinding.inflate(inflater, container, true))
                CalcKind.CASH ->
                    Cash(type, LnCardInputsCashBinding.inflate(inflater, container, true))
            }
        }
    }

    /* ----- shared helpers ----- */

    protected fun EditText.amount(): Double =
        text.toString().filter(Char::isDigit).toDoubleOrNull() ?: 0.0

    protected fun EditText.decimal(): Double =
        text.toString().toDoubleOrNull() ?: 0.0

    protected fun EditText.intValue(): Int =
        text.toString().toIntOrNull() ?: 0

    protected fun bindMonthYearToggle(
        btnMonths: TextView,
        btnYears: TextView,
        startInYears: Boolean,
        onChanged: (years: Boolean) -> Unit
    ) {
        var yearsActive = startInYears
        val ctx = btnMonths.context
        val applyState: () -> Unit = {
            btnYears.setTextColor(
                ContextCompat.getColor(
                    ctx,
                    if (yearsActive) R.color.ln_brand_primary else R.color.ln_text_secondary
                )
            )
            btnMonths.setTextColor(
                ContextCompat.getColor(
                    ctx,
                    if (yearsActive) R.color.ln_text_secondary else R.color.ln_brand_primary
                )
            )
        }
        applyState()
        btnYears.setOnClickListener {
            if (!yearsActive) {
                yearsActive = true
                applyState()
                onChanged(true)
            }
        }
        btnMonths.setOnClickListener {
            if (yearsActive) {
                yearsActive = false
                applyState()
                onChanged(false)
            }
        }
    }

    /* =====================================================================
     * Subclasses
     * ==================================================================== */

    /** LOAN_EMI / AUTO_LOAN — basic 3 fields + start date. */
    class Loan(
        type: CalcKind,
        private val b: LnCardInputsLoanBinding
    ) : CalcInputPanel(type) {
        override val root: View get() = b.root
        override val calculateButton: View get() = b.btnCalculate
        override val resetButton: View get() = b.btnReset

        private var tenureInYears = true
        private val startDate = Calendar.getInstance()
        private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        init {
            reset()
            bindMonthYearToggle(b.btnMonths, b.btnYears, true) { tenureInYears = it }
            b.btnStartDate.setOnClickListener {
                DatePickerDialog(
                    b.root.context,
                    { _, y, m, d ->
                        startDate.set(y, m, d)
                        b.tvStartDate.text = dateFormat.format(startDate.time)
                    },
                    startDate.get(Calendar.YEAR),
                    startDate.get(Calendar.MONTH),
                    startDate.get(Calendar.DAY_OF_MONTH)
                ).show()
            }
        }

        override fun applyCurrency(symbol: String) {
            b.tvAmountSymbol.text = symbol
        }

        override fun compute(): CalcOutcome {
            val amount = b.etAmount.amount().takeIf { it > 0 } ?: type.defaultAmount
            val rate = b.etRate.decimal().takeIf { it > 0 } ?: type.defaultRate
            val tenure = b.etTenure.intValue().takeIf { it > 0 } ?: type.defaultTenureMonths
            val months = if (tenureInYears) tenure * 12 else tenure
            return type.compute(amount, rate, months)
        }

        override fun reset() {
            b.etAmount.setText("")
            b.etRate.setText("")
            b.etTenure.setText("")
            tenureInYears = true
            b.tvStartDate.text = dateFormat.format(startDate.time)
        }
    }

    /** BUSINESS_LOAN — Loan + Payment Frequency dropdown. */
    class Business(
        type: CalcKind,
        private val b: LnCardInputsBusinessBinding
    ) : CalcInputPanel(type) {
        override val root: View get() = b.root
        override val calculateButton: View get() = b.btnCalculate
        override val resetButton: View get() = b.btnReset

        private var tenureInYears = true

        init {
            reset()
            bindMonthYearToggle(b.btnMonths, b.btnYears, true) { tenureInYears = it }
            val ctx = b.root.context
            b.spinnerFrequency.adapter = ArrayAdapter(
                ctx,
                android.R.layout.simple_spinner_dropdown_item,
                listOf(
                    ctx.getString(R.string.ln_ci_freq_monthly),
                    ctx.getString(R.string.ln_ci_freq_quarterly),
                    ctx.getString(R.string.ln_ci_freq_half_yearly),
                    ctx.getString(R.string.ln_ci_freq_yearly)
                )
            )
        }

        override fun applyCurrency(symbol: String) {
            b.tvAmountSymbol.text = symbol
        }

        override fun compute(): CalcOutcome {
            val amount = b.etAmount.amount().takeIf { it > 0 } ?: type.defaultAmount
            val rate = b.etRate.decimal().takeIf { it > 0 } ?: type.defaultRate
            val tenure = b.etTenure.intValue().takeIf { it > 0 } ?: type.defaultTenureMonths
            val months = if (tenureInYears) tenure * 12 else tenure
            return type.compute(amount, rate, months)
        }

        override fun reset() {
            b.etAmount.setText("")
            b.etRate.setText("")
            b.etTenure.setText("")
            tenureInYears = true
            b.spinnerFrequency.setSelection(0)
        }
    }

    /** HOME_LOAN — full mortgage form: home price, down payment ($/%), tax, PMI, insurance, HOA. */
    class Home(
        type: CalcKind,
        private val b: LnCardInputsHomeBinding
    ) : CalcInputPanel(type) {
        override val root: View get() = b.root
        override val calculateButton: View get() = b.btnCalculate
        override val resetButton: View get() = b.btnReset

        private var tenureInYears = true

        init {
            reset()
            bindMonthYearToggle(b.btnMonths, b.btnYears, true) { tenureInYears = it }
        }

        override fun applyCurrency(symbol: String) {
            b.tvHomeSymbol.text = symbol
            b.tvDownSymbol.text = symbol
            b.tvTaxSymbol.text = symbol
            b.tvPmiSymbol.text = symbol
            b.tvInsuranceSymbol.text = symbol
            b.tvHoaSymbol.text = symbol
        }

        override fun compute(): CalcOutcome {
            val price = b.etHomePrice.amount().takeIf { it > 0 } ?: type.defaultAmount
            // Use $ down payment if provided, else fall back to %
            val downPct = b.etDownPct.decimal()
            val downAmt = b.etDownAmount.amount().takeIf { it > 0 }
                ?: (price * downPct / 100.0)
            val financed = (price - downAmt).coerceAtLeast(0.0)
            val rate = b.etRate.decimal().takeIf { it > 0 } ?: type.defaultRate
            val tenure = b.etTenure.intValue().takeIf { it > 0 } ?: type.defaultTenureMonths
            val months = if (tenureInYears) tenure * 12 else tenure

            // Standard EMI on the financed portion
            val base = type.compute(financed, rate, months)

            // Add monthly tax / PMI / insurance / HOA on top of the EMI headline
            val monthlyAddOns = (b.etTax.amount() + b.etPmi.amount() +
                b.etInsurance.amount() + b.etHoa.amount()) / 12.0
            val totalMonthly = base.headlineValue + monthlyAddOns
            return base.copy(headlineValue = totalMonthly)
        }

        override fun reset() {
            b.etHomePrice.setText("")
            b.etDownAmount.setText("")
            b.etDownPct.setText("")
            b.etTenure.setText("")
            b.etRate.setText("")
            b.etTax.setText("")
            b.etPmi.setText("")
            b.etInsurance.setText("")
            b.etHoa.setText("")
            tenureInYears = true
        }
    }

    /** RD — monthly deposit + rate + tenure + payment frequency. */
    class Rd(
        type: CalcKind,
        private val b: LnCardInputsRdBinding
    ) : CalcInputPanel(type) {
        override val root: View get() = b.root
        override val calculateButton: View get() = b.btnCalculate
        override val resetButton: View get() = b.btnReset

        private var tenureInYears = true

        init {
            reset()
            bindMonthYearToggle(b.btnMonths, b.btnYears, true) { tenureInYears = it }
            val ctx = b.root.context
            b.spinnerFrequency.adapter = ArrayAdapter(
                ctx,
                android.R.layout.simple_spinner_dropdown_item,
                listOf(
                    ctx.getString(R.string.ln_ci_freq_monthly),
                    ctx.getString(R.string.ln_ci_freq_quarterly),
                    ctx.getString(R.string.ln_ci_freq_half_yearly),
                    ctx.getString(R.string.ln_ci_freq_yearly)
                )
            )
        }

        override fun applyCurrency(symbol: String) {
            b.tvAmountSymbol.text = symbol
        }

        override fun compute(): CalcOutcome {
            val amount = b.etAmount.amount().takeIf { it > 0 } ?: type.defaultAmount
            val rate = b.etRate.decimal().takeIf { it > 0 } ?: type.defaultRate
            val tenure = b.etTenure.intValue().takeIf { it > 0 } ?: type.defaultTenureMonths
            val months = if (tenureInYears) tenure * 12 else tenure
            return type.compute(amount, rate, months)
        }

        override fun reset() {
            b.etAmount.setText("")
            b.etRate.setText("")
            b.etTenure.setText("")
            tenureInYears = true
            b.spinnerFrequency.setSelection(0)
        }
    }

    /** FD — investment + rate + tenure + compounding frequency. */
    class Fd(
        type: CalcKind,
        private val b: LnCardInputsFdBinding
    ) : CalcInputPanel(type) {
        override val root: View get() = b.root
        override val calculateButton: View get() = b.btnCalculate
        override val resetButton: View get() = b.btnReset

        private var tenureInYears = true

        init {
            reset()
            bindMonthYearToggle(b.btnMonths, b.btnYears, true) { tenureInYears = it }
            val ctx = b.root.context
            b.spinnerCompounding.adapter = ArrayAdapter(
                ctx,
                android.R.layout.simple_spinner_dropdown_item,
                listOf(
                    ctx.getString(R.string.ln_ci_freq_monthly),
                    ctx.getString(R.string.ln_ci_freq_quarterly),
                    ctx.getString(R.string.ln_ci_freq_half_yearly),
                    ctx.getString(R.string.ln_ci_freq_yearly)
                )
            )
        }

        override fun applyCurrency(symbol: String) {
            b.tvAmountSymbol.text = symbol
        }

        override fun compute(): CalcOutcome {
            val amount = b.etAmount.amount().takeIf { it > 0 } ?: type.defaultAmount
            val rate = b.etRate.decimal().takeIf { it > 0 } ?: type.defaultRate
            val tenure = b.etTenure.intValue().takeIf { it > 0 } ?: type.defaultTenureMonths
            val months = if (tenureInYears) tenure * 12 else tenure
            return type.compute(amount, rate, months)
        }

        override fun reset() {
            b.etAmount.setText("")
            b.etRate.setText("")
            b.etTenure.setText("")
            tenureInYears = true
            b.spinnerCompounding.setSelection(0)
        }
    }

    /** ROI — starting + final + tenure (annualised compound return). */
    class Roi(
        type: CalcKind,
        private val b: LnCardInputsRoiBinding
    ) : CalcInputPanel(type) {
        override val root: View get() = b.root
        override val calculateButton: View get() = b.btnCalculate
        override val resetButton: View get() = b.btnReset

        private var tenureInYears = true

        init {
            reset()
            bindMonthYearToggle(b.btnMonths, b.btnYears, true) { tenureInYears = it }
        }

        override fun applyCurrency(symbol: String) {
            b.tvStartingSymbol.text = symbol
            b.tvLastSymbol.text = symbol
        }

        override fun compute(): CalcOutcome {
            val starting = b.etStarting.amount().takeIf { it > 0 } ?: type.defaultAmount
            val last = b.etLast.amount().takeIf { it > 0 } ?: starting
            val tenure = b.etTenure.intValue().takeIf { it > 0 } ?: type.defaultTenureMonths
            val months = if (tenureInYears) tenure * 12 else tenure
            val years = months / 12.0
            val gain = last - starting
            val pct = if (starting > 0) (gain / starting * 100.0).toFloat() else 0f
            // Use the standard donut as "ROI %" of total — clamp to 0..100
            val donutPct = pct.coerceIn(0f, 100f)
            return CalcOutcome(
                headlineValue = last,
                principal = starting,
                interest = gain,
                total = last,
                interestPct = donutPct
            )
        }

        override fun reset() {
            b.etStarting.setText("")
            b.etLast.setText("")
            b.etTenure.setText("")
            tenureInYears = true
        }
    }

    /** PPF — yearly investment + rate + tenure (yearly compounding). */
    class Ppf(
        type: CalcKind,
        private val b: LnCardInputsPpfBinding
    ) : CalcInputPanel(type) {
        override val root: View get() = b.root
        override val calculateButton: View get() = b.btnCalculate
        override val resetButton: View get() = b.btnReset

        private var tenureInYears = true

        init {
            reset()
            bindMonthYearToggle(b.btnMonths, b.btnYears, true) { tenureInYears = it }
        }

        override fun applyCurrency(symbol: String) {
            b.tvAmountSymbol.text = symbol
        }

        override fun compute(): CalcOutcome {
            val yearly = b.etAmount.amount().takeIf { it > 0 } ?: type.defaultAmount
            val rate = b.etRate.decimal().takeIf { it > 0 } ?: type.defaultRate
            val tenure = b.etTenure.intValue().takeIf { it > 0 } ?: type.defaultTenureMonths
            val months = if (tenureInYears) tenure * 12 else tenure
            // Convert yearly deposit → monthly equivalent so we can reuse the RD-style compute
            val monthly = yearly / 12.0
            return type.compute(monthly, rate, months)
        }

        override fun reset() {
            b.etAmount.setText("")
            b.etRate.setText("")
            b.etTenure.setText("")
            tenureInYears = true
        }
    }

    /** COMPOUND — principal + rate + frequency-per-year + compounding cycle. */
    class Compound(
        type: CalcKind,
        private val b: LnCardInputsCompoundBinding
    ) : CalcInputPanel(type) {
        override val root: View get() = b.root
        override val calculateButton: View get() = b.btnCalculate
        override val resetButton: View get() = b.btnReset

        init {
            reset()
            val ctx = b.root.context
            b.spinnerCompounding.adapter = ArrayAdapter(
                ctx,
                android.R.layout.simple_spinner_dropdown_item,
                listOf(
                    ctx.getString(R.string.ln_ci_freq_daily),
                    ctx.getString(R.string.ln_ci_freq_monthly),
                    ctx.getString(R.string.ln_ci_freq_quarterly),
                    ctx.getString(R.string.ln_ci_freq_half_yearly),
                    ctx.getString(R.string.ln_ci_freq_yearly)
                )
            )
        }

        override fun applyCurrency(symbol: String) {
            b.tvPrincipalSymbol.text = symbol
        }

        override fun compute(): CalcOutcome {
            val principal = b.etPrincipal.amount().takeIf { it > 0 } ?: type.defaultAmount
            val rate = b.etRate.decimal().takeIf { it > 0 } ?: type.defaultRate
            val timesPerYear = b.etTimes.intValue().takeIf { it > 0 } ?: 12
            // Treat "Times In Year" as compounds per year over a default 1-year horizon
            val r = rate / 100.0
            val maturity = principal * (1 + r / timesPerYear).pow(timesPerYear.toDouble())
            val interest = (maturity - principal).coerceAtLeast(0.0)
            val pct = if (maturity > 0) (interest / maturity * 100.0).toFloat() else 0f
            return CalcOutcome(maturity, principal, interest, maturity, pct)
        }

        override fun reset() {
            b.etPrincipal.setText("")
            b.etRate.setText("")
            b.etTimes.setText("")
            b.spinnerCompounding.setSelection(1) // Monthly
        }
    }

    /** CASH — sum(denomination × count) over 6 fixed denominations. */
    class Cash(
        type: CalcKind,
        private val b: LnCardInputsCashBinding
    ) : CalcInputPanel(type) {
        override val root: View get() = b.root
        override val calculateButton: View get() = b.btnCalculate
        override val resetButton: View get() = b.btnReset

        private val denoms = listOf(10, 20, 50, 100, 200, 500)

        init {
            // Live total updates as user types
            val watcher = object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: android.text.Editable?) { updateTotal() }
            }
            listOf(b.count10, b.count20, b.count50, b.count100, b.count200, b.count500)
                .forEach { it.addTextChangedListener(watcher) }
            updateTotal()
        }

        override fun applyCurrency(symbol: String) {
            b.sym10.text = symbol
            b.sym20.text = symbol
            b.sym50.text = symbol
            b.sym100.text = symbol
            b.sym200.text = symbol
            b.sym500.text = symbol
        }

        private fun rawTotal(): Double {
            val counts = listOf(
                b.count10.intValue(), b.count20.intValue(), b.count50.intValue(),
                b.count100.intValue(), b.count200.intValue(), b.count500.intValue()
            )
            return denoms.zip(counts).sumOf { (denom, c) -> (denom * c).toDouble() }
        }

        private fun updateTotal() {
            val total = rawTotal()
            val fmt = java.text.NumberFormat.getNumberInstance(Locale.US).apply {
                maximumFractionDigits = 0
            }
            b.tvTotalAmount.text = fmt.format(total)
        }

        override fun compute(): CalcOutcome {
            val total = rawTotal()
            return CalcOutcome(
                headlineValue = total,
                principal = total,
                interest = 0.0,
                total = total,
                interestPct = 0f
            )
        }

        override fun reset() {
            listOf(b.count10, b.count20, b.count50, b.count100, b.count200, b.count500)
                .forEach { it.setText("") }
            updateTotal()
        }
    }
}
