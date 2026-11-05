package com.loanmate.loan.emicalculator.carloan.homelloan.kit.device

import com.loanmate.loan.emicalculator.carloan.homelloan.store.cloud.flags.SysShortcut

/** A screen that reacts to Home / Back / Recents from outside it (the onboarding screen; ToggleShield and the launcher call it). */
interface SysShortcutAim {
    fun systemKey(key: SysShortcut)

    companion object {
        /** Intent extra carrying a [SysShortcut.key] forwarded to the main screen. */
        const val EXTRA_SYSTEM_KEY = "system_key"
    }
}
