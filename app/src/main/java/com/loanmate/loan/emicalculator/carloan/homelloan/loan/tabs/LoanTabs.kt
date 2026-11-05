package com.loanmate.loan.emicalculator.carloan.homelloan.loan.tabs

import android.util.TypedValue
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.children
import android.content.Intent
import androidx.fragment.app.Fragment
import com.loanmate.loan.emicalculator.carloan.adkit.Spots
import com.loanmate.loan.emicalculator.carloan.adkit.metrics.MetricsTwo
import com.loanmate.loan.emicalculator.carloan.homelloan.screens.dashboard.RootFabric
import com.loanmate.loan.emicalculator.carloan.homelloan.R
import com.loanmate.loan.emicalculator.carloan.homelloan.databinding.LnBottomNavBinding

/** Entry points for a host showing the loan tabs: 0 home, 1 tools, 2 history, 3 settings. */
object LoanTabs {
    const val HOME = 0
    const val TOOLS = 1
    const val HISTORY = 2
    const val SETTINGS = 3

    fun fragment(tab: Int): Fragment = when (tab) {
        TOOLS -> LoanToolsFragment()
        HISTORY -> LoanHistoryFragment()
        SETTINGS -> LoanSettingsFragment()
        else -> LoanHomeFragment()
    }

    /** Opens a screen from a tab the way the old home did: count the [button] click, run the `screens.home` exit ad, then go. */
    fun open(fragment: Fragment, button: String, intent: Intent) {
        val activity = fragment.requireActivity()
        MetricsTwo.click(button)
        Spots.exit(activity, RootFabric.PLACE) { activity.startActivity(intent) }
    }

    /** Highlights [tab] in an `ln_bottom_nav` bar (the reference's updateNavState). */
    fun paintNav(binding: LnBottomNavBinding, tab: Int) {
        styleNavItem(binding.navHome, tab == HOME)
        styleNavItem(binding.navTools, tab == TOOLS)
        styleNavItem(binding.navHistory, tab == HISTORY)
        styleNavItem(binding.navSetting, tab == SETTINGS)
    }

    private fun styleNavItem(item: LinearLayout, active: Boolean) {
        val ctx = item.context
        val colorRes = if (active) R.color.ln_brand_primary else R.color.ln_bottom_nav_inactive
        if (active) {
            item.setBackgroundResource(R.drawable.ln_bg_nav_pill)
        } else {
            // Inactive items keep touch feedback.
            val outValue = TypedValue()
            ctx.theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
            item.setBackgroundResource(outValue.resourceId)
        }
        item.children.forEach { child ->
            when (child) {
                is ImageView -> child.imageTintList = ContextCompat.getColorStateList(ctx, colorRes)
                is TextView -> child.setTextColor(ContextCompat.getColor(ctx, colorRes))
            }
        }
    }
}
