package com.loanmate.loan.emicalculator.carloan.homelloan.screens.guides

import com.loanmate.loan.emicalculator.carloan.homelloan.screens.shared.wireScreenPromo
import com.loanmate.loan.emicalculator.carloan.adkit.Spots
import com.loanmate.loan.emicalculator.carloan.adkit.metrics.Views
import com.loanmate.loan.emicalculator.carloan.adkit.metrics.MetricsTwo
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import com.loanmate.loan.emicalculator.carloan.homelloan.screens.shared.kitEnableLightEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.loanmate.loan.emicalculator.carloan.homelloan.R
import com.loanmate.loan.emicalculator.carloan.homelloan.databinding.ScreenGuideBinding
import com.loanmate.loan.emicalculator.carloan.homelloan.databinding.ScreenGuidesBinding
import com.loanmate.loan.emicalculator.carloan.homelloan.databinding.RowToolBinding
import com.loanmate.loan.emicalculator.carloan.homelloan.screens.shared.toolScreenModel
import com.loanmate.loan.emicalculator.carloan.homelloan.screens.shared.padForSysBars
import com.loanmate.loan.emicalculator.carloan.homelloan.screens.shared.listAd
import com.loanmate.loan.emicalculator.carloan.homelloan.screens.shared.insertInto

/** Tips & Tricks and Diamond Tips: a list of articles. */
class ToursActivity : AppCompatActivity() {
    private val viewModel by toolScreenModel { handle -> ToursScreenModel(catalogRepository, handle) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        kitEnableLightEdgeToEdge()
        val b = ScreenGuidesBinding.inflate(layoutInflater)
        setContentView(b.root)
        b.root.padForSysBars()
        b.crest.backVw.setOnClickListener { finish() }

        b.crest.titleVw.setText(viewModel.title)
        viewModel.articles.forEachIndexed { i, a ->
            val row = RowToolBinding.inflate(layoutInflater, b.rowsVw, true)
            row.iconVw.setImageResource(viewModel.icon)
            row.nameVw.text = a.title
            row.subVw.text = a.summary
            row.subVw.visibility = if (a.summary.isEmpty()) View.GONE else View.VISIBLE
            row.root.setOnClickListener {
                MetricsTwo.click("article", i)
                startActivity(TourActivity.intent(this, viewModel.guides, i))
            }
        }
        listAd(if (viewModel.guides) "screens.diamond_tips" else "screens.tips")
            ?.insertInto(b.rowsVw, resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._7sdp))
        wireScreenPromo("screens.articles", b.adInlineVw, b.adSlotVw)
    }

    override fun onResume() {
        super.onResume()
        Views.show("articles")
    }

    companion object {
        const val EXTRA_GUIDES = "guides"

        fun intent(context: Context, guides: Boolean): Intent =
            Intent(context, ToursActivity::class.java).putExtra(EXTRA_GUIDES, guides)
    }
}

class TourActivity : AppCompatActivity() {
    private val viewModel by toolScreenModel { handle -> ToursScreenModel(catalogRepository, handle) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        kitEnableLightEdgeToEdge()
        val b = ScreenGuideBinding.inflate(layoutInflater)
        setContentView(b.root)
        b.root.padForSysBars()
        b.crest.backVw.setOnClickListener { finish() }

        val a = viewModel.article
        b.crest.titleVw.setText(viewModel.title)
        b.numberVw.visibility = if (viewModel.showsNumber) View.VISIBLE else View.GONE
        b.numberVw.text = getString(R.string.tip_number, viewModel.index + 1)
        b.titleVw.text = a.title
        b.bodyVw.text = a.body
        // Diamond tips: its own place, the bottom ad pinned under the text; tips keep screens.article.
        if (viewModel.guides) Spots.bottom(this, b.adSlotVw, "screens.diamond_article")
        else wireScreenPromo("screens.article", b.adInlineVw, b.adSlotVw)
    }

    override fun onResume() {
        super.onResume()
        Views.show("article")
    }

    companion object {
        const val EXTRA_INDEX = "index"

        fun intent(context: Context, guides: Boolean, index: Int): Intent =
            Intent(context, TourActivity::class.java)
                .putExtra(ToursActivity.EXTRA_GUIDES, guides)
                .putExtra(EXTRA_INDEX, index)
    }
}
