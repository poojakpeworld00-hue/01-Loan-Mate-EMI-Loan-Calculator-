package com.loanmate.loan.emicalculator.carloan.homelloan.loan.currency

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.loanmate.loan.emicalculator.carloan.homelloan.R
import com.loanmate.loan.emicalculator.carloan.homelloan.loan.data.CurrencyEntry
import com.loanmate.loan.emicalculator.carloan.homelloan.databinding.LnItemCurrencyBinding

class LoanCurrencyAdapter(
    private val items: List<CurrencyEntry>,
    private val onClick: (Int) -> Unit
) : RecyclerView.Adapter<LoanCurrencyAdapter.VH>() {

    private var selectedIndex: Int = 0

    fun setSelectedIndex(index: Int) {
        if (index == selectedIndex) return
        val previous = selectedIndex
        selectedIndex = index
        notifyItemChanged(previous)
        notifyItemChanged(selectedIndex)
    }

    inner class VH(val b: LnItemCurrencyBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(position: Int) {
            val item = items[position]
            val isSelected = position == selectedIndex
            b.tvCode.setText(item.codeRes)
            b.tvSymbol.setText(item.symbolRes)
            b.itemRoot.isActivated = isSelected
            b.ivRadio.isActivated = isSelected
            b.ivRadio.contentDescription = b.root.context.getString(
                if (isSelected) R.string.ln_cd_radio_selected else R.string.ln_cd_radio_unselected
            )
            b.itemRoot.setOnClickListener { onClick(position) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = LnItemCurrencyBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(position)
}
