package com.loanmate.loan.emicalculator.carloan.homepanel.overlays

import android.app.Activity
import android.app.AlertDialog
import org.fossify.commons.extensions.*
import org.fossify.commons.helpers.ensureBackgroundThread
import com.loanmate.loan.emicalculator.carloan.homepanel.databinding.HpPopupRenameBinBinding
import com.loanmate.loan.emicalculator.carloan.homepanel.helpers.homeScreenGridItemsDB
import com.loanmate.loan.emicalculator.carloan.homepanel.models.DeskViewerGridItem

class RetitleItemPanel(val activity: Activity, val item: DeskViewerGridItem, val callback: () -> Unit) {
    init {
        val binding = HpPopupRenameBinBinding.inflate(activity.layoutInflater)
        val view = binding.root
        binding.renameBoxEdittextIxVw.setText(item.title)

        activity.getAlertDialogBuilder()
            .setPositiveButton(org.fossify.commons.R.string.ok, null)
            .setNegativeButton(org.fossify.commons.R.string.cancel) { _, _ -> com.loanmate.loan.emicalculator.carloan.homepanel.Launcher.events.onClick("rename_cancel") }
            .apply {
                activity.setupDialogStuff(view, this, org.fossify.commons.R.string.rename) { alertDialog ->
                    alertDialog.showKeyboard(binding.renameBoxEdittextIxVw)
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        com.loanmate.loan.emicalculator.carloan.homepanel.Launcher.events.onClick("rename_ok")
                        val newTitle = binding.renameBoxEdittextIxVw.value
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
