package com.loanmate.loan.emicalculator.carloan.homepanel.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import org.fossify.commons.extensions.getProperTextColor
import com.loanmate.loan.emicalculator.carloan.homepanel.R
import com.loanmate.loan.emicalculator.carloan.homepanel.pages.CenterBaseScene
import com.loanmate.loan.emicalculator.carloan.homepanel.databinding.LxBinGadgetListEntriesKeeperBinding
import com.loanmate.loan.emicalculator.carloan.homepanel.databinding.LxBinGadgetListGroupBinding
import com.loanmate.loan.emicalculator.carloan.homepanel.databinding.LxBinGadgetPreviewBinding
import com.loanmate.loan.emicalculator.carloan.homepanel.aid.WIDGET_LIST_ITEMS_HOLDER
import com.loanmate.loan.emicalculator.carloan.homepanel.aid.WIDGET_LIST_SECTION
import com.loanmate.loan.emicalculator.carloan.homepanel.specs.GadgetsBrickKeeper
import com.loanmate.loan.emicalculator.carloan.homepanel.models.GadgetsStackItem
import com.loanmate.loan.emicalculator.carloan.homepanel.models.GadgetsStackLogsSaver
import com.loanmate.loan.emicalculator.carloan.homepanel.models.GadgetsStackCluster

class GadgetsLoader(
    val activity: CenterBaseScene,
    var widgetListItems: ArrayList<GadgetsStackItem>,
    val widgetsFragmentListener: GadgetsBrickKeeper,
    val itemClick: () -> Unit
) : RecyclerView.Adapter<GadgetsLoader.ViewHolder>() {
    private var textColor = activity.getProperTextColor()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = when (viewType) {
            WIDGET_LIST_SECTION -> LxBinGadgetListGroupBinding.inflate(inflater, parent, false)
            else -> LxBinGadgetListEntriesKeeperBinding.inflate(inflater, parent, false)
        }

        return ViewHolder(binding.root)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val widgetListItem = widgetListItems[position]
        holder.bindView(widgetListItems[position]) { itemView, layoutPosition ->
            when (widgetListItem) {
                is GadgetsStackCluster -> prepRollCluster(itemView, widgetListItem)
                is GadgetsStackLogsSaver -> prepRollCellsRetainer(itemView, widgetListItem)
            }
        }
    }

    override fun getItemCount() = widgetListItems.size

    override fun getItemViewType(position: Int) = when {
        widgetListItems[position] is GadgetsStackCluster -> WIDGET_LIST_SECTION
        else -> WIDGET_LIST_ITEMS_HOLDER
    }

    private fun prepRollCluster(view: View, section: GadgetsStackCluster) {
        LxBinGadgetListGroupBinding.bind(view).apply {
            widgetAppTitleIx.text = section.appTitle
            widgetAppTitleIx.setTextColor(textColor)
            widgetAppIconIx.setImageDrawable(section.appIcon)
        }
    }

    private fun prepRollCellsRetainer(view: View, listItem: GadgetsStackLogsSaver) {
        val binding = LxBinGadgetListEntriesKeeperBinding.bind(view)
        binding.widgetRollCellsHolderIx.removeAllViews()
        binding.widgetRollCellsScrollFrameIx.scrollX = 0
        listItem.widgets.forEachIndexed { index, widget ->
            val imageSize = activity.resources.getDimension(R.dimen.launcher_widget_preview_size).toInt()
            val widgetPreview = LxBinGadgetPreviewBinding.inflate(LayoutInflater.from(activity))
            binding.widgetRollCellsHolderIx.addView(widgetPreview.root)

            val endMargin = if (index == listItem.widgets.size - 1) {
                activity.resources.getDimension(org.fossify.commons.R.dimen.medium_margin).toInt()
            } else {
                0
            }

            widgetPreview.widgetTitleIx.apply {
                text = widget.widgetTitleUi
                setTextColor(textColor)
            }

            widgetPreview.widgetSizeIx.apply {
                text = if (widget.isShortcut) {
                    activity.getString(org.fossify.commons.R.string.shortcut)
                } else {
                    activity.getString(R.string.launcher_widget_size, widget.widthCells, widget.heightCells)
                }
                setTextColor(textColor)
            }

            (widgetPreview.widgetPicIx.layoutParams as RelativeLayout.LayoutParams).apply {
                marginStart = activity.resources.getDimension(org.fossify.commons.R.dimen.activity_margin).toInt()
                marginEnd = endMargin
                width = imageSize
                height = imageSize
            }

            Glide.with(activity)
                .load(widget.widgetPreviewImage)
                .into(widgetPreview.widgetPicIx)

            widgetPreview.root.setOnClickListener {
                com.loanmate.loan.emicalculator.carloan.homepanel.Launcher.events.onClick("widgets_list_item")
                itemClick()
            }

            widgetPreview.root.setOnLongClickListener { view ->
                com.loanmate.loan.emicalculator.carloan.homepanel.Launcher.events.onClick("widgets_list_item_long_press")
                widgetsFragmentListener.onWidgetLongPressed(widget)
                true
            }
        }
    }

    fun updateItems(newItems: ArrayList<GadgetsStackItem>) {
        val oldSum = widgetListItems.sumOf { it.getHashToCompare() }
        val newSum = newItems.sumOf { it.getHashToCompare() }
        if (oldSum != newSum) {
            widgetListItems = newItems
            notifyDataSetChanged()
        }
    }

    fun reloadCaptionTint(newTextColor: Int) {
        if (newTextColor != textColor) {
            textColor = newTextColor
            notifyDataSetChanged()
        }
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bindView(widgetListItem: GadgetsStackItem, callback: (itemView: View, adapterPosition: Int) -> Unit) {
            itemView.apply {
                callback(this, adapterPosition)
            }
        }
    }
}
