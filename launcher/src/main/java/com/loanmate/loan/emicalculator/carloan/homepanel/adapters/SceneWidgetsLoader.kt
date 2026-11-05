package com.loanmate.loan.emicalculator.carloan.homepanel.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.loanmate.loan.emicalculator.carloan.homepanel.R
import com.loanmate.loan.emicalculator.carloan.homepanel.aid.IconFetcher
import com.loanmate.loan.emicalculator.carloan.homepanel.models.WidgetCenter

class SceneWidgetsLoader(
    private val layoutRes: Int,
    private val itemClick: (WidgetCenter) -> Unit,
) : ListAdapter<WidgetCenter, SceneWidgetsLoader.ViewHolder>(WidgetCenterDeltaHook()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(layoutRes, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.attachScreen(getItem(position))
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun attachScreen(launcher: WidgetCenter) {
            itemView.findViewById<TextView>(R.id.cell_headingAltIxVw).text = launcher.title

            // the icons are already loaded in memory by the drawer, no need for Glide here; one that
            // is not yet (a cold start) is decoded on its own instead of waiting for the full scan
            val icon = itemView.findViewById<ImageView>(R.id.box_iconIxVw)
            val ready = launcher.drawable ?: IconFetcher.cached(launcher.packageName, launcher.activityName)
            icon.setImageDrawable(ready)
            if (ready == null) {
                val wanted = launcher.packageName + "/" + launcher.activityName
                icon.tag = wanted
                IconFetcher.load(itemView.context, launcher.packageName, launcher.activityName) { loaded ->
                    if (icon.tag == wanted) icon.setImageDrawable(loaded)
                }
            } else {
                icon.tag = null
            }
            itemView.setOnClickListener { itemClick(launcher) }
        }
    }
}
