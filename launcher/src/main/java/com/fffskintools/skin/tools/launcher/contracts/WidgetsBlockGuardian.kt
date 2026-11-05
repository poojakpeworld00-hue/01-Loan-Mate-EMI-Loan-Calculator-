package com.fffskintools.skin.tools.launcher.contracts

import com.fffskintools.skin.tools.launcher.entities.GadgetWidget

interface WidgetsBlockGuardian {
    fun onWidgetLongPressed(appWidget: GadgetWidget)
}
