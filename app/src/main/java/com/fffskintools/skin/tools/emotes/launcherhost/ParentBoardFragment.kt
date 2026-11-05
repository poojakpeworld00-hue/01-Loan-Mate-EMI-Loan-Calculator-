package com.fffskintools.skin.tools.emotes.launcherhost

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.fffskintools.skin.tools.ads.analytics.Views
import com.fffskintools.skin.tools.emotes.databinding.PageBaseBinding
import com.fffskintools.skin.tools.emotes.presentation.common.padForOsBars
import com.fffskintools.skin.tools.emotes.presentation.home.BaseMaterial
import com.fffskintools.skin.tools.emotes.presentation.home.BaseGrants

/**
 * The app home inside the launcher's swipe-right panel. The launcher keeps this fragment at STARTED while the panel is
 * off screen and resumes it only once the slide-in has settled, so content and ads are bound on the first resume:
 * nothing loads (or counts as an impression) off screen.
 */
class ParentBoardFragment : Fragment() {
    private var binding: PageBaseBinding? = null
    private var shown = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        PageBaseBinding.inflate(inflater, container, false).also {
            binding = it
            it.screen.padForOsBars()
        }.root

    override fun onResume() {
        super.onResume()
        Views.show("host_app_panel")
        val b = binding ?: return
        if (!shown) {
            shown = true
            BaseMaterial(requireActivity(), b, adPlace = "launcher.host_app_panel").bind()
            BaseGrants.queryOnOpeningCall(requireActivity()) // first visit, one still missing
        }
    }

    override fun onDestroyView() {
        binding = null
        shown = false
        super.onDestroyView()
    }
}
