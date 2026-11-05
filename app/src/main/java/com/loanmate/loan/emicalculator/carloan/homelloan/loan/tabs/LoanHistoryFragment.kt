package com.loanmate.loan.emicalculator.carloan.homelloan.loan.tabs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.loanmate.loan.emicalculator.carloan.homelloan.R
import com.loanmate.loan.emicalculator.carloan.homelloan.loan.data.CalcKind
import com.loanmate.loan.emicalculator.carloan.homelloan.loan.data.HistoryEntry
import com.loanmate.loan.emicalculator.carloan.homelloan.loan.data.CurrencyRepo
import com.loanmate.loan.emicalculator.carloan.homelloan.databinding.LnFragmentHistoryBinding
import com.loanmate.loan.emicalculator.carloan.homelloan.loan.LoanViewModelFactory
import com.loanmate.loan.emicalculator.carloan.homelloan.loan.calc.LoanCalculatorActivity
import com.loanmate.loan.emicalculator.carloan.homelloan.loan.data.LoanPrefs

class LoanHistoryFragment : Fragment() {

    private var _binding: LnFragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LoanHistoryViewModel by viewModels {
        LoanViewModelFactory(requireContext())
    }

    private lateinit var adapter: LoanHistoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = LnFragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = LoanHistoryAdapter(
            currencySymbolProvider = { resolveCurrencySymbol() },
            onView = ::openCalculator,
            onDelete = ::confirmDelete
        )
        binding.rvHistory.layoutManager = LinearLayoutManager(requireContext())
        binding.rvHistory.adapter = adapter

        viewModel.items.observe(viewLifecycleOwner) { items -> render(items) }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }

    private fun render(items: List<HistoryEntry>) {
        if (items.isEmpty()) {
            binding.listGroup.visibility = View.GONE
            binding.emptyGroup.visibility = View.VISIBLE
        } else {
            binding.emptyGroup.visibility = View.GONE
            binding.listGroup.visibility = View.VISIBLE
            binding.tvHeaderCount.text = if (items.size == 1)
                getString(R.string.ln_history_items_count_one)
            else
                getString(R.string.ln_history_items_count, items.size)
            adapter.submitList(items)
        }
    }

    private fun openCalculator(item: HistoryEntry) {
        val type = CalcKind.fromIdOrNull(item.calculatorTypeName) ?: return
        startActivity(LoanCalculatorActivity.newIntent(requireContext(), type, item))
    }

    private fun confirmDelete(item: HistoryEntry) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.ln_history_delete_confirm_title)
            .setMessage(R.string.ln_history_delete_confirm_message)
            .setNegativeButton(R.string.ln_history_action_cancel, null)
            .setPositiveButton(R.string.ln_history_action_delete) { _, _ ->
                viewModel.delete(item.id)
            }
            .show()
    }

    private fun resolveCurrencySymbol(): String {
        val ctx = requireContext()
        val pref = LoanPrefs(ctx)
        val code = pref.selectedCurrency.ifEmpty { "USD" }
        val item = CurrencyRepo(pref).getAll().firstOrNull { it.code == code }
        return item?.symbolRes?.let { ctx.getString(it) } ?: "$"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
