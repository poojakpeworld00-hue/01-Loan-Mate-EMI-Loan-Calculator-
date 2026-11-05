package com.loanmate.loan.emicalculator.carloan.homelloan.screens.library

import com.loanmate.loan.emicalculator.carloan.homelloan.screens.shared.wireScreenPromo
import com.loanmate.loan.emicalculator.carloan.adkit.Spots
import com.loanmate.loan.emicalculator.carloan.adkit.metrics.Views
import com.loanmate.loan.emicalculator.carloan.adkit.metrics.MetricsTwo
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Toast
import com.loanmate.loan.emicalculator.carloan.homelloan.screens.shared.kitEnableLightEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.loanmate.loan.emicalculator.carloan.homelloan.databinding.ScreenFocusBinding
import com.loanmate.loan.emicalculator.carloan.homelloan.databinding.RowStatBinding
import com.loanmate.loan.emicalculator.carloan.homelloan.logic.entity.ShelfOrder
import com.loanmate.loan.emicalculator.carloan.homelloan.screens.shared.toolScreenModel
import com.loanmate.loan.emicalculator.carloan.homelloan.screens.shared.kitCollectWhenStarted
import com.loanmate.loan.emicalculator.carloan.homelloan.screens.shared.padForSysBars

/** Image, stats, description and a Download button for one item. */
class SpotlightActivity : AppCompatActivity() {
    private val viewModel by toolScreenModel { handle -> SpotlightScreenModel(catalogRepository, mediaRepository, handle) }

    // Saving to Pictures needs the storage permission only before Android 10.
    private val requestWrite = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) viewModel.download()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        kitEnableLightEdgeToEdge()
        val b = ScreenFocusBinding.inflate(layoutInflater)
        setContentView(b.root)
        b.root.padForSysBars()
        b.crest.backVw.setOnClickListener { finish() }

        val item = viewModel.item
        b.crest.titleVw.text = item.title
        b.imageVw.setImageResource(item.image)
        if (viewModel.type.itemShape.fullBleed) {
            // Wide artwork: show it edge to edge at its own aspect ratio instead of inside the square well.
            b.imageVw.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
            b.imageVw.adjustViewBounds = true
            b.imageVw.setPadding(0, 0, 0, 0)
        }
        b.imageVw.contentDescription = item.name
        b.nameVw.text = item.name
        b.descVw.text = item.desc
        item.stats.forEach { (label, value) ->
            val s = RowStatBinding.inflate(layoutInflater, b.statsVw, true)
            s.labelVw.setText(label)
            s.valueVw.text = value
        }
        b.downloadVw.setOnClickListener {
            MetricsTwo.click("download", item.name)
            val needsPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                PackageManager.PERMISSION_GRANTED
            if (needsPermission) requestWrite.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE) else viewModel.download()
        }
        kitCollectWhenStarted(viewModel.messages) { Toast.makeText(this, it, Toast.LENGTH_SHORT).show() }
        wireScreenPromo("screens.detail", b.adInlineVw, b.adSlotVw)
    }

    override fun onResume() {
        super.onResume()
        Views.show("detail")
    }

    companion object {
        const val EXTRA_CATALOG = "catalog"
        const val EXTRA_GROUP = "group"
        const val EXTRA_ITEM = "item"

        fun intent(context: Context, catalog: ShelfOrder, group: Int, item: Int): Intent =
            Intent(context, SpotlightActivity::class.java)
                .putExtra(EXTRA_CATALOG, catalog.ordinal)
                .putExtra(EXTRA_GROUP, group)
                .putExtra(EXTRA_ITEM, item)
    }
}
