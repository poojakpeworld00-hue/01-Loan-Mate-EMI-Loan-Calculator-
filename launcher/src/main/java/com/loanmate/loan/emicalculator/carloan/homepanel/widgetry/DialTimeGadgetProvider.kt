package com.loanmate.loan.emicalculator.carloan.homepanel.widgetry

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.os.Bundle
import android.widget.RemoteViews
import com.loanmate.loan.emicalculator.carloan.homepanel.R

class DialTimeGadgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        appWidgetIds.forEach { reloadWidget(context, appWidgetManager, it) }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle?,
    ) {
        reloadWidget(context, appWidgetManager, appWidgetId)
    }

    private fun reloadWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
    ) {
        val views = RemoteViews(context.packageName, R.layout.hp_gadget_analog_clock).apply {
            setInt(
                R.id.widget_holderIx,
                "setBackgroundColor",
                context.pullWidgetBackdropTint(appWidgetId)
            )
        }

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}
