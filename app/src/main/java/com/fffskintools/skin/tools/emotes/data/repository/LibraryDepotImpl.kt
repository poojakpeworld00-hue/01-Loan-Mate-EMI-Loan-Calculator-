package com.fffskintools.skin.tools.emotes.data.repository

import com.fffskintools.skin.tools.emotes.data.local.Material
import com.fffskintools.skin.tools.emotes.domain.model.Guide2
import com.fffskintools.skin.tools.emotes.domain.model.LibrarySort
import com.fffskintools.skin.tools.emotes.domain.model.KitSection
import com.fffskintools.skin.tools.emotes.domain.repository.LibraryDepot

/** Serves the bundled content generated from the reference app (data/local/Material.kt). */
class LibraryDepotImpl : LibraryDepot {
    override fun section(type: LibrarySort): KitSection = when (type) {
        LibrarySort.CHARACTERS -> Material.CHARACTERS
        LibrarySort.BUNDLES -> Material.BUNDLES
        LibrarySort.EMOTES -> Material.EMOTES
        LibrarySort.WEAPONS -> Material.WEAPONS
        LibrarySort.PETS -> Material.PETS
        LibrarySort.VEHICLES -> Material.VEHICLES
        LibrarySort.MAPS -> Material.MAPS
    }

    override fun tips(): List<Guide2> = Material.TIPS
    override fun diamondGuides(): List<Guide2> = Material.DIAMOND_TIPS
}
