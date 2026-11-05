package com.loanmate.loan.emicalculator.carloan.homelloan.store.repos

import com.loanmate.loan.emicalculator.carloan.homelloan.store.disk.Material
import com.loanmate.loan.emicalculator.carloan.homelloan.logic.entity.TourTwo
import com.loanmate.loan.emicalculator.carloan.homelloan.logic.entity.ShelfOrder
import com.loanmate.loan.emicalculator.carloan.homelloan.logic.entity.PackPart
import com.loanmate.loan.emicalculator.carloan.homelloan.logic.repos.ShelfStore

/** Serves the bundled content generated from the reference app (data/local/Material.kt). */
class ShelfStoreImpl : ShelfStore {
    override fun section(type: ShelfOrder): PackPart = when (type) {
        ShelfOrder.CHARACTERS -> Material.CHARACTERS
        ShelfOrder.BUNDLES -> Material.BUNDLES
        ShelfOrder.EMOTES -> Material.EMOTES
        ShelfOrder.WEAPONS -> Material.WEAPONS
        ShelfOrder.PETS -> Material.PETS
        ShelfOrder.VEHICLES -> Material.VEHICLES
        ShelfOrder.MAPS -> Material.MAPS
    }

    override fun tips(): List<TourTwo> = Material.TIPS
    override fun diamondGuides(): List<TourTwo> = Material.DIAMOND_TIPS
}
