package com.loanmate.loan.emicalculator.carloan.homelloan.presentation.articles

import com.loanmate.loan.emicalculator.carloan.homelloan.presentation.common.wireViewSpot
import com.loanmate.loan.emicalculator.carloan.homelloan.ads.Spots
import com.loanmate.loan.emicalculator.carloan.homelloan.ads.analytics.Views
import com.loanmate.loan.emicalculator.carloan.homelloan.ads.analytics.Stats2
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import com.loanmate.loan.emicalculator.carloan.homelloan.presentation.common.kitEnableDarkEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.loanmate.loan.emicalculator.carloan.homelloan.R
import com.loanmate.loan.emicalculator.carloan.homelloan.databinding.PageGuideBinding
import com.loanmate.loan.emicalculator.carloan.homelloan.databinding.PageGuidesBinding
import com.loanmate.loan.emicalculator.carloan.homelloan.databinding.CellToolBinding
import com.loanmate.loan.emicalculator.carloan.homelloan.presentation.common.toolViewForm
import com.loanmate.loan.emicalculator.carloan.homelloan.presentation.common.padForOsBars
import com.loanmate.loan.emicalculator.carloan.homelloan.presentation.common.listAd
import com.loanmate.loan.emicalculator.carloan.homelloan.presentation.common.insertInto

/** Tips & Tricks and Diamond Tips: a list of articles. */
class GuidesActivity : AppCompatActivity() {
    private val viewModel by toolViewForm { handle -> GuidesViewForm(catalogRepository, handle) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        kitEnableDarkEdgeToEdge()
        val b = PageGuidesBinding.inflate(layoutInflater)
        setContentView(b.root)
        b.root.padForOsBars()
        b.header.back.setOnClickListener { finish() }

        b.header.title.setText(viewModel.title)
        viewModel.articles.forEachIndexed { i, a ->
            val row = CellToolBinding.inflate(layoutInflater, b.rows, true)
            row.icon.setImageResource(viewModel.icon)
            row.name.text = a.title
            row.sub.text = a.summary
            row.sub.visibility = if (a.summary.isEmpty()) View.GONE else View.VISIBLE
            row.root.setOnClickListener {
                Stats2.click("article", i)
                startActivity(GuideActivity.intent(this, viewModel.guides, i))
            }
        }
        listAd(if (viewModel.guides) "screens.diamond_tips" else "screens.tips")
            ?.insertInto(b.rows, resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._7sdp))
        wireViewSpot("screens.articles", b.adInline, b.adSlot)
    }

    override fun onResume() {
        super.onResume()
        Views.show("articles")
    }

    companion object {
        const val EXTRA_GUIDES = "guides"

        fun intent(context: Context, guides: Boolean): Intent =
            Intent(context, GuidesActivity::class.java).putExtra(EXTRA_GUIDES, guides)
    }
}

class GuideActivity : AppCompatActivity() {
    private val viewModel by toolViewForm { handle -> GuidesViewForm(catalogRepository, handle) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        kitEnableDarkEdgeToEdge()
        val b = PageGuideBinding.inflate(layoutInflater)
        setContentView(b.root)
        b.root.padForOsBars()
        b.header.back.setOnClickListener { finish() }

        val a = viewModel.article
        b.header.title.setText(viewModel.title)
        b.number.visibility = if (viewModel.showsNumber) View.VISIBLE else View.GONE
        b.number.text = getString(R.string.tip_number, viewModel.index + 1)
        b.title.text = a.title
        b.body.text = a.body
        // Diamond tips: its own place, the bottom ad pinned under the text; tips keep screens.article.
        if (viewModel.guides) Spots.bottom(this, b.adSlot, "screens.diamond_article")
        else wireViewSpot("screens.article", b.adInline, b.adSlot)
    }

    override fun onResume() {
        super.onResume()
        Views.show("article")
    }

    companion object {
        const val EXTRA_INDEX = "index"

        fun intent(context: Context, guides: Boolean, index: Int): Intent =
            Intent(context, GuideActivity::class.java)
                .putExtra(GuidesActivity.EXTRA_GUIDES, guides)
                .putExtra(EXTRA_INDEX, index)
    }
}
