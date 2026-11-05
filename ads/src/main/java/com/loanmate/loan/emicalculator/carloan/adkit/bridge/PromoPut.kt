package com.loanmate.loan.emicalculator.carloan.adkit.bridge

import android.content.Context
import android.util.Log
import com.loanmate.loan.emicalculator.carloan.adkit.BuildConfig

/** Debug-only ad engine log. Never logs config values: RC carries secrets. */
object PromoPut {
    private const val TAG = "AdEngine"

    fun d(area: String, message: String) {
        if (BuildConfig.DEBUG) Log.d(TAG, "[$area] $message")
    }

    @Suppress("UNUSED_PARAMETER")
    fun read(source: String, key: String?, value: Any?) = Unit

    @Suppress("UNUSED_PARAMETER")
    fun dumpBox(context: Context, reason: String) = d("VAULT", reason)
}
