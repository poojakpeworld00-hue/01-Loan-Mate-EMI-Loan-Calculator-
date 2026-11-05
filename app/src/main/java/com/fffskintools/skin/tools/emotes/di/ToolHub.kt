package com.fffskintools.skin.tools.emotes.di

import android.content.Context
import com.fffskintools.skin.tools.emotes.data.local.ChoicesFactsFeed
import com.fffskintools.skin.tools.emotes.data.repository.LibraryDepotImpl
import com.fffskintools.skin.tools.emotes.data.repository.FilesDepotImpl
import com.fffskintools.skin.tools.emotes.data.repository.OptionsDepotImpl
import com.fffskintools.skin.tools.emotes.domain.repository.LibraryDepot
import com.fffskintools.skin.tools.emotes.domain.repository.FilesDepot
import com.fffskintools.skin.tools.emotes.domain.repository.OptionsDepot
import com.fffskintools.skin.tools.emotes.domain.usecase.WorkApplyTask
import com.fffskintools.skin.tools.emotes.data.remote.config.ToolProfile
import com.fffskintools.skin.tools.emotes.data.remote.config.ToolProfileKeeper
import kotlinx.coroutines.flow.StateFlow

/** Manual dependency wiring: one instance per app, created in [com.fffskintools.skin.tools.emotes.Tool2]. */
class ToolHub(context: Context) {
    private val preferences = ChoicesFactsFeed(context)

    val catalogRepository: LibraryDepot = LibraryDepotImpl()
    val settingsRepository: OptionsDepot = OptionsDepotImpl(preferences)
    val mediaRepository: FilesDepot = FilesDepotImpl(context.applicationContext)
    val calculate = WorkApplyTask()

    /** The live Remote Config ([ToolProfileKeeper] is started in Tool2.onCreate). */
    val config: StateFlow<ToolProfile> = ToolProfileKeeper.current
}
