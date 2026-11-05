package com.fffskintools.skin.tools.launcher.contracts

import com.fffskintools.skin.tools.launcher.entities.GadgetHub

interface AllGadgetsGuardian {
    fun onAppLauncherLongPressed(x: Float, y: Float, appLauncher: GadgetHub)
}
