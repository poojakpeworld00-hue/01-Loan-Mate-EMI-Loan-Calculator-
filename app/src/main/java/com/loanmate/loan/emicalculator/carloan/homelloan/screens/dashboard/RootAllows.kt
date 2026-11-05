package com.loanmate.loan.emicalculator.carloan.homelloan.screens.dashboard

import android.content.Context
import androidx.activity.ComponentActivity
import com.loanmate.loan.emicalculator.carloan.homelloan.screens.access.Grants

/** `permissions.home` over Home and over the launcher's app panel: the system prompts directly, on the first visit only. */
object RootAllows {
    private const val KEY_ASKED = "home_permissions_asked"

    fun queryOnOpeningCall(activity: ComponentActivity) {
        val prefs = activity.getSharedPreferences("user_profile", Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_ASKED, false) || Grants.pending(activity, "home").isEmpty()) return
        prefs.edit().putBoolean(KEY_ASKED, true).apply()
        Grants.queryFor(activity, "home")
    }
}
