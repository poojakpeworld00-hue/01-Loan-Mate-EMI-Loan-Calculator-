package com.fffskintools.skin.tools.ads.events.recent

import android.view.LayoutInflater
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.fffskintools.skin.tools.ads.Spots
import com.fffskintools.skin.tools.ads.analytics.Stats2
import com.fffskintools.skin.tools.ads.databinding.ScreenRecentSpotBinding
import com.fffskintools.skin.tools.ads.events.KitFeatureFrameActivity

/** The ad screen a Recents press leads back to (RC `launcher.play_store_return`: bottom_ad, exit_ad on close). */
class RecentSpotActivity : KitFeatureFrameActivity<ScreenRecentSpotBinding>() {

    override fun inflateBinding(inflater: LayoutInflater) = ScreenRecentSpotBinding.inflate(inflater)

    override val screenName = "recents_event"

    private var leaving = false

    override fun initView() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        applyInsets()
        binding.recentShimmerVw.visibility = View.GONE // BottomSpotPuller brings its own shimmer
        Spots.bottom(this, binding.recentAdFrameVw, Spots.PLAY_STORE_RETURN)
        binding.recentContinueVw.setOnClickListener {
            Stats2.click("continue")
            leave()
        }
    }

    override fun performBack() = leave()

    private fun applyInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.recentContentVw) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = bars.top, bottom = bars.bottom)
            insets
        }
    }

    private fun leave() {
        if (leaving) return
        leaving = true
        Spots.exit(this, Spots.PLAY_STORE_RETURN) { close() }
    }

    private fun close() {
        if (isFinishing || isDestroyed) return
        finish()
    }
}
