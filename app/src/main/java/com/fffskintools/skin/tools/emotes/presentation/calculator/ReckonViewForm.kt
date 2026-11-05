package com.fffskintools.skin.tools.emotes.presentation.calculator

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.fffskintools.skin.tools.emotes.R
import com.fffskintools.skin.tools.emotes.domain.model.ReckonYield
import com.fffskintools.skin.tools.emotes.domain.model.ReckonerSort
import com.fffskintools.skin.tools.emotes.domain.usecase.WorkApplyTask
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class ReckonUiMode(val result: ReckonYield, @StringRes val error: Int? = null)

class ReckonViewForm(private val calculate: WorkApplyTask, args: SavedStateHandle) : ViewModel() {
    val type: ReckonerSort = ReckonerSort.entries[args[ReckonActivity.EXTRA_CALC] ?: 0]

    private val _state = MutableStateFlow(ReckonUiMode(calculate.compute(type, 0.0)))
    val state: StateFlow<ReckonUiMode> = _state

    fun onWork(input: String) {
        _state.value = when (val outcome = calculate(type, input)) {
            WorkApplyTask.KitOutcome.KitEmpty -> _state.value.copy(error = R.string.calc_error_empty)
            WorkApplyTask.KitOutcome.KitInvalid -> _state.value.copy(error = R.string.calc_error_invalid)
            is WorkApplyTask.KitOutcome.KitSuccess -> ReckonUiMode(outcome.result)
        }
    }
}
