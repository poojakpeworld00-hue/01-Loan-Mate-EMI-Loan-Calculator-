package com.loanmate.loan.emicalculator.carloan.homepanel.visual

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetProviderInfo
import android.content.Context

class TailoredWidgetGadgetKeeper(context: Context, hostId: Int) : AppWidgetHost(context, hostId) {
    override fun onCreateView(context: Context, appWidgetId: Int, appWidget: AppWidgetProviderInfo): AppWidgetHostView {
        return TailoredWidgetGadgetKeeperPanel(context)
    }
}
