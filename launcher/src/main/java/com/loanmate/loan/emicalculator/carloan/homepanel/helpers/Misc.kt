package com.loanmate.loan.emicalculator.carloan.homepanel.helpers

import android.content.pm.ShortcutInfo

fun ShortcutInfo?.getLabel() = this?.longLabel?.toString().ifNullOrBlank { this?.shortLabel?.toString() } ?: ""

private fun String?.ifNullOrBlank(block: () -> String?) = this?.ifEmpty { block() } ?: block()
