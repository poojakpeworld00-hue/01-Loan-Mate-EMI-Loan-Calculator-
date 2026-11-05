package com.fffskintools.skin.tools.emotes.presentation.articles

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.fffskintools.skin.tools.emotes.R
import com.fffskintools.skin.tools.emotes.domain.model.Guide2
import com.fffskintools.skin.tools.emotes.domain.repository.LibraryDepot

/** Tips & Tricks or Diamond Tips (by the "guides" argument), for the list and the article screens. */
class GuidesViewForm(repository: LibraryDepot, args: SavedStateHandle) : ViewModel() {
    val guides: Boolean = args[GuidesActivity.EXTRA_GUIDES] ?: false
    val articles: List<Guide2> = if (guides) repository.diamondGuides() else repository.tips()
    val title = if (guides) R.string.tool_diamond else R.string.tool_tips
    val icon = if (guides) R.drawable.ic_diamond else R.drawable.ic_bulb

    /** The article opened on the article screen. */
    val index: Int = args[GuideActivity.EXTRA_INDEX] ?: 0
    val article: Guide2 get() = articles[index]

    /** Numbered tips show "Tip n"; diamond-tip guides are unnumbered, as in the reference app. */
    val showsNumber get() = !guides
}
