package com.loanmate.loan.emicalculator.carloan.homelloan.presentation.common

import android.content.Context
import androidx.annotation.StringRes

/** Text a ViewModel can produce without a Context: a string resource or content text. */
sealed interface UiCaption {
    data class KitRes(@StringRes val id: Int) : UiCaption
    data class KitPlain(val text: String) : UiCaption

    fun resolve(context: Context): String = when (this) {
        is KitRes -> context.getString(id)
        is KitPlain -> text
    }
}
