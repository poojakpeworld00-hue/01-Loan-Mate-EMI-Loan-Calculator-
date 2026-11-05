package com.loanmate.loan.emicalculator.carloan.homelloan.screens.guides

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.loanmate.loan.emicalculator.carloan.homelloan.R
import com.loanmate.loan.emicalculator.carloan.homelloan.logic.entity.TourTwo
import com.loanmate.loan.emicalculator.carloan.homelloan.logic.repos.ShelfStore

/** Tips & Tricks or Diamond Tips (by the "guides" argument), for the list and the article screens. */
class ToursScreenModel(repository: ShelfStore, args: SavedStateHandle) : ViewModel() {
    val guides: Boolean = args[ToursActivity.EXTRA_GUIDES] ?: false
    val articles: List<TourTwo> = if (guides) repository.diamondGuides() else repository.tips()
    val title = if (guides) R.string.tool_diamond else R.string.tool_tips
    val icon = if (guides) R.drawable.ic_diamond else R.drawable.ic_bulb

    /** The article opened on the article screen. */
    val index: Int = args[TourActivity.EXTRA_INDEX] ?: 0
    val article: TourTwo get() = articles[index]

    /** Numbered tips show "Tip n"; diamond-tip guides are unnumbered, as in the reference app. */
    val showsNumber get() = !guides
}
