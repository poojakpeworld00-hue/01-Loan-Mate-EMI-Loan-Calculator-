package com.loanmate.loan.emicalculator.carloan.homelloan.screens.emi

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.loanmate.loan.emicalculator.carloan.homelloan.R
import com.loanmate.loan.emicalculator.carloan.homelloan.logic.entity.ComputeResult
import com.loanmate.loan.emicalculator.carloan.homelloan.logic.entity.CalculatorOrder
import com.loanmate.loan.emicalculator.carloan.homelloan.logic.cases.LaborCommitJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class ComputeLookStyle(val result: ComputeResult, @StringRes val error: Int? = null)

class ComputeScreenModel(private val calculate: LaborCommitJob, args: SavedStateHandle) : ViewModel() {
    val type: CalculatorOrder = CalculatorOrder.entries[args[ComputeActivity.EXTRA_CALC] ?: 0]

    private val _state = MutableStateFlow(ComputeLookStyle(calculate.compute(type, 0.0)))
    val state: StateFlow<ComputeLookStyle> = _state

    fun onLabor(input: String) {
        _state.value = when (val outcome = calculate(type, input)) {
            LaborCommitJob.PackResult.PackBlank -> _state.value.copy(error = R.string.calc_error_empty)
            LaborCommitJob.PackResult.PackBroken -> _state.value.copy(error = R.string.calc_error_invalid)
            is LaborCommitJob.PackResult.PackWin -> ComputeLookStyle(outcome.result)
        }
    }
}
