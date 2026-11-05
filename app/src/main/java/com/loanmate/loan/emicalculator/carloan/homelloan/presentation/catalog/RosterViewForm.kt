package com.loanmate.loan.emicalculator.carloan.homelloan.presentation.catalog

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.loanmate.loan.emicalculator.carloan.homelloan.domain.model.LibrarySort
import com.loanmate.loan.emicalculator.carloan.homelloan.domain.repository.LibraryDepot
import com.loanmate.loan.emicalculator.carloan.homelloan.presentation.common.UiCaption

/** A section's categories (weapon types, maps, emote rarities) or one category's items, from the screen's arguments. */
class RosterViewForm(repository: LibraryDepot, args: SavedStateHandle) : ViewModel() {
    val type: LibrarySort = LibrarySort.entries[args[RosterActivity.EXTRA_CATALOG] ?: 0]
    private val section = repository.section(type)
    private val groupArg: Int = args[RosterActivity.EXTRA_GROUP] ?: -1

    /** True when this screen lists categories rather than items. */
    val showsGroups = section.groups.size > 1 && groupArg < 0

    /** The section has categories (emotes, weapons, maps), whichever level this screen shows. */
    val hasGroups = section.groups.size > 1
    val groupIndex = groupArg.coerceAtLeast(0)

    val title: UiCaption = if (showsGroups) UiCaption.KitRes(section.title)
    else section.groups[groupIndex].name?.let { UiCaption.KitPlain(it) } ?: UiCaption.KitRes(section.title)

    val tiles: List<Block2> = if (showsGroups) section.groups.map { Block2(it.name, it.image) }
    else section.groups[groupIndex].items.map { Block2(it.name, it.thumb) }

    val columns = if (showsGroups) 2 else type.columns
    val shape = if (showsGroups) type.groupShape else type.itemShape
}
