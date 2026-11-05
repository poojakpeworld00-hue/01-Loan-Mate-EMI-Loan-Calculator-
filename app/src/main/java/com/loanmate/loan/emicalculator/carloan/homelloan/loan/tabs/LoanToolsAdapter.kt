package com.loanmate.loan.emicalculator.carloan.homelloan.loan.tabs

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.loanmate.loan.emicalculator.carloan.homelloan.loan.data.ToolEntry
import com.loanmate.loan.emicalculator.carloan.homelloan.databinding.LnItemToolCardBinding

class LoanToolsAdapter(
    private val items: List<ToolEntry>,
    private val onClick: (ToolEntry) -> Unit
) : RecyclerView.Adapter<LoanToolsAdapter.VH>() {

    inner class VH(val b: LnItemToolCardBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: ToolEntry) {
            b.tvTitle.setText(item.titleRes)
            b.tvSubtitle.setText(item.subtitleRes)
            b.ivIcon.setImageResource(item.iconRes)
            val title = b.root.context.getString(item.titleRes)
            b.ivIcon.contentDescription = title
            b.itemRoot.setOnClickListener { onClick(item) }
            b.btnArrow.setOnClickListener { onClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = LnItemToolCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])
}
