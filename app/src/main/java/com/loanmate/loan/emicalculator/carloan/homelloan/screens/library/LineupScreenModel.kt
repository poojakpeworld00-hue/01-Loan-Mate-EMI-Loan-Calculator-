package com.loanmate.loan.emicalculator.carloan.homelloan.screens.library

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.loanmate.loan.emicalculator.carloan.homelloan.logic.entity.ShelfOrder
import com.loanmate.loan.emicalculator.carloan.homelloan.logic.repos.ShelfStore
import com.loanmate.loan.emicalculator.carloan.homelloan.screens.shared.LookLabel

/** A section's categories (weapon types, maps, emote rarities) or one category's items, from the screen's arguments. */
class LineupScreenModel(repository: ShelfStore, args: SavedStateHandle) : ViewModel() {
    val type: ShelfOrder = ShelfOrder.entries[args[LineupActivity.EXTRA_CATALOG] ?: 0]
    private val section = repository.section(type)
    private val groupArg: Int = args[LineupActivity.EXTRA_GROUP] ?: -1

    /** True when this screen lists categories rather than items. */
    val showsGroups = section.groups.size > 1 && groupArg < 0

    /** The section has categories (emotes, weapons, maps), whichever level this screen shows. */
    val hasGroups = section.groups.size > 1
    val groupIndex = groupArg.coerceAtLeast(0)

    val title: LookLabel = if (showsGroups) LookLabel.PackOut(section.title)
    else section.groups[groupIndex].name?.let { LookLabel.PackSimple(it) } ?: LookLabel.PackOut(section.title)

    val tiles: List<BrickTwo> = if (showsGroups) section.groups.map { BrickTwo(it.name, it.image) }
    else section.groups[groupIndex].items.map { BrickTwo(it.name, it.thumb) }

    val columns = if (showsGroups) 2 else type.columns
    val shape = if (showsGroups) type.groupShape else type.itemShape
}
