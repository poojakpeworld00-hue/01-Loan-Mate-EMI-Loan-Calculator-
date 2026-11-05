package com.loanmate.loan.emicalculator.carloan.homelloan.screens.welcome

import com.loanmate.loan.emicalculator.carloan.homepanel.gateway.StarterConfig
import com.loanmate.loan.emicalculator.carloan.homelloan.store.cloud.flags.LoanSpec
import com.loanmate.loan.emicalculator.carloan.homelloan.store.cloud.flags.ShortcutJob
import com.loanmate.loan.emicalculator.carloan.homelloan.store.cloud.flags.OnboardSpec
import com.loanmate.loan.emicalculator.carloan.homelloan.store.cloud.flags.OnboardScreen.DEFAULT_HOME
import com.loanmate.loan.emicalculator.carloan.homelloan.store.cloud.flags.OnboardScreen.LANGUAGE
import com.loanmate.loan.emicalculator.carloan.homelloan.store.cloud.flags.OnboardScreen.EXISTING_EMI
import com.loanmate.loan.emicalculator.carloan.homelloan.store.cloud.flags.SysShortcut
import com.loanmate.loan.emicalculator.carloan.homelloan.store.cloud.flags.SysShortcuts
import com.loanmate.loan.emicalculator.carloan.homelloan.screens.welcome.OnboardChecks.ShortcutResult
import org.junit.Assert.assertEquals
import org.junit.Test

class OnboardChecksTest {
    private val order = listOf(LANGUAGE, DEFAULT_HOME, EXISTING_EMI)
    private fun config(launcherOn: Boolean) =
        LoanSpec(onboarding = OnboardSpec(order = order), launcher = StarterConfig(on = launcherOn))

    @Test fun baseBaseOnlyWhenDeckOnAndNotBase() {
        assertEquals(order, OnboardChecks.views2(config(true), isDefaultHome = false))
        assertEquals(listOf(LANGUAGE, EXISTING_EMI), OnboardChecks.views2(config(true), isDefaultHome = true))
        assertEquals(listOf(LANGUAGE, EXISTING_EMI), OnboardChecks.views2(config(false), isDefaultHome = false))
    }

    @Test fun keys() {
        val keys = SysShortcuts(home = ShortcutJob.RESET, back = ShortcutJob.BACK, sheetOn = true, sheetKeys = setOf(SysShortcut.BACK))
        assertEquals(ShortcutResult.ShowDock, OnboardChecks.onKey(keys, SysShortcut.BACK, 2))
        assertEquals(ShortcutResult.PackGoTo(0), OnboardChecks.onKey(keys, SysShortcut.HOME, 2))
        assertEquals(ShortcutResult.PackGoTo(1), OnboardChecks.onKey(keys, SysShortcut.BACK, 2, sheetOpen = true))
        assertEquals(ShortcutResult.QuitLoan, OnboardChecks.onKey(keys, SysShortcut.BACK, 0, sheetOpen = true))
        assertEquals(ShortcutResult.Onward, OnboardChecks.onKey(SysShortcuts(), SysShortcut.BACK, 0))
        val device = SysShortcuts(home = ShortcutJob.DEFAULT, back = ShortcutJob.DEFAULT, sheetOn = true, sheetKeys = setOf(SysShortcut.BACK))
        assertEquals(ShortcutResult.GearRoot, OnboardChecks.onKey(device, SysShortcut.HOME, 2))
        assertEquals(ShortcutResult.ShowDock, OnboardChecks.onKey(device, SysShortcut.BACK, 2)) // a listed key still opens the sheet first
        assertEquals(ShortcutResult.GearRoot, OnboardChecks.onKey(device, SysShortcut.BACK, 2, sheetOpen = true))
        // sheet switched off: listed keys do their action
        assertEquals(ShortcutResult.Onward, OnboardChecks.onKey(SysShortcuts(sheetKeys = setOf(SysShortcut.BACK)), SysShortcut.BACK, 0))
    }

    @Test fun switcherHotkey() {
        assertEquals(ShortcutResult.PackNothing, OnboardChecks.onKey(SysShortcuts(), SysShortcut.RECENTS, 2)) // missing key: stays
        assertEquals(ShortcutResult.Onward, OnboardChecks.onKey(SysShortcuts(recents = ShortcutJob.NEXT), SysShortcut.RECENTS, 2))
        assertEquals(ShortcutResult.PackGoTo(0), OnboardChecks.onKey(SysShortcuts(recents = ShortcutJob.RESET), SysShortcut.RECENTS, 2))
        assertEquals(ShortcutResult.GearRoot, OnboardChecks.onKey(SysShortcuts(recents = ShortcutJob.DEFAULT), SysShortcut.RECENTS, 2))
        val sheet = SysShortcuts(recents = ShortcutJob.NEXT, sheetOn = true, sheetKeys = setOf(SysShortcut.RECENTS))
        assertEquals(ShortcutResult.ShowDock, OnboardChecks.onKey(sheet, SysShortcut.RECENTS, 2))
        assertEquals(ShortcutResult.Onward, OnboardChecks.onKey(sheet, SysShortcut.RECENTS, 2, sheetOpen = true))
    }

    @Test fun listedHotkey_opensTrayOpening_thenDoesItsTask() {
        val keys = SysShortcuts(home = ShortcutJob.NEXT, back = ShortcutJob.NEXT, sheetOn = true, sheetKeys = setOf(SysShortcut.BACK, SysShortcut.HOME))
        assertEquals(ShortcutResult.ShowDock, OnboardChecks.onKey(keys, SysShortcut.BACK, 1))
        assertEquals(ShortcutResult.Onward, OnboardChecks.onKey(keys, SysShortcut.BACK, 1, sheetOpen = true))
        assertEquals(ShortcutResult.Onward, OnboardChecks.onKey(keys, SysShortcut.HOME, 1, sheetOpen = true)) // another listed key
        assertEquals(ShortcutResult.Onward, OnboardChecks.onKey(keys.copy(sheetKeys = setOf(SysShortcut.HOME)), SysShortcut.BACK, 1)) // not listed: at once
        // empty list: every key does its own action at once
        assertEquals(ShortcutResult.Onward, OnboardChecks.onKey(keys.copy(sheetKeys = emptySet()), SysShortcut.BACK, 1))
        // second press keeps the key's own action (back → previous screen)
        assertEquals(ShortcutResult.PackGoTo(0), OnboardChecks.onKey(keys.copy(back = ShortcutJob.BACK), SysShortcut.BACK, 1, sheetOpen = true))
    }
}
