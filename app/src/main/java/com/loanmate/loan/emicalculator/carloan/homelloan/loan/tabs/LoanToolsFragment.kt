package com.loanmate.loan.emicalculator.carloan.homelloan.loan.tabs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.loanmate.loan.emicalculator.carloan.homelloan.databinding.LnFragmentToolsBinding
import com.loanmate.loan.emicalculator.carloan.homelloan.loan.LoanViewModelFactory

class LoanToolsFragment : Fragment() {

    private var _binding: LnFragmentToolsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LoanToolsViewModel by viewModels {
        LoanViewModelFactory(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = LnFragmentToolsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvTools.layoutManager = LinearLayoutManager(requireContext())

        viewModel.tools.observe(viewLifecycleOwner) { tools ->
            binding.rvTools.adapter = LoanToolsAdapter(tools) { item ->
                Toast.makeText(
                    requireContext(),
                    getString(item.titleRes),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
