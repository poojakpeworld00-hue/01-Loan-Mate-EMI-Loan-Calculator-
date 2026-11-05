package com.loanmate.loan.emicalculator.carloan.homepanel.widgetry

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.os.Bundle
import android.text.format.DateFormat
import android.view.View
import android.widget.RemoteViews
import com.loanmate.loan.emicalculator.carloan.homepanel.R

class NumericTimeGadgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        appWidgetIds.forEach { reloadGadget(context, appWidgetManager, it) }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle?,
    ) {
        reloadGadget(context, appWidgetManager, appWidgetId)
    }

    private fun reloadGadget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
    ) {
        val textColor = context.pullGadgetLabelTint(appWidgetId)
        val is24Hours = DateFormat.is24HourFormat(context)

        val views = RemoteViews(context.packageName, R.layout.hp_gadget_digital_clock).apply {
            setInt(
                R.id.widget_holderIxVw,
                "setBackgroundColor",
                context.pullGadgetBackdropTint(appWidgetId)
            )

            setViewVisibility(R.id.widget_name_timer_24IxVw, if (is24Hours) View.VISIBLE else View.GONE)
            setViewVisibility(R.id.widget_name_timer_12IxVw, if (is24Hours) View.GONE else View.VISIBLE)

            setTextColor(R.id.widget_name_timer_24IxVw, textColor)
            setTextColor(R.id.widget_name_timer_12IxVw, textColor)
            setTextColor(R.id.widget_dayIxVw, textColor)
        }

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}
