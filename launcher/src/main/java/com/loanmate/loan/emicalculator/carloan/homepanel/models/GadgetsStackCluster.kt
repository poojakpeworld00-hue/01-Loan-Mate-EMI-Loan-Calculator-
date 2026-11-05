package com.loanmate.loan.emicalculator.carloan.homepanel.models

import android.graphics.drawable.Drawable

data class GadgetsStackCluster(var appTitle: String, var appIcon: Drawable?) : GadgetsStackItem() {
    override fun getHashToCompare() = pullStringToCompare().hashCode()

    private fun pullStringToCompare(): String {
        return copy(appIcon = null).toString()
    }
}
