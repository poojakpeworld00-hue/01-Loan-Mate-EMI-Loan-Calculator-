package com.loanmate.loan.emicalculator.carloan.homelloan.screens.prefs

import com.loanmate.loan.emicalculator.carloan.adkit.Spots
import com.loanmate.loan.emicalculator.carloan.adkit.metrics.MetricsTwo
import com.loanmate.loan.emicalculator.carloan.adkit.metrics.Views
import android.os.Bundle
import com.loanmate.loan.emicalculator.carloan.homelloan.screens.shared.kitEnableDarkEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.loanmate.loan.emicalculator.carloan.homelloan.R
import com.loanmate.loan.emicalculator.carloan.homelloan.databinding.PageTongueBinding
import com.loanmate.loan.emicalculator.carloan.homelloan.screens.shared.toolViewForm
import com.loanmate.loan.emicalculator.carloan.homelloan.screens.shared.kitCollectWhenStarted
import com.loanmate.loan.emicalculator.carloan.homelloan.screens.shared.padForOsBars
import com.loanmate.loan.emicalculator.carloan.homelloan.screens.welcome.ChooseAdapter
import com.loanmate.loan.emicalculator.carloan.homelloan.screens.welcome.tonguePhase

/** Tongue picker opened from Settings; Apply switches the app language and returns to Settings. */
class LanguageActivity : AppCompatActivity() {
    private val viewModel by toolViewForm { handle -> LanguageScreenModel(settingsRepository, handle) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        kitEnableDarkEdgeToEdge()
        val b = PageTongueBinding.inflate(layoutInflater)
        setContentView(b.root)
        b.root.padForOsBars()
        b.header.back.setOnClickListener { finish() }
        b.header.title.setText(R.string.language)

        val adapter = ChooseAdapter(tonguePhase(viewModel.languages), setOf(viewModel.selected.value)) { viewModel.select(it) }
        b.list.layoutManager = LinearLayoutManager(this)
        b.list.adapter = adapter
        b.list.scrollToPosition(viewModel.selected.value)
        kitCollectWhenStarted(viewModel.selected) { adapter.kitSubmitSelection(setOf(it)) }

        b.apply.setOnClickListener {
            MetricsTwo.click("apply", viewModel.languages[viewModel.selected.value].tag)
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
