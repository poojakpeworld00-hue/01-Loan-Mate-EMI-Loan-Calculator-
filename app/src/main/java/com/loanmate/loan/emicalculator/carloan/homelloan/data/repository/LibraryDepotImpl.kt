package com.loanmate.loan.emicalculator.carloan.homelloan.data.repository

import com.loanmate.loan.emicalculator.carloan.homelloan.data.local.Material
import com.loanmate.loan.emicalculator.carloan.homelloan.domain.model.Guide2
import com.loanmate.loan.emicalculator.carloan.homelloan.domain.model.LibrarySort
import com.loanmate.loan.emicalculator.carloan.homelloan.domain.model.KitSection
import com.loanmate.loan.emicalculator.carloan.homelloan.domain.repository.LibraryDepot

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
