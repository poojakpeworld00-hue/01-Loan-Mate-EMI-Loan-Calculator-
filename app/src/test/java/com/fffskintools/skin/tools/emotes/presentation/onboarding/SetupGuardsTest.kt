package com.fffskintools.skin.tools.emotes.presentation.onboarding

import com.fffskintools.skin.tools.launcher.api.LauncherSettings
import com.fffskintools.skin.tools.emotes.data.remote.config.ToolProfile
import com.fffskintools.skin.tools.emotes.data.remote.config.HotkeyTask
import com.fffskintools.skin.tools.emotes.data.remote.config.SetupProfile
import com.fffskintools.skin.tools.emotes.data.remote.config.SetupView.DEFAULT_HOME
import com.fffskintools.skin.tools.emotes.data.remote.config.SetupView.LANGUAGE
import com.fffskintools.skin.tools.emotes.data.remote.config.SetupView.DAILY
import com.fffskintools.skin.tools.emotes.data.remote.config.OsHotkey
import com.fffskintools.skin.tools.emotes.data.remote.config.OsHotkeys
import com.fffskintools.skin.tools.emotes.presentation.onboarding.SetupGuards.HotkeyYield
import org.junit.Assert.assertEquals
import org.junit.Test

class SetupGuardsTest {
    private val order = listOf(LANGUAGE, DEFAULT_HOME, DAILY)
    private fun config(launcherOn: Boolean) =
        ToolProfile(onboarding = SetupProfile(order = order), launcher = LauncherSettings(on = launcherOn))

    @Test fun baseBaseOnlyWhenDeckOnAndNotBase() {
        assertEquals(order, SetupGuards.views2(config(true), isDefaultHome = false))
        assertEquals(listOf(LANGUAGE, DAILY), SetupGuards.views2(config(true), isDefaultHome = true))
        assertEquals(listOf(LANGUAGE, DAILY), SetupGuards.views2(config(false), isDefaultHome = false))
    }

    @Test fun keys() {
        val keys = OsHotkeys(home = HotkeyTask.RESET, back = HotkeyTask.BACK, sheetOn = true, sheetKeys = setOf(OsHotkey.BACK))
        assertEquals(HotkeyYield.PresentTray, SetupGuards.onKey(keys, OsHotkey.BACK, 2))
        assertEquals(HotkeyYield.KitGoTo(0), SetupGuards.onKey(keys, OsHotkey.HOME, 2))
        assertEquals(HotkeyYield.KitGoTo(1), SetupGuards.onKey(keys, OsHotkey.BACK, 2, sheetOpen = true))
        assertEquals(HotkeyYield.LeaveTool, SetupGuards.onKey(keys, OsHotkey.BACK, 0, sheetOpen = true))
        assertEquals(HotkeyYield.Onward, SetupGuards.onKey(OsHotkeys(), OsHotkey.BACK, 0))
        val device = OsHotkeys(home = HotkeyTask.DEFAULT, back = HotkeyTask.DEFAULT, sheetOn = true, sheetKeys = setOf(OsHotkey.BACK))
        assertEquals(HotkeyYield.DeviceBase, SetupGuards.onKey(device, OsHotkey.HOME, 2))
        assertEquals(HotkeyYield.PresentTray, SetupGuards.onKey(device, OsHotkey.BACK, 2)) // a listed key still opens the sheet first
        assertEquals(HotkeyYield.DeviceBase, SetupGuards.onKey(device, OsHotkey.BACK, 2, sheetOpen = true))
        // sheet switched off: listed keys do their action
        assertEquals(HotkeyYield.Onward, SetupGuards.onKey(OsHotkeys(sheetKeys = setOf(OsHotkey.BACK)), OsHotkey.BACK, 0))
    }

    @Test fun switcherHotkey() {
        assertEquals(HotkeyYield.KitNothing, SetupGuards.onKey(OsHotkeys(), OsHotkey.RECENTS, 2)) // missing key: stays
        assertEquals(HotkeyYield.Onward, SetupGuards.onKey(OsHotkeys(recents = HotkeyTask.NEXT), OsHotkey.RECENTS, 2))
        assertEquals(HotkeyYield.KitGoTo(0), SetupGuards.onKey(OsHotkeys(recents = HotkeyTask.RESET), OsHotkey.RECENTS, 2))
        assertEquals(HotkeyYield.DeviceBase, SetupGuards.onKey(OsHotkeys(recents = HotkeyTask.DEFAULT), OsHotkey.RECENTS, 2))
        val sheet = OsHotkeys(recents = HotkeyTask.NEXT, sheetOn = true, sheetKeys = setOf(OsHotkey.RECENTS))
        assertEquals(HotkeyYield.PresentTray, SetupGuards.onKey(sheet, OsHotkey.RECENTS, 2))
        assertEquals(HotkeyYield.Onward, SetupGuards.onKey(sheet, OsHotkey.RECENTS, 2, sheetOpen = true))
    }

    @Test fun listedHotkey_opensTrayOpening_thenDoesItsTask() {
        val keys = OsHotkeys(home = HotkeyTask.NEXT, back = HotkeyTask.NEXT, sheetOn = true, sheetKeys = setOf(OsHotkey.BACK, OsHotkey.HOME))
        assertEquals(HotkeyYield.PresentTray, SetupGuards.onKey(keys, OsHotkey.BACK, 1))
        assertEquals(HotkeyYield.Onward, SetupGuards.onKey(keys, OsHotkey.BACK, 1, sheetOpen = true))
        assertEquals(HotkeyYield.Onward, SetupGuards.onKey(keys, OsHotkey.HOME, 1, sheetOpen = true)) // another listed key
        assertEquals(HotkeyYield.Onward, SetupGuards.onKey(keys.copy(sheetKeys = setOf(OsHotkey.HOME)), OsHotkey.BACK, 1)) // not listed: at once
        // empty list: every key does its own action at once
        assertEquals(HotkeyYield.Onward, SetupGuards.onKey(keys.copy(sheetKeys = emptySet()), OsHotkey.BACK, 1))
        // second press keeps the key's own action (back → previous screen)
        assertEquals(HotkeyYield.KitGoTo(0), SetupGuards.onKey(keys.copy(back = HotkeyTask.BACK), OsHotkey.BACK, 1, sheetOpen = true))
    }
}
