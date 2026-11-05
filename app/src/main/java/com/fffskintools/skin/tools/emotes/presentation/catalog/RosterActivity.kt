package com.fffskintools.skin.tools.emotes.presentation.catalog

import com.fffskintools.skin.tools.ads.Spots
import com.fffskintools.skin.tools.ads.analytics.Views
import com.fffskintools.skin.tools.ads.analytics.Stats2
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.fffskintools.skin.tools.emotes.presentation.common.kitEnableDarkEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.fffskintools.skin.tools.emotes.databinding.PageRosterBinding
import com.fffskintools.skin.tools.emotes.domain.model.LibrarySort
import com.fffskintools.skin.tools.emotes.presentation.common.toolViewForm
import com.fffskintools.skin.tools.emotes.presentation.common.padForOsBars
import com.fffskintools.skin.tools.emotes.presentation.common.listAd

/** Category grid (weapon types, maps, emote rarities) or item grid for one catalog section. */
class RosterActivity : AppCompatActivity() {
    private val viewModel by toolViewForm { handle -> RosterViewForm(catalogRepository, handle) }

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
        b.list.adapter = BlockAdapter(vm.tiles, vm.shape, listAd(listPlace)) { i ->
            Stats2.click("item", i)
            Spots.exit(this, PLACE) {
                startActivity(
                    if (vm.showsGroups) intent(this, vm.type, i)
                    else FocusActivity.intent(this, vm.type, vm.groupIndex, i)
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

        fun intent(context: Context, catalog: LibrarySort, group: Int = -1): Intent =
            Intent(context, RosterActivity::class.java)
                .putExtra(EXTRA_CATALOG, catalog.ordinal)
                .putExtra(EXTRA_GROUP, group)
    }
}
