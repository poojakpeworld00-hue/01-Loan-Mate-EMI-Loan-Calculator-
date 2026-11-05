package com.loanmate.loan.emicalculator.carloan.homelloan.screens.emi

import com.loanmate.loan.emicalculator.carloan.adkit.Spots
import com.loanmate.loan.emicalculator.carloan.adkit.metrics.Views
import com.loanmate.loan.emicalculator.carloan.adkit.metrics.MetricsTwo
import android.content.Intent
import android.os.Bundle
import com.loanmate.loan.emicalculator.carloan.homelloan.screens.shared.kitEnableDarkEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.loanmate.loan.emicalculator.carloan.homelloan.R
import com.loanmate.loan.emicalculator.carloan.homelloan.databinding.ScreenReckonBinding
import com.loanmate.loan.emicalculator.carloan.homelloan.databinding.ScreenRosterBinding
import com.loanmate.loan.emicalculator.carloan.homelloan.logic.entity.CalculatorOrder
import com.loanmate.loan.emicalculator.carloan.homelloan.screens.library.BrickTwo
import com.loanmate.loan.emicalculator.carloan.homelloan.screens.library.BrickAdapter
import com.loanmate.loan.emicalculator.carloan.homelloan.screens.shared.toolViewForm
import com.loanmate.loan.emicalculator.carloan.homelloan.screens.shared.kitCollectWhenStarted
import com.loanmate.loan.emicalculator.carloan.homelloan.screens.shared.padForOsBars
import com.loanmate.loan.emicalculator.carloan.homelloan.screens.shared.listAd

/** The four calculators as tiles. Static, so it has no ViewModel. */
class CalculatorActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        kitEnableDarkEdgeToEdge()
        val b = ScreenRosterBinding.inflate(layoutInflater)
        setContentView(b.root)
        b.root.padForOsBars()
        b.crest.backVw.setOnClickListener { finish() }
        b.crest.titleVw.setText(R.string.tool_calculator)
        b.listVw.layoutManager = GridLayoutManager(this, 2)
        b.listVw.adapter = BrickAdapter(CalculatorOrder.entries.map { BrickTwo(getString(it.title), it.image) }, ad = listAd(PLACE)) {
            MetricsTwo.click("calculator", CalculatorOrder.entries[it].name.lowercase())
            Spots.exit(this, PLACE) { startActivity(Intent(this, ComputeActivity::class.java).putExtra(ComputeActivity.EXTRA_CALC, it)) }
        }
        Spots.bottom(this, b.adSlotVw, PLACE)
    }

    override fun onResume() {
        super.onResume()
        Views.show("calculator")
    }

    private companion object {
        const val PLACE = "screens.calculator"
    }
}

class ComputeActivity : AppCompatActivity() {
    private val viewModel by toolViewForm { handle -> ComputeScreenModel(calculate, handle) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        kitEnableDarkEdgeToEdge()
        val b = ScreenReckonBinding.inflate(layoutInflater)
        setContentView(b.root)
        b.root.padForOsBars()
        b.crest.backVw.setOnClickListener { finish() }

        b.crest.titleVw.setText(viewModel.type.title)
        b.inputLayoutVw.setHint(viewModel.type.inputHint)
        listOf(b.flag50 to 50, b.flag100 to 100, b.flag500 to 500, b.flag1000 to 1000).forEach { (chip, v) ->
            chip.setOnClickListener { b.amountVw.setText(v.toString()); b.amountVw.setSelection(b.amountVw.length()) }
        }
        b.calculateVw.setOnClickListener { viewModel.onWork(b.amountVw.text?.toString().orEmpty()) }
        kitCollectWhenStarted(viewModel.state) { state ->
            b.resultVw.text = state.result.format(this)
            b.inputLayoutVw.error = state.error?.let(::getString)
        }
        Spots.list(this, b.adListVw, "screens.calc") // above the Calculate button
        Spots.bottom(this, b.adSlotVw, "screens.calc")
    }

    override fun onResume() {
        super.onResume()
        Views.show("calc")
    }

    companion object {
        const val EXTRA_CALC = "calc"
    }
}
