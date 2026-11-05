package com.loanmate.loan.emicalculator.carloan.homelloan.screens.shared

import android.content.Context
import androidx.annotation.StringRes

/** Text a ViewModel can produce without a Context: a string resource or content text. */
sealed interface LookLabel {
    data class PackOut(@StringRes val id: Int) : LookLabel
    data class PackSimple(val text: String) : LookLabel

    fun resolve(context: Context): String = when (this) {
        is PackOut -> context.getString(id)
        is PackSimple -> text
    }
}
