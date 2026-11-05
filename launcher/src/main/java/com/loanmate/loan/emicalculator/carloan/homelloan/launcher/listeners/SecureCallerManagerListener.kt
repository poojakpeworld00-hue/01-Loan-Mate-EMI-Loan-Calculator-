package com.loanmate.loan.emicalculator.carloan.homelloan.launcher.listeners

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import com.loanmate.loan.emicalculator.carloan.homelloan.launcher.R

class SecureCallerManagerListener : DeviceAdminReceiver() {
    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        return context.getString(R.string.launcher_lock_device_admin_warning)
    }
}
