package com.loanmate.loan.emicalculator.carloan.homepanel.models

import android.appwidget.AppWidgetProviderInfo
import android.content.pm.ActivityInfo
import android.graphics.drawable.Drawable

data class WidgetGadget(
    var appPackageName: String,
    var appTitle: String,
    val appIcon: Drawable?,
    val widgetTitleUi: String,
    val widgetPreviewImage: Drawable?,
    var widthCells: Int,
    val heightCells: Int,
    val isShortcut: Boolean,
    val className: String,
    val providerInfo: AppWidgetProviderInfo?,
    val activityInfo: ActivityInfo?
) : GadgetsStackItem() {
    override fun getHashToCompare() = pullStringToCompare().hashCode()

    private fun pullStringToCompare(): String {
        return copy(appIcon = null, widgetPreviewImage = null).toString()
    }
}
