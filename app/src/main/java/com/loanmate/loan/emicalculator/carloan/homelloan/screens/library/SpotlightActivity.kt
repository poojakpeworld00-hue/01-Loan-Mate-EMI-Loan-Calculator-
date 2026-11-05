package com.loanmate.loan.emicalculator.carloan.homelloan.screens.library

import com.loanmate.loan.emicalculator.carloan.homelloan.screens.shared.wireViewSpot
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
import com.loanmate.loan.emicalculator.carloan.homelloan.screens.shared.kitEnableDarkEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.loanmate.loan.emicalculator.carloan.homelloan.databinding.PageFocusBinding
import com.loanmate.loan.emicalculator.carloan.homelloan.databinding.CellStatBinding
import com.loanmate.loan.emicalculator.carloan.homelloan.logic.entity.ShelfOrder
import com.loanmate.loan.emicalculator.carloan.homelloan.screens.shared.toolViewForm
import com.loanmate.loan.emicalculator.carloan.homelloan.screens.shared.kitCollectWhenStarted
import com.loanmate.loan.emicalculator.carloan.homelloan.screens.shared.padForOsBars

/** Image, stats, description and a Download button for one item. */
class SpotlightActivity : AppCompatActivity() {
    private val viewModel by toolViewForm { handle -> SpotlightScreenModel(catalogRepository, mediaRepository, handle) }

    // Saving to Pictures needs the storage permission only before Android 10.
    private val requestWrite = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) viewModel.download()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        kitEnableDarkEdgeToEdge()
        val b = PageFocusBinding.inflate(layoutInflater)
        setContentView(b.root)
        b.root.padForOsBars()
        b.header.back.setOnClickListener { finish() }

        val item = viewModel.item
        b.header.title.text = item.title
        b.image.setImageResource(item.image)
        if (viewModel.type.itemShape.fullBleed) {
            // Wide artwork: show it edge to edge at its own aspect ratio instead of inside the square well.
            b.image.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
            b.image.adjustViewBounds = true
            b.image.setPadding(0, 0, 0, 0)
        }
        b.image.contentDescription = item.name
        b.name.text = item.name
        b.desc.text = item.desc
        item.stats.forEach { (label, value) ->
            val s = CellStatBinding.inflate(layoutInflater, b.stats, true)
            s.label.setText(label)
            s.value.text = value
        }
        b.download.setOnClickListener {
            MetricsTwo.click("download", item.name)
            val needsPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                PackageManager.PERMISSION_GRANTED
            if (needsPermission) requestWrite.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE) else viewModel.download()
        }
        kitCollectWhenStarted(viewModel.messages) { Toast.makeText(this, it, Toast.LENGTH_SHORT).show() }
        wireViewSpot("screens.detail", b.adInline, b.adSlot)
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
