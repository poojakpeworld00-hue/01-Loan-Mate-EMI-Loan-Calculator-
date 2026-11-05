package com.loanmate.loan.emicalculator.carloan.homelloan.core.system

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import com.loanmate.loan.emicalculator.carloan.homelloan.ads.OutSpotDriver
import com.loanmate.loan.emicalculator.carloan.homelloan.Tool2
import com.loanmate.loan.emicalculator.carloan.homelloan.presentation.home.BaseActivity
import com.loanmate.loan.emicalculator.carloan.homelloan.presentation.onboarding.SetupActivity
import com.loanmate.loan.emicalculator.carloan.homelloan.presentation.splash.BootActivity

/**
 * The reference app's single MainActivity, mapped onto FFF's screens: before onboarding is done the "main screen" is
 * [SetupActivity], after it [BaseActivity]. A task of ours that started on one of [classes] is an app task (not the
 * launcher's home task).
 */
object MainViews {
    private val classes = setOf(BootActivity::class.java.name, SetupActivity::class.java.name, BaseActivity::class.java.name)

    fun kitIsMain(activity: Activity) = activity is SetupActivity || activity is BaseActivity

    fun isToolTaskBase(className: String?) = className in classes

    fun onboardingDone(context: Context) = (context.applicationContext as Tool2).container.settingsRepository.isOnboarded()

    private fun kitMainClass(context: Context): Class<out Activity> =
        if (onboardingDone(context)) BaseActivity::class.java else SetupActivity::class.java

    /** Our launcher home through Android's own Home intent, so it is the one in the system's home stack. */
    fun deckBaseIntent(context: Context): Intent =
        Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).setPackage(context.packageName).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /** Brings the task of our app screens to the front as it is, whatever is on top of it. */
    fun toolTaskToFront(context: Context): Boolean {
        val task = context.getSystemService(ActivityManager::class.java)?.appTasks?.firstOrNull {
            val info = it.taskInfo
            isToolTaskBase(info?.baseIntent?.component?.className) || isToolTaskBase(info?.baseActivity?.className)
        } ?: return false
        return runCatching { task.moveToFront() }.isSuccess
    }

    fun open(context: Context, systemKey: String? = null) {
        // An exit ad (its ad screen or link tabs) is on top of our screen: bring the task back as it is, so the ad finishes.
        if (OutSpotDriver.busy() && toolTaskToFront(context)) return
        context.startActivity(
            Intent(context, kitMainClass(context))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                .apply { if (systemKey != null) putExtra(OsHotkeyTarget.EXTRA_SYSTEM_KEY, systemKey) }
        )
    }
}
