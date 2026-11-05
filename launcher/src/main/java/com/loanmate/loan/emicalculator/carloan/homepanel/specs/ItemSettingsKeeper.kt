package com.loanmate.loan.emicalculator.carloan.homepanel.specs

import com.loanmate.loan.emicalculator.carloan.homepanel.models.DeskViewerGridItem

interface ItemSettingsKeeper {
    fun onAnyClick()
    fun hide(gridItem: DeskViewerGridItem)
    fun rename(gridItem: DeskViewerGridItem)
    fun resize(gridItem: DeskViewerGridItem)
    fun appInfoUi(gridItem: DeskViewerGridItem)
    fun remove(gridItem: DeskViewerGridItem)
    fun uninstall(gridItem: DeskViewerGridItem)
    fun onDismiss()
}

abstract class ItemSettingsKeeperJoiner : ItemSettingsKeeper {
    override fun onAnyClick() = Unit
    override fun hide(gridItem: DeskViewerGridItem) = Unit
    override fun rename(gridItem: DeskViewerGridItem) = Unit
    override fun resize(gridItem: DeskViewerGridItem) = Unit
    override fun appInfoUi(gridItem: DeskViewerGridItem) = Unit
    override fun remove(gridItem: DeskViewerGridItem) = Unit
    override fun uninstall(gridItem: DeskViewerGridItem) = Unit
    override fun onDismiss() = Unit
}
