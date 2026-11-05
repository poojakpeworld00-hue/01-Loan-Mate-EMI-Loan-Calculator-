package com.loanmate.loan.emicalculator.carloan.adkit.bridge

import android.app.Activity
import android.view.ViewTreeObserver

/** Runs [action] once the activity window has focus (port of QR ScannerApplication.onceWindowFocused). */
internal fun Activity.kitOnceWindowFocused(action: () -> Unit) {
    if (hasWindowFocus()) {
        action()
        return
    }
    val decorView = window.decorView
    decorView.viewTreeObserver.addOnWindowFocusChangeListener(
        object : ViewTreeObserver.OnWindowFocusChangeListener {
            override fun onWindowFocusChanged(hasFocus: Boolean) {
                if (!hasFocus) return
                decorView.viewTreeObserver.removeOnWindowFocusChangeListener(this)
                action()
            }
        }
    )
}
