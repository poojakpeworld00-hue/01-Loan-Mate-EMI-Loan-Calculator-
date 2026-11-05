package com.loanmate.loan.emicalculator.carloan.homelloan.screens.shared

import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.loanmate.loan.emicalculator.carloan.homelloan.LoanTwo
import com.loanmate.loan.emicalculator.carloan.homelloan.inject.LoanCenter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * `by appViewModel { handle -> MyViewModel(repo, handle) }`: builds the screen's ViewModel from the [LoanCenter].
 * The SavedStateHandle carries the activity's intent extras and survives recreation (e.g. a language change).
 */
inline fun <reified VM : ViewModel> ComponentActivity.toolViewForm(
    crossinline create: LoanCenter.(SavedStateHandle) -> VM,
): Lazy<VM> = viewModels {
    viewModelFactory {
        initializer { (this@toolViewForm.application as LoanTwo).container.create(createSavedStateHandle()) }
    }
}

/** Collects [flow] while the screen is at least started. */
fun <T> LifecycleOwner.kitCollectWhenStarted(flow: Flow<T>, action: (T) -> Unit) {
    lifecycleScope.launch { repeatOnLifecycle(Lifecycle.State.STARTED) { flow.collect { action(it) } } }
}
