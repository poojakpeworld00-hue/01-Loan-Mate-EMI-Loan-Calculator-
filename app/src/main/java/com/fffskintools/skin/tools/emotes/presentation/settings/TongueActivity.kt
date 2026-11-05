package com.fffskintools.skin.tools.emotes.presentation.settings

import com.fffskintools.skin.tools.ads.Spots
import com.fffskintools.skin.tools.ads.analytics.Stats2
import com.fffskintools.skin.tools.ads.analytics.Views
import android.os.Bundle
import com.fffskintools.skin.tools.emotes.presentation.common.kitEnableDarkEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.fffskintools.skin.tools.emotes.R
import com.fffskintools.skin.tools.emotes.databinding.PageTongueBinding
import com.fffskintools.skin.tools.emotes.presentation.common.toolViewForm
import com.fffskintools.skin.tools.emotes.presentation.common.kitCollectWhenStarted
import com.fffskintools.skin.tools.emotes.presentation.common.padForOsBars
import com.fffskintools.skin.tools.emotes.presentation.onboarding.PickAdapter
import com.fffskintools.skin.tools.emotes.presentation.onboarding.tonguePhase

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
