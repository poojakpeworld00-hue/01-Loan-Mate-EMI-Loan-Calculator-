package com.loanmate.loan.emicalculator.carloan.homelloan.screens.leave

import android.app.AlertDialog
import android.content.res.Configuration
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.loanmate.loan.emicalculator.carloan.adkit.metrics.MetricsTwo
import com.loanmate.loan.emicalculator.carloan.adkit.metrics.Views
import com.loanmate.loan.emicalculator.carloan.homelloan.LoanTwo
import com.loanmate.loan.emicalculator.carloan.homelloan.R
import com.loanmate.loan.emicalculator.carloan.homelloan.store.cloud.flags.Follow

/**
 * The reference app's uninstall as an Android-style dialog (`uninstall_flow.system_dialog`), opened by Uninstall in the
 * launcher's long-press menu on our icon. No ads. OK: paid hides our icon from home and drawer (the app stays installed);
 * organic opens Android's LoanTwo info page, as the end of [ExitPathActivity]. Cancel / Back / outside: nothing.
 */
class ExitDialogActivity : AppCompatActivity() {
    private var dialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val config = (application as LoanTwo).container.config.value.uninstallFlow
        if (!config.premium || !config.systemDialog) return finish()
        if (savedInstanceState == null) MetricsTwo.step(MetricsTwo.UNINSTALL, "start")
        var ended = false
        // The phone's own alert theme, light or dark with the system, so it looks like that brand's uninstall dialog.
        val night = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        val theme = if (night) android.R.style.Theme_DeviceDefault_Dialog_Alert else android.R.style.Theme_DeviceDefault_Light_Dialog_Alert
        dialog = AlertDialog.Builder(this, theme)
            .setTitle(R.string.app_name)
            .setMessage(R.string.un_dialog_message)
            .setNegativeButton(android.R.string.cancel) { _, _ -> MetricsTwo.click("uninstall_cancel") }
            .setPositiveButton(android.R.string.ok) { _, _ ->
                ended = true
                MetricsTwo.click("uninstall_ok")
                endExit(this, (application as LoanTwo).container.config.value.track == Follow.B)
            }
            .setOnDismissListener {
                if (!ended) MetricsTwo.step(MetricsTwo.UNINSTALL, "cancel")
                finish()
            }
            .show()
    }

    override fun onResume() {
        super.onResume()
        Views.show("uninstall_dialog")
    }

    override fun onDestroy() {
        dialog?.setOnDismissListener(null)
        dialog?.dismiss()
        super.onDestroy()
    }
}
