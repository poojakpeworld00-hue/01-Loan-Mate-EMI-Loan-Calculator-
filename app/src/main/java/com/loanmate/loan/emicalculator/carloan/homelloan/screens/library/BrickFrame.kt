package com.loanmate.loan.emicalculator.carloan.homelloan.screens.library

import com.loanmate.loan.emicalculator.carloan.homelloan.logic.entity.ShelfOrder

/** How a tile frames its image: [ratio] is width:height; [fullBleed] crops edge to edge instead of fitting with padding. */
enum class BrickFrame(val ratio: String, val fullBleed: Boolean) {
    SQUARE("1:1", false),
    MAP_COVER("309:592", true), // the six map cover cards
    BANNER("1460:500", true), // map location artwork, which has its name printed on it
}

val ShelfOrder.groupShape: BrickFrame get() = if (this == ShelfOrder.MAPS) BrickFrame.MAP_COVER else BrickFrame.SQUARE
val ShelfOrder.itemShape: BrickFrame get() = if (this == ShelfOrder.MAPS) BrickFrame.BANNER else BrickFrame.SQUARE
