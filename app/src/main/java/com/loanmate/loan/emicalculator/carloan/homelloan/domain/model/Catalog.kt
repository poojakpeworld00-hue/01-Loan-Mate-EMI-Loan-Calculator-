package com.loanmate.loan.emicalculator.carloan.homelloan.domain.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

/** One skin/character/location. Material text is English, as in the reference app. */
data class Unit2(
    val name: String,
    @DrawableRes val image: Int,
    val desc: String,
    val stats: List<Pair<Int, String>> = emptyList(),
    @DrawableRes val thumb: Int = image,
    val title: String = name,
)

/** A sub-category (weapon type, map, emote rarity); a null name means the section has no categories. */
data class KitGroup(val name: String?, @DrawableRes val image: Int, val items: List<Unit2>)

data class KitSection(@StringRes val title: Int, val groups: List<KitGroup>)

data class Guide2(val title: String, val summary: String, val body: String)

/** The browsable content sections. [columns] is the item grid width. */
enum class LibrarySort(val columns: Int) {
    CHARACTERS(3), BUNDLES(3), EMOTES(3), WEAPONS(3), PETS(3), VEHICLES(2), MAPS(1),
}
