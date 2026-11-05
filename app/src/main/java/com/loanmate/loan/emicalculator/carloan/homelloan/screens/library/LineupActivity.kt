package com.loanmate.loan.emicalculator.carloan.homelloan.screens.library

import com.loanmate.loan.emicalculator.carloan.adkit.Spots
import com.loanmate.loan.emicalculator.carloan.adkit.metrics.Views
import com.loanmate.loan.emicalculator.carloan.adkit.metrics.MetricsTwo
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.loanmate.loan.emicalculator.carloan.homelloan.screens.shared.kitEnableDarkEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.loanmate.loan.emicalculator.carloan.homelloan.databinding.PageRosterBinding
import com.loanmate.loan.emicalculator.carloan.homelloan.logic.entity.ShelfOrder
import com.loanmate.loan.emicalculator.carloan.homelloan.screens.shared.toolViewForm
import com.loanmate.loan.emicalculator.carloan.homelloan.screens.shared.padForOsBars
import com.loanmate.loan.emicalculator.carloan.homelloan.screens.shared.listAd

/** Category grid (weapon types, maps, emote rarities) or item grid for one catalog section. */
class LineupActivity : AppCompatActivity() {
    private val viewModel by toolViewForm { handle -> LineupScreenModel(catalogRepository, handle) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        kitEnableDarkEdgeToEdge()
        val b = PageRosterBinding.inflate(layoutInflater)
        setContentView(b.root)
        b.root.padForOsBars()
        b.header.back.setOnClickListener { finish() }

        val vm = viewModel
        b.header.title.text = vm.title.resolve(this)
        b.list.layoutManager = GridLayoutManager(this, vm.columns)
        // Per-list RC place for the in-list ad: screens.list_<type>, and screens.list_<type>_items inside a category.
        val listPlace = "screens.list_${vm.type.name.lowercase()}" + if (!vm.showsGroups && vm.hasGroups) "_items" else ""
        b.list.adapter = BrickAdapter(vm.tiles, vm.shape, listAd(listPlace)) { i ->
            MetricsTwo.click("item", i)
            Spots.exit(this, PLACE) {
                startActivity(
                    if (vm.showsGroups) intent(this, vm.type, i)
                    else SpotlightActivity.intent(this, vm.type, vm.groupIndex, i)
                )
            }
        }
        Spots.bottom(this, b.adSlot, PLACE)
    }

    override fun onResume() {
        super.onResume()
        Views.show("list_${viewModel.type.name.lowercase()}")
    }

    companion object {
        private const val PLACE = "screens.list"
        const val EXTRA_CATALOG = "catalog"
        const val EXTRA_GROUP = "group"

        fun intent(context: Context, catalog: ShelfOrder, group: Int = -1): Intent =
            Intent(context, LineupActivity::class.java)
                .putExtra(EXTRA_CATALOG, catalog.ordinal)
                .putExtra(EXTRA_GROUP, group)
    }
}
