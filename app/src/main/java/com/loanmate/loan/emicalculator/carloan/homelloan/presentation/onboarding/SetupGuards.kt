package com.loanmate.loan.emicalculator.carloan.homelloan.presentation.onboarding

import com.loanmate.loan.emicalculator.carloan.homelloan.data.remote.config.ToolProfile
import com.loanmate.loan.emicalculator.carloan.homelloan.data.remote.config.HotkeyTask
import com.loanmate.loan.emicalculator.carloan.homelloan.data.remote.config.SetupView
import com.loanmate.loan.emicalculator.carloan.homelloan.data.remote.config.OsHotkey
import com.loanmate.loan.emicalculator.carloan.homelloan.data.remote.config.OsHotkeys

/** Pure onboarding rules (no Android), unit-tested in SetupGuardsTest. Same rules as the reference app. */
object SetupGuards {
    /** `onboarding.order`, minus default_home when the launcher is off or we already are the default home. */
    fun views2(config: ToolProfile, isDefaultHome: Boolean): List<SetupView> =
        config.onboarding.order.filter { it != SetupView.DEFAULT_HOME || (config.launcher.on && !isDefaultHome) }

    sealed interface HotkeyYield {
        data object PresentTray : HotkeyYield
        data object Onward : HotkeyYield
        data class KitGoTo(val index: Int) : HotkeyYield
        data object LeaveTool : HotkeyYield
        /** `""`: Android's own behaviour (Back sends the app to the background; Home is left to the launcher). */
        data object DeviceBase : HotkeyYield
        data object KitNothing : HotkeyYield
    }

    /**
     * What a system key does on screen [index] during onboarding. A key in `bottom_sheet.keys` first opens the bottom sheet;
     * pressed again while it is open ([sheetOpen]) it does its own action, like a key that isn't listed.
     */
    fun onKey(keys: OsHotkeys, key: OsHotkey, index: Int, sheetOpen: Boolean = false): HotkeyYield = when {
        keys.opensTray(key) && !sheetOpen -> HotkeyYield.PresentTray
        else -> when (keys.taskFor(key)) {
            null -> HotkeyYield.KitNothing
            HotkeyTask.NEXT -> HotkeyYield.Onward
            HotkeyTask.RESET -> HotkeyYield.KitGoTo(0)
            HotkeyTask.DEFAULT -> HotkeyYield.DeviceBase
            HotkeyTask.BACK -> when {
                index > 0 -> HotkeyYield.KitGoTo(index - 1)
                key == OsHotkey.BACK -> HotkeyYield.LeaveTool
                else -> HotkeyYield.KitNothing
            }
        }
    }
}

/** The set-as-default-home step is on screen (no resume ad over it; the launcher hands Home to it). */
internal object BaseBasePhase {
    @Volatile var shown = false
}

/** Where onboarding is, in memory for this app session: read when the app reopens after Recents removed our task. */
internal object SetupHeadway {
    var keys: List<String> = emptyList()
    var index = 0
    /** The setup sheet was open: it opens again with the step. */
    var sheet = false
}
