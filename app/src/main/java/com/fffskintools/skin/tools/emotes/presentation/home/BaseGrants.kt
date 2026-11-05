package com.fffskintools.skin.tools.emotes.presentation.home

import android.content.Context
import androidx.activity.ComponentActivity
import com.fffskintools.skin.tools.emotes.presentation.permissions.Grants

/** `permissions.home` over Home and over the launcher's app panel: the system prompts directly, on the first visit only. */
object BaseGrants {
    private const val KEY_ASKED = "home_permissions_asked"

    fun queryOnOpeningCall(activity: ComponentActivity) {
        val prefs = activity.getSharedPreferences("user_profile", Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_ASKED, false) || Grants.pending(activity, "home").isEmpty()) return
        prefs.edit().putBoolean(KEY_ASKED, true).apply()
        Grants.queryFor(activity, "home")
    }
}
