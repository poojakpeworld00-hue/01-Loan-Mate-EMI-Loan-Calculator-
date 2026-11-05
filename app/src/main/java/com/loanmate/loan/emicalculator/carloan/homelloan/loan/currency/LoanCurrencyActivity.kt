package com.loanmate.loan.emicalculator.carloan.homelloan.loan.currency

import androidx.activity.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.loanmate.loan.emicalculator.carloan.homelloan.screens.shared.kitEnableLightEdgeToEdge
import com.loanmate.loan.emicalculator.carloan.homelloan.screens.shared.padForSysBars
import com.loanmate.loan.emicalculator.carloan.homelloan.databinding.LnActivityCurrencyBinding
import com.loanmate.loan.emicalculator.carloan.homelloan.loan.LoanViewModelFactory

class LoanCurrencyActivity : AppCompatActivity() {

    private lateinit var binding: LnActivityCurrencyBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        kitEnableLightEdgeToEdge()
        binding = LnActivityCurrencyBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.padForSysBars()
        initView()
        initListeners()
        initObservers()
    }

    private val viewModel: LoanCurrencyViewModel by viewModels { LoanViewModelFactory(this) }

    private lateinit var adapter: LoanCurrencyAdapter

    private fun initView() {
        adapter = LoanCurrencyAdapter(viewModel.currencies) { position ->
            viewModel.select(position)
        }
        binding.rvCurrencies.layoutManager = LinearLayoutManager(this)
        binding.rvCurrencies.adapter = adapter
    }

    private fun initObservers() {
        viewModel.selectedIndex.observe(this) { index ->
            adapter.setSelectedIndex(index)
        }
    }

    private fun initListeners() {
        binding.btnNext.setOnClickListener {
            viewModel.confirmSelection()
            // The reference continued to its terms screen here; in Loan Mate this just saves and returns.
            setResult(RESULT_OK)
            finish()
        }
    }
}
