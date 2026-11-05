package com.loanmate.loan.emicalculator.carloan.homelloan.kit.device

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import com.loanmate.loan.emicalculator.carloan.adkit.metrics.MetricsTwo

/** `key_press` for Home / Recents on any of our screens (app, launcher, event screens), while the app is in front. */
object SysShortcuts {
    fun listen(app: Application) {
        @Suppress("DEPRECATION") // still delivered to apps for Home/Recents
        ContextCompat.registerReceiver(app, object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (!ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) return
                when (intent.getStringExtra("reason")) {
                    "homekey" -> MetricsTwo.hotkeyPress("home")
                    "recentapps", "recent_apps" -> MetricsTwo.hotkeyPress("recents")
                }
            }
        }, IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS), ContextCompat.RECEIVER_EXPORTED)
    }
}
