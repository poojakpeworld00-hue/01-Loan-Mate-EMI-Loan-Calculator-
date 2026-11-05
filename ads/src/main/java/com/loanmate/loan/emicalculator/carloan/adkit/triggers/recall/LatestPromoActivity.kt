package com.loanmate.loan.emicalculator.carloan.adkit.triggers.recall

import android.view.LayoutInflater
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.loanmate.loan.emicalculator.carloan.adkit.Spots
import com.loanmate.loan.emicalculator.carloan.adkit.metrics.MetricsTwo
import com.loanmate.loan.emicalculator.carloan.adkit.databinding.PgRecentSpotBinding
import com.loanmate.loan.emicalculator.carloan.adkit.triggers.PackTraitFrameActivity

/** The ad screen a Recents press leads back to (RC `launcher.play_store_return`: bottom_ad, exit_ad on close). */
class LatestPromoActivity : PackTraitFrameActivity<PgRecentSpotBinding>() {

    override fun inflateBinding(inflater: LayoutInflater) = PgRecentSpotBinding.inflate(inflater)

    override val screenName = "recents_event"

    private var leaving = false

    override fun initView() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        applyInsets()
        binding.recentShimmerVw.visibility = View.GONE // FootPromoFetcher brings its own shimmer
        Spots.bottom(this, binding.recentAdFrameVw, Spots.PLAY_STORE_RETURN)
        binding.recentContinueVw.setOnClickListener {
            MetricsTwo.click("continue")
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
