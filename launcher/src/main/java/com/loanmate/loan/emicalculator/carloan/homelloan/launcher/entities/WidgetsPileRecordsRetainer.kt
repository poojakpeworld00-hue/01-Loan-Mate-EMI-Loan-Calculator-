package com.loanmate.loan.emicalculator.carloan.homelloan.launcher.entities

open class WidgetsPileRecordsRetainer(val widgets: ArrayList<GadgetWidget>) : WidgetsPileDatum() {
    override fun getHashToCompare() = widgets.sumOf { it.getHashToCompare() }
}
