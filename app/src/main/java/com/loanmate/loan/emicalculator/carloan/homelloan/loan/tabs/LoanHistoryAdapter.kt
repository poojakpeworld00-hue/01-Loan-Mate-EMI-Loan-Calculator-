package com.loanmate.loan.emicalculator.carloan.homelloan.loan.tabs

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.loanmate.loan.emicalculator.carloan.homelloan.R
import com.loanmate.loan.emicalculator.carloan.homelloan.loan.data.CalcKind
import com.loanmate.loan.emicalculator.carloan.homelloan.loan.data.HistoryEntry
import com.loanmate.loan.emicalculator.carloan.homelloan.databinding.LnItemHistoryBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LoanHistoryAdapter(
    private val currencySymbolProvider: () -> String,
    private val onView: (HistoryEntry) -> Unit,
    private val onDelete: (HistoryEntry) -> Unit
) : ListAdapter<HistoryEntry, LoanHistoryAdapter.VH>(DIFF) {

    private val moneyFormat = java.text.NumberFormat.getNumberInstance(Locale.US).apply {
        maximumFractionDigits = 0
    }
    private val dateFormat = SimpleDateFormat("yyyy-M-d", Locale.US)

    inner class VH(val b: LnItemHistoryBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: HistoryEntry) {
            val context: Context = b.root.context

            val type = CalcKind.fromIdOrNull(item.calculatorTypeName)
            b.tvTitle.text = type?.let { context.getString(it.titleRes) }
                ?: item.calculatorTypeName
            b.tvDate.text = dateFormat.format(Date(item.createdAtMillis))

            b.tvInterest.text = formatRate(item.ratePct)
            b.tvDuration.text = formatTenure(context, item.tenureMonths, item.tenureWasYears)
            b.tvAmount.text = currencySymbolProvider() + moneyFormat.format(item.amount)

            b.itemRoot.setOnClickListener { onView(item) }
            b.btnView.setOnClickListener { onView(item) }
            b.btnDelete.setOnClickListener { onDelete(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = LnItemHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    private fun formatRate(value: Double): String =
        if (value == value.toInt().toDouble()) "${value.toInt()}%"
        else String.format(Locale.US, "%.1f%%", value)

    private fun formatTenure(context: Context, months: Int, wasYears: Boolean): String =
        if (wasYears) {
            val years = months / 12
            context.resources.getQuantityStringSafe(years) {
                if (years == 1) context.getString(R.string.ln_history_value_year, 1)
                else context.getString(R.string.ln_history_value_years, years)
            }
        } else {
            context.getString(R.string.ln_history_value_months, months)
        }

    /** Tiny inline helper so we can branch on count without juggling plurals XML. */
    private inline fun android.content.res.Resources.getQuantityStringSafe(
        @Suppress("UNUSED_PARAMETER") count: Int,
        block: () -> String
    ): String = block()

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<HistoryEntry>() {
            override fun areItemsTheSame(o: HistoryEntry, n: HistoryEntry) = o.id == n.id
            override fun areContentsTheSame(o: HistoryEntry, n: HistoryEntry) = o == n
        }
    }
}
