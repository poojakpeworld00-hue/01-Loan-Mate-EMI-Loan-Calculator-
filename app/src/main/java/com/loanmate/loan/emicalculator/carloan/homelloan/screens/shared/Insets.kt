package com.loanmate.loan.emicalculator.carloan.homelloan.screens.shared

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.activity.enableEdgeToEdge
import androidx.core.view.updatePadding

/** Pads this view by the system bars, for edge-to-edge activities. */
fun View.padForSysBars() {
    val start = paddingStart; val top = paddingTop; val end = paddingEnd; val bottom = paddingBottom
    ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
        val b = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        v.updatePadding(start + b.left, top + b.top, end + b.right, bottom + b.bottom)
        insets
    }
}

/** Edge-to-edge with dark status / navigation bar icons always: the app is light only. */
fun androidx.activity.ComponentActivity.kitEnableLightEdgeToEdge() {
    val bars = androidx.activity.SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
    enableEdgeToEdge(statusBarStyle = bars, navigationBarStyle = bars)
    window.maskNavBar()
}

/** LoanTwo-wide: the navigation bar stays hidden; a swipe from the edge shows it for a moment, then it hides again. */
fun android.view.Window.maskNavBar() {
    androidx.core.view.WindowCompat.getInsetsController(this, decorView).apply {
        systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        hide(WindowInsetsCompat.Type.navigationBars())
    }
}
