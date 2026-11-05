package com.loanmate.loan.emicalculator.carloan.homelloan.loan.terms

import androidx.activity.viewModels
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.loanmate.loan.emicalculator.carloan.homelloan.screens.shared.kitEnableLightEdgeToEdge
import com.loanmate.loan.emicalculator.carloan.homelloan.screens.shared.padForSysBars
import com.loanmate.loan.emicalculator.carloan.homelloan.databinding.LnActivityTermsBinding
import com.loanmate.loan.emicalculator.carloan.homelloan.loan.LoanViewModelFactory

class LoanTermsActivity : AppCompatActivity() {

    private lateinit var binding: LnActivityTermsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        kitEnableLightEdgeToEdge()
        binding = LnActivityTermsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.padForSysBars()
        initListeners()
        initObservers()
    }

    private val viewModel: LoanTermsViewModel by viewModels { LoanViewModelFactory(this) }

    private fun initListeners() {
        binding.cbAgree.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setAgreed(isChecked)
        }
        binding.btnAccept.setOnClickListener {
            if (viewModel.confirmAcceptance()) {
                // The reference went on to its onboarding here; in Loan Mate the caller decides what follows.
                setResult(RESULT_OK)
                finish()
            }
        }
    }

    private fun initObservers() {
        viewModel.agreed.observe(this) { agreed ->
            binding.btnAccept.isEnabled = agreed
            binding.btnAccept.isClickable = agreed
            binding.btnAccept.isFocusable = agreed
            binding.btnAccept.alpha = if (agreed) 1f else 0.5f
        }
    }
}
