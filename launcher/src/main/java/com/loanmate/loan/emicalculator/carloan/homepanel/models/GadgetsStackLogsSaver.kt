package com.loanmate.loan.emicalculator.carloan.homepanel.models

open class GadgetsStackLogsSaver(val widgets: ArrayList<WidgetGadget>) : GadgetsStackItem() {
    override fun getHashToCompare() = widgets.sumOf { it.getHashToCompare() }
}
