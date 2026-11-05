package com.loanmate.loan.emicalculator.carloan.homelloan.loan.tabs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.loanmate.loan.emicalculator.carloan.homelloan.R
import com.loanmate.loan.emicalculator.carloan.homelloan.loan.data.BankCard
import com.loanmate.loan.emicalculator.carloan.homelloan.loan.data.CalcCard
import com.loanmate.loan.emicalculator.carloan.homelloan.loan.data.CalcKind
import com.loanmate.loan.emicalculator.carloan.homelloan.databinding.LnFragmentHomeBinding
import com.loanmate.loan.emicalculator.carloan.homelloan.databinding.LnItemBankingGridBinding
import com.loanmate.loan.emicalculator.carloan.homelloan.databinding.LnItemCalcTileBinding
import com.loanmate.loan.emicalculator.carloan.homelloan.loan.LoanViewModelFactory
import android.content.Intent
import com.loanmate.loan.emicalculator.carloan.homelloan.loan.calc.CalcPlaceholderActivity
import com.loanmate.loan.emicalculator.carloan.homelloan.loan.calc.LoanCalculatorActivity
import com.loanmate.loan.emicalculator.carloan.homelloan.loan.calc.FreedomCountdownActivity

class LoanHomeFragment : Fragment() {

    private var _binding: LnFragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LoanHomeViewModel by viewModels {
        LoanViewModelFactory(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = LnFragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initListeners()
        initObservers()
    }

    private fun initListeners() {
        binding.btnPromoCta.setOnClickListener {
            startActivity(Intent(requireContext(), FreedomCountdownActivity::class.java))
        }
    }

    private fun initObservers() {
        viewModel.calcTiles.observe(viewLifecycleOwner) { tiles ->
            val targets = listOf(
                binding.tileEmi,
                binding.tileBusiness,
                binding.tileAuto,
                binding.tileHomeLoan
            )
            targets.zip(tiles).forEach { (b, tile) -> bindCalcTile(b, tile) }
        }

        viewModel.bankingTiles.observe(viewLifecycleOwner) { tiles ->
            val targets = listOf(
                binding.gridRd,
                binding.gridFd,
                binding.gridRoi,
                binding.gridPpf,
                binding.gridCompound,
                binding.gridCash,
                binding.gridQuiz,
                binding.gridPlay
            )
            targets.zip(tiles).forEach { (b, tile) -> bindBankingTile(b, tile) }
        }
    }

    private fun bindCalcTile(b: LnItemCalcTileBinding, tile: CalcCard) {
        b.tvLabel.setText(tile.labelRes)
        b.tvSubtitle.setText(tile.subtitleRes)
        b.ivIcon.setImageResource(tile.iconRes)
        b.ivIcon.contentDescription = getString(tile.labelRes)
        b.tileRoot.setOnClickListener { route(tile.id, tile.labelRes, tile.iconRes) }
    }

    private fun bindBankingTile(b: LnItemBankingGridBinding, tile: BankCard) {
        b.tvLabel.setText(tile.labelRes)
        b.iconHolder.setBackgroundResource(tile.backgroundRes)
        b.ivIcon.setImageResource(tile.iconRes)
        b.ivIcon.contentDescription = getString(tile.labelRes)
        b.itemRoot.setOnClickListener { route(tile.id, tile.labelRes, tile.iconRes) }
    }

    /**
     * Tiles that map to a real numeric calculator launch [LoanCalculatorActivity];
     * everything else (cash converter / quizzes / promo) opens the placeholder.
     */
    private fun route(tileId: String, titleRes: Int, iconRes: Int) {
        val type = tileIdToCalculatorType[tileId]
        if (type != null) {
            startActivity(LoanCalculatorActivity.newIntent(requireContext(), type))
        } else {
            startActivity(
                CalcPlaceholderActivity.newIntent(requireContext(), titleRes, iconRes)
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        /**
         * Single source of truth mapping tile ids (defined in HomeRepo)
         * to a real CalcKind. Tiles that do not appear here fall through
         * to the placeholder destination.
         */
        private val tileIdToCalculatorType: Map<String, CalcKind> = mapOf(
            // Top calc tiles
            "emi" to CalcKind.LOAN_EMI,
            "business" to CalcKind.BUSINESS_LOAN,
            "auto" to CalcKind.AUTO_LOAN,
            "home_loan" to CalcKind.HOME_LOAN,
            // Banking grid
            "rd" to CalcKind.RD,
            "fd" to CalcKind.FD,
            "roi" to CalcKind.ROI,
            "ppf" to CalcKind.PPF,
            "compound" to CalcKind.COMPOUND,
            "cash" to CalcKind.CASH
            // "quiz", "play" intentionally absent → placeholder
        )
    }
}
