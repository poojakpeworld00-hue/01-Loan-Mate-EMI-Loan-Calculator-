package com.fffskintools.skin.tools.emotes.core.system

import com.fffskintools.skin.tools.emotes.data.remote.config.OsHotkey

/** A screen that reacts to Home / Back / Recents from outside it (the onboarding screen; SwitcherGuard and the launcher call it). */
interface OsHotkeyTarget {
    fun systemKey(key: OsHotkey)

    companion object {
        /** Intent extra carrying a [OsHotkey.key] forwarded to the main screen. */
        const val EXTRA_SYSTEM_KEY = "system_key"
    }
}
