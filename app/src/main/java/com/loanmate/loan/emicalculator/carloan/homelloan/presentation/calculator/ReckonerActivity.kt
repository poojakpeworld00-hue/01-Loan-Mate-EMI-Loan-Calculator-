package com.loanmate.loan.emicalculator.carloan.homelloan.presentation.calculator

import com.loanmate.loan.emicalculator.carloan.homelloan.ads.Spots
import com.loanmate.loan.emicalculator.carloan.homelloan.ads.analytics.Views
import com.loanmate.loan.emicalculator.carloan.homelloan.ads.analytics.Stats2
import android.content.Intent
import android.os.Bundle
import com.loanmate.loan.emicalculator.carloan.homelloan.presentation.common.kitEnableDarkEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.loanmate.loan.emicalculator.carloan.homelloan.R
import com.loanmate.loan.emicalculator.carloan.homelloan.databinding.PageReckonBinding
import com.loanmate.loan.emicalculator.carloan.homelloan.databinding.PageRosterBinding
import com.loanmate.loan.emicalculator.carloan.homelloan.domain.model.ReckonerSort
import com.loanmate.loan.emicalculator.carloan.homelloan.presentation.catalog.Block2
import com.loanmate.loan.emicalculator.carloan.homelloan.presentation.catalog.BlockAdapter
import com.loanmate.loan.emicalculator.carloan.homelloan.presentation.common.toolViewForm
import com.loanmate.loan.emicalculator.carloan.homelloan.presentation.common.kitCollectWhenStarted
import com.loanmate.loan.emicalculator.carloan.homelloan.presentation.common.padForOsBars
import com.loanmate.loan.emicalculator.carloan.homelloan.presentation.common.listAd

/** The four calculators as tiles. Static, so it has no ViewModel. */
class ReckonerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        kitEnableDarkEdgeToEdge()
        val b = PageRosterBinding.inflate(layoutInflater)
        setContentView(b.root)
        b.root.padForOsBars()
        b.header.back.setOnClickListener { finish() }
        b.header.title.setText(R.string.tool_calculator)
        b.list.layoutManager = GridLayoutManager(this, 2)
        b.list.adapter = BlockAdapter(ReckonerSort.entries.map { Block2(getString(it.title), it.image) }, ad = listAd(PLACE)) {
            Stats2.click("calculator", ReckonerSort.entries[it].name.lowercase())
            Spots.exit(this, PLACE) { startActivity(Intent(this, ReckonActivity::class.java).putExtra(ReckonActivity.EXTRA_CALC, it)) }
        }
        Spots.bottom(this, b.adSlot, PLACE)
    }

    override fun onResume() {
        super.onResume()
        Views.show("calculator")
    }

    private companion object {
        const val PLACE = "screens.calculator"
    }
}

class ReckonActivity : AppCompatActivity() {
    private val viewModel by toolViewForm { handle -> ReckonViewForm(calculate, handle) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        kitEnableDarkEdgeToEdge()
        val b = PageReckonBinding.inflate(layoutInflater)
        setContentView(b.root)
        b.root.padForOsBars()
        b.header.back.setOnClickListener { finish() }

        b.header.title.setText(viewModel.type.title)
        b.inputLayout.setHint(viewModel.type.inputHint)
        listOf(b.chip50 to 50, b.chip100 to 100, b.chip500 to 500, b.chip1000 to 1000).forEach { (chip, v) ->
            chip.setOnClickListener { b.amount.setText(v.toString()); b.amount.setSelection(b.amount.length()) }
        }
        b.calculate.setOnClickListener { viewModel.onWork(b.amount.text?.toString().orEmpty()) }
        kitCollectWhenStarted(viewModel.state) { state ->
            b.result.text = state.result.format(this)
            b.inputLayout.error = state.error?.let(::getString)
        }
        Spots.list(this, b.adList, "screens.calc") // above the Calculate button
        Spots.bottom(this, b.adSlot, "screens.calc")
    }

    override fun onResume() {
        super.onResume()
        Views.show("calc")
    }

    companion object {
        const val EXTRA_CALC = "calc"
    }
}
