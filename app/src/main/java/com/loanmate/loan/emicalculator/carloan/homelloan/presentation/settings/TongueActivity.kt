package com.loanmate.loan.emicalculator.carloan.homelloan.presentation.settings

import com.loanmate.loan.emicalculator.carloan.homelloan.ads.Spots
import com.loanmate.loan.emicalculator.carloan.homelloan.ads.analytics.Stats2
import com.loanmate.loan.emicalculator.carloan.homelloan.ads.analytics.Views
import android.os.Bundle
import com.loanmate.loan.emicalculator.carloan.homelloan.presentation.common.kitEnableDarkEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.loanmate.loan.emicalculator.carloan.homelloan.R
import com.loanmate.loan.emicalculator.carloan.homelloan.databinding.PageTongueBinding
import com.loanmate.loan.emicalculator.carloan.homelloan.presentation.common.toolViewForm
import com.loanmate.loan.emicalculator.carloan.homelloan.presentation.common.kitCollectWhenStarted
import com.loanmate.loan.emicalculator.carloan.homelloan.presentation.common.padForOsBars
import com.loanmate.loan.emicalculator.carloan.homelloan.presentation.onboarding.PickAdapter
import com.loanmate.loan.emicalculator.carloan.homelloan.presentation.onboarding.tonguePhase

/** Tongue picker opened from Settings; Apply switches the app language and returns to Settings. */
class TongueActivity : AppCompatActivity() {
    private val viewModel by toolViewForm { handle -> TongueViewForm(settingsRepository, handle) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        kitEnableDarkEdgeToEdge()
        val b = PageTongueBinding.inflate(layoutInflater)
        setContentView(b.root)
        b.root.padForOsBars()
        b.header.back.setOnClickListener { finish() }
        b.header.title.setText(R.string.language)

        val adapter = PickAdapter(tonguePhase(viewModel.languages), setOf(viewModel.selected.value)) { viewModel.select(it) }
        b.list.layoutManager = LinearLayoutManager(this)
        b.list.adapter = adapter
        b.list.scrollToPosition(viewModel.selected.value)
        kitCollectWhenStarted(viewModel.selected) { adapter.kitSubmitSelection(setOf(it)) }

        b.apply.setOnClickListener {
            Stats2.click("apply", viewModel.languages[viewModel.selected.value].tag)
            Spots.exit(this, PLACE) {
                // Finish first: the locale change then recreates only Settings (and Home) behind us, in the new language.
                finish()
                viewModel.apply()
            }
        }
        Spots.bottom(this, b.adSlot, PLACE)
    }

    override fun onResume() {
        super.onResume()
        Views.show("language")
    }

    private companion object {
        const val PLACE = "screens.language"
    }
}
