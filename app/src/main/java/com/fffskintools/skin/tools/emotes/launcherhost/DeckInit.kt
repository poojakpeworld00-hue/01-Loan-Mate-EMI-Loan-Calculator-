package com.fffskintools.skin.tools.emotes.launcherhost

import android.app.Application
import com.fffskintools.skin.tools.launcher.Launcher
import com.fffskintools.skin.tools.emotes.data.remote.config.ToolProfileKeeper
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Hands the app's host + events to the launcher module. Call once from Tool2.onCreate. */
object DeckInit {
    private var installed = false

    fun install(app: Application) {
        if (installed) return
        installed = true
        Launcher.install(app, ToolDeckParent(app), ToolDeckCues())
        MainScope().launch {
            // drop(1): install() already read the current values.
            ToolProfileKeeper.current.map { it.launcher }.distinctUntilChanged().drop(1).collect { Launcher.refresh(app) }
        }
    }
}
