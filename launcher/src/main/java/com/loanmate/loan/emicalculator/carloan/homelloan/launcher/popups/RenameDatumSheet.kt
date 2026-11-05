package com.loanmate.loan.emicalculator.carloan.homelloan.launcher.popups

import android.app.Activity
import android.app.AlertDialog
import org.fossify.commons.extensions.*
import org.fossify.commons.helpers.ensureBackgroundThread
import com.loanmate.loan.emicalculator.carloan.homelloan.launcher.databinding.LxPopupRenameBinBinding
import com.loanmate.loan.emicalculator.carloan.homelloan.launcher.exts.homeScreenGridItemsDB
import com.loanmate.loan.emicalculator.carloan.homelloan.launcher.entities.LobbyLookerMatrixDatum

class RenameDatumSheet(val activity: Activity, val item: LobbyLookerMatrixDatum, val callback: () -> Unit) {
    init {
        val binding = LxPopupRenameBinBinding.inflate(activity.layoutInflater)
        val view = binding.root
        binding.renameBoxEdittextIx.setText(item.title)

        activity.getAlertDialogBuilder()
            .setPositiveButton(org.fossify.commons.R.string.ok, null)
            .setNegativeButton(org.fossify.commons.R.string.cancel) { _, _ -> com.loanmate.loan.emicalculator.carloan.homelloan.launcher.Launcher.events.onClick("rename_cancel") }
            .apply {
                activity.setupDialogStuff(view, this, org.fossify.commons.R.string.rename) { alertDialog ->
                    alertDialog.showKeyboard(binding.renameBoxEdittextIx)
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        com.loanmate.loan.emicalculator.carloan.homelloan.launcher.Launcher.events.onClick("rename_ok")
                        val newTitle = binding.renameBoxEdittextIx.value
                        if (newTitle.isNotEmpty()) {
                            ensureBackgroundThread {
                                val result = activity.homeScreenGridItemsDB.reloadCellTitle(newTitle, item.id!!)
                                if (result == 1) {
                                    callback()
                                    alertDialog.dismiss()
                                } else {
                                    activity.toast(org.fossify.commons.R.string.unknown_error_occurred)
                                }
                            }
                        } else {
                            activity.toast(org.fossify.commons.R.string.value_cannot_be_empty)
                        }
                    }
                }
            }
    }
}
