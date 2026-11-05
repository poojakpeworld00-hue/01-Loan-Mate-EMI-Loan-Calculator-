package com.loanmate.loan.emicalculator.carloan.homelloan.presentation.catalog

import com.loanmate.loan.emicalculator.carloan.homelloan.domain.model.LibrarySort

/** How a tile frames its image: [ratio] is width:height; [fullBleed] crops edge to edge instead of fitting with padding. */
enum class BlockOutline(val ratio: String, val fullBleed: Boolean) {
    SQUARE("1:1", false),
    MAP_COVER("309:592", true), // the six map cover cards
    BANNER("1460:500", true), // map location artwork, which has its name printed on it
}

val LibrarySort.groupShape: BlockOutline get() = if (this == LibrarySort.MAPS) BlockOutline.MAP_COVER else BlockOutline.SQUARE
val LibrarySort.itemShape: BlockOutline get() = if (this == LibrarySort.MAPS) BlockOutline.BANNER else BlockOutline.SQUARE
