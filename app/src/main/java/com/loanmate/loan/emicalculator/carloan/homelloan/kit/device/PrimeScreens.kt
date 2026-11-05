package com.loanmate.loan.emicalculator.carloan.homelloan.kit.device

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import com.loanmate.loan.emicalculator.carloan.adkit.ExitPromoRunner
import com.loanmate.loan.emicalculator.carloan.homelloan.LoanTwo
import com.loanmate.loan.emicalculator.carloan.homelloan.screens.dashboard.RootActivity
import com.loanmate.loan.emicalculator.carloan.homelloan.screens.welcome.OnboardActivity
import com.loanmate.loan.emicalculator.carloan.homelloan.screens.intro.StartActivity

/**
 * The reference app's single MainActivity, mapped onto FFF's screens: before onboarding is done the "main screen" is
 * [OnboardActivity], after it [RootActivity]. A task of ours that started on one of [classes] is an app task (not the
 * launcher's home task).
 */
object PrimeScreens {
    private val classes = setOf(StartActivity::class.java.name, OnboardActivity::class.java.name, RootActivity::class.java.name)

    fun kitIsPrime(activity: Activity) = activity is OnboardActivity || activity is RootActivity

    fun isLoanJobRoot(className: String?) = className in classes

    fun onboardingDone(context: Context) = (context.applicationContext as LoanTwo).container.settingsRepository.isOnboarded()

    private fun kitPrimeClass(context: Context): Class<out Activity> =
        if (onboardingDone(context)) RootActivity::class.java else OnboardActivity::class.java

    /** Our launcher home through Android's own Home intent, so it is the one in the system's home stack. */
    fun deckRootIntent(context: Context): Intent =
        Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).setPackage(context.packageName).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /** Brings the task of our app screens to the front as it is, whatever is on top of it. */
    fun toolJobToFront(context: Context): Boolean {
        val task = context.getSystemService(ActivityManager::class.java)?.appTasks?.firstOrNull {
            val info = it.taskInfo
            isLoanJobRoot(info?.baseIntent?.component?.className) || isLoanJobRoot(info?.baseActivity?.className)
        } ?: return false
        return runCatching { task.moveToFront() }.isSuccess
    }

    fun open(context: Context, systemKey: String? = null) {
        // An exit ad (its ad screen or link tabs) is on top of our screen: bring the task back as it is, so the ad finishes.
        if (ExitPromoRunner.busy() && toolJobToFront(context)) return
        context.startActivity(
            Intent(context, kitPrimeClass(context))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                .apply { if (systemKey != null) putExtra(SysShortcutAim.EXTRA_SYSTEM_KEY, systemKey) }
        )
    }
}
