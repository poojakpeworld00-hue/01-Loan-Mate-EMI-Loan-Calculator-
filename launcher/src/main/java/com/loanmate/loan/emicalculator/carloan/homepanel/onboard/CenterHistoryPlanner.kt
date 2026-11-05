package com.loanmate.loan.emicalculator.carloan.homepanel.onboard

import android.app.Activity
import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.os.Bundle

/** Our tasks are never listed in Recents (neither the app nor any of its screens), unless the host says [listed]. */
object CenterHistoryPlanner : Application.ActivityLifecycleCallbacks {
    /** Host policy: true keeps the app listed in Recents (the host app sets it). */
    @Volatile var listed: (Context) -> Boolean = { false }

    fun install(app: Application) = app.registerActivityLifecycleCallbacks(this)

    private fun apply(activity: Activity) {
        val hidden = !listed(activity)
        val tasks = runCatching { activity.getSystemService(ActivityManager::class.java)?.appTasks.orEmpty() }.getOrDefault(emptyList())
        tasks.forEach { task ->
            runCatching { task.setExcludeFromRecents(hidden) }
                .onFailure { android.util.Log.w("RecentsCharter", "setExcludeFromRecents failed", it) }
        }
    }

    override fun onActivityResumed(activity: Activity) = apply(activity)

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityStarted(activity: Activity) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}
}
