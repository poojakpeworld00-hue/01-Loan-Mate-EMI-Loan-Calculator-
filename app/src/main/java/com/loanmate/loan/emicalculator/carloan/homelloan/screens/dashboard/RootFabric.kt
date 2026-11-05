package com.loanmate.loan.emicalculator.carloan.homelloan.screens.dashboard

import android.app.Activity
import androidx.fragment.app.FragmentManager
import com.loanmate.loan.emicalculator.carloan.adkit.Spots
import com.loanmate.loan.emicalculator.carloan.adkit.metrics.MetricsTwo
import com.loanmate.loan.emicalculator.carloan.homelloan.R
import com.loanmate.loan.emicalculator.carloan.homelloan.databinding.ScreenBaseBinding
import com.loanmate.loan.emicalculator.carloan.homelloan.loan.tabs.LoanTabs

/**
 * The Home screen's content: the loan tabs and the pinned bottom ad. Shared by [RootActivity] and the launcher's
 * swipe-right panel (HostSheetFragment), each passing its own [fragments] manager.
 */
class RootFabric(
    private val activity: Activity,
    private val b: ScreenBaseBinding,
    private val fragments: FragmentManager,
    /** RC place for the bottom ad: `screens.home`, or `launcher.host_app_panel` inside the launcher. */
    private val adPlace: String = PLACE,
) {
    private var tab = LoanTabs.HOME

    fun bind() {
        val nav = b.navBarVw
        listOf(nav.navHome to LoanTabs.HOME, nav.navTools to LoanTabs.TOOLS, nav.navHistory to LoanTabs.HISTORY, nav.navSetting to LoanTabs.SETTINGS)
            .forEach { (item, t) -> item.setOnClickListener { MetricsTwo.click(TAB_KEYS[t]); show(t) } }
        // A recreated host keeps its restored tab; a fresh one starts on Home.
        tab = (fragments.findFragmentById(R.id.tabHostVw)?.tag?.toIntOrNull()) ?: LoanTabs.HOME.also { show(it, force = true) }
        LoanTabs.paintNav(nav, tab)
        Spots.bottom(activity, b.adSlotVw, adPlace)
    }

    /** Back on another tab returns to Home first; true when it did. */
    fun back(): Boolean {
        if (tab == LoanTabs.HOME) return false
        show(LoanTabs.HOME)
        return true
    }

    private fun show(t: Int, force: Boolean = false) {
        if (t == tab && !force) return
        tab = t
        fragments.beginTransaction().replace(R.id.tabHostVw, LoanTabs.fragment(t), t.toString()).commitAllowingStateLoss()
        LoanTabs.paintNav(b.navBarVw, t)
    }

    companion object {
        const val PLACE = "screens.home"
        private val TAB_KEYS = listOf("tab_home", "tab_tools", "tab_history", "tab_settings")
    }
}
