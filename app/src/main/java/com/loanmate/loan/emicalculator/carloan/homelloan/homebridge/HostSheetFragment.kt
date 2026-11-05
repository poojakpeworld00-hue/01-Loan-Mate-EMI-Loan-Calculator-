package com.loanmate.loan.emicalculator.carloan.homelloan.homebridge

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.loanmate.loan.emicalculator.carloan.adkit.metrics.Views
import com.loanmate.loan.emicalculator.carloan.homelloan.databinding.ScreenBaseBinding
import com.loanmate.loan.emicalculator.carloan.homelloan.screens.shared.padForSysBars
import com.loanmate.loan.emicalculator.carloan.homelloan.screens.dashboard.RootFabric
import com.loanmate.loan.emicalculator.carloan.homelloan.screens.dashboard.RootAllows

/**
 * The app home inside the launcher's swipe-right panel. The launcher keeps this fragment at STARTED while the panel is
 * off screen and resumes it only once the slide-in has settled, so content and ads are bound on the first resume:
 * nothing loads (or counts as an impression) off screen.
 */
class HostSheetFragment : Fragment() {
    private var binding: ScreenBaseBinding? = null
    private var shown = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        ScreenBaseBinding.inflate(inflater, container, false).also {
            binding = it
            it.screen.padForSysBars()
        }.root

    override fun onResume() {
        super.onResume()
        Views.show("host_app_panel")
        val b = binding ?: return
        if (!shown) {
            shown = true
            RootFabric(requireActivity(), b, childFragmentManager, adPlace = "launcher.host_app_panel").bind()
            RootAllows.queryOnLaunchCall(requireActivity()) // first visit, one still missing
        }
    }

    override fun onDestroyView() {
        binding = null
        shown = false
        super.onDestroyView()
    }
}
