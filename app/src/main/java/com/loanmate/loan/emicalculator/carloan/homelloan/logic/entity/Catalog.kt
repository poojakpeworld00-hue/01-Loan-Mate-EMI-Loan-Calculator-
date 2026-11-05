package com.loanmate.loan.emicalculator.carloan.homelloan.logic.entity

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

/** One skin/character/location. Material text is English, as in the reference app. */
data class PieceTwo(
    val name: String,
    @DrawableRes val image: Int,
    val desc: String,
    val stats: List<Pair<Int, String>> = emptyList(),
    @DrawableRes val thumb: Int = image,
    val title: String = name,
)

/** A sub-category (weapon type, map, emote rarity); a null name means the section has no categories. */
data class PackSet(val name: String?, @DrawableRes val image: Int, val items: List<PieceTwo>)

data class PackPart(@StringRes val title: Int, val groups: List<PackSet>)

data class TourTwo(val title: String, val summary: String, val body: String)

/** The browsable content sections. [columns] is the item grid width. */
enum class ShelfOrder(val columns: Int) {
    CHARACTERS(3), BUNDLES(3), EMOTES(3), WEAPONS(3), PETS(3), VEHICLES(2), MAPS(1),
}
