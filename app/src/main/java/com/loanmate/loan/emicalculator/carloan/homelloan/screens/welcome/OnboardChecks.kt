package com.loanmate.loan.emicalculator.carloan.homelloan.screens.welcome

import com.loanmate.loan.emicalculator.carloan.homelloan.store.cloud.flags.LoanSpec
import com.loanmate.loan.emicalculator.carloan.homelloan.store.cloud.flags.ShortcutJob
import com.loanmate.loan.emicalculator.carloan.homelloan.store.cloud.flags.OnboardScreen
import com.loanmate.loan.emicalculator.carloan.homelloan.store.cloud.flags.SysShortcut
import com.loanmate.loan.emicalculator.carloan.homelloan.store.cloud.flags.SysShortcuts

/** Pure onboarding rules (no Android), unit-tested in OnboardChecksTest. Same rules as the reference app. */
object OnboardChecks {
    /** `onboarding.order`, minus default_home when the launcher is off or we already are the default home. */
    fun views2(config: LoanSpec, isDefaultHome: Boolean): List<OnboardScreen> =
        config.onboarding.order.filter { it != OnboardScreen.DEFAULT_HOME || (config.launcher.on && !isDefaultHome) }

    sealed interface ShortcutResult {
        data object ShowDock : ShortcutResult
        data object Onward : ShortcutResult
        data class PackGoTo(val index: Int) : ShortcutResult
        data object QuitLoan : ShortcutResult
        /** `""`: Android's own behaviour (Back sends the app to the background; Home is left to the launcher). */
        data object GearRoot : ShortcutResult
        data object PackNothing : ShortcutResult
    }

    /**
     * What a system key does on screen [index] during onboarding. A key in `bottom_sheet.keys` first opens the bottom sheet;
     * pressed again while it is open ([sheetOpen]) it does its own action, like a key that isn't listed.
     */
    fun onKey(keys: SysShortcuts, key: SysShortcut, index: Int, sheetOpen: Boolean = false): ShortcutResult = when {
        keys.opensDock(key) && !sheetOpen -> ShortcutResult.ShowDock
        else -> when (keys.taskFor(key)) {
            null -> ShortcutResult.PackNothing
            ShortcutJob.NEXT -> ShortcutResult.Onward
            ShortcutJob.RESET -> ShortcutResult.PackGoTo(0)
            ShortcutJob.DEFAULT -> ShortcutResult.GearRoot
            ShortcutJob.BACK -> when {
                index > 0 -> ShortcutResult.PackGoTo(index - 1)
                key == SysShortcut.BACK -> ShortcutResult.QuitLoan
                else -> ShortcutResult.PackNothing
            }
        }
    }
}

/** The set-as-default-home step is on screen (no resume ad over it; the launcher hands Home to it). */
internal object RootRootStep {
    @Volatile var shown = false
}

/** Where onboarding is, in memory for this app session: read when the app reopens after Recents removed our task. */
internal object OnboardProgress {
    var keys: List<String> = emptyList()
    var index = 0
    /** The setup sheet was open: it opens again with the step. */
    var sheet = false
}
