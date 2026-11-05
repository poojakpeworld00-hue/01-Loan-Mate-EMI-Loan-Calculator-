package com.fffskintools.skin.tools.emotes.domain.repository

import com.fffskintools.skin.tools.emotes.domain.model.Guide2
import com.fffskintools.skin.tools.emotes.domain.model.LibrarySort
import com.fffskintools.skin.tools.emotes.domain.model.KitSection

interface LibraryDepot {
    fun section(type: LibrarySort): KitSection
    fun tips(): List<Guide2>
    fun diamondGuides(): List<Guide2>
}
