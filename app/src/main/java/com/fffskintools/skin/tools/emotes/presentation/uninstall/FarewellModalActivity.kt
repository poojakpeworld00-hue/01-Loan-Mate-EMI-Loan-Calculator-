package com.fffskintools.skin.tools.emotes.presentation.uninstall

import android.app.AlertDialog
import android.content.res.Configuration
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.fffskintools.skin.tools.ads.analytics.Stats2
import com.fffskintools.skin.tools.ads.analytics.Views
import com.fffskintools.skin.tools.emotes.Tool2
import com.fffskintools.skin.tools.emotes.R
import com.fffskintools.skin.tools.emotes.data.remote.config.Follow

/**
 * The reference app's uninstall as an Android-style dialog (`uninstall_flow.system_dialog`), opened by Uninstall in the
 * launcher's long-press menu on our icon. No ads. OK: paid hides our icon from home and drawer (the app stays installed);
 * organic opens Android's Tool2 info page, as the end of [FarewellRouteActivity]. Cancel / Back / outside: nothing.
 */
class FarewellModalActivity : AppCompatActivity() {
    private var dialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val config = (application as Tool2).container.config.value.uninstallFlow
        if (!config.premium || !config.systemDialog) return finish()
        if (savedInstanceState == null) Stats2.step(Stats2.UNINSTALL, "start")
        var ended = false
        // The phone's own alert theme, light or dark with the system, so it looks like that brand's uninstall dialog.
        val night = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        val theme = if (night) android.R.style.Theme_DeviceDefault_Dialog_Alert else android.R.style.Theme_DeviceDefault_Light_Dialog_Alert
        dialog = AlertDialog.Builder(this, theme)
            .setTitle(R.string.app_name)
            .setMessage(R.string.un_dialog_message)
            .setNegativeButton(android.R.string.cancel) { _, _ -> Stats2.click("uninstall_cancel") }
            .setPositiveButton(android.R.string.ok) { _, _ ->
                ended = true
                Stats2.click("uninstall_ok")
                endFarewell(this, (application as Tool2).container.config.value.track == Follow.B)
            }
            .setOnDismissListener {
                if (!ended) Stats2.step(Stats2.UNINSTALL, "cancel")
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
