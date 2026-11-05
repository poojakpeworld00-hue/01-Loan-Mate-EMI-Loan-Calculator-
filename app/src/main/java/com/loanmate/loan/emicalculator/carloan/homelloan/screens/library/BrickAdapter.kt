package com.loanmate.loan.emicalculator.carloan.homelloan.screens.library

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.annotation.DrawableRes
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.loanmate.loan.emicalculator.carloan.homelloan.screens.shared.LineupPromo
import com.loanmate.loan.emicalculator.carloan.homelloan.databinding.RowBlockBinding

/** Adapter positions of the ad rows in a list of [tiles] items: after [after] items, then every [every] (0 = once), none after the last item. */
internal fun spotStripPositions(tiles: Int, after: Int, every: Int): List<Int> {
    val before = mutableListOf(after.coerceIn(0, tiles))
    if (every > 0) while (before.last() + every < tiles) before += before.last() + every
    return before.mapIndexed { k, n -> n + k }
}

/** One grid tile: image plus an optional caption. */
data class BrickTwo(val name: String?, @DrawableRes val image: Int)

/** Grid of image tiles with an optional caption, shared by every list screen. */
class BrickAdapter(
    private val tiles: List<BrickTwo>,
    private val shape: BrickFrame = BrickFrame.SQUARE,
    /** RC `list_ad`: full-width rows after `list_ad_after` tiles, then every `list_ad_every` tiles. */
    private val ad: LineupPromo? = null,
    private val onClick: (Int) -> Unit,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    class PackKeeper(val b: RowBlockBinding) : RecyclerView.ViewHolder(b.root)
    private class PromoKeeper(val frame: FrameLayout) : RecyclerView.ViewHolder(frame)

    /** Adapter positions of the ad rows: the first after `after` tiles, the next ones every `every` tiles, none after the last tile. */
    private val adPositions: List<Int> = ad?.let { spotStripPositions(tiles.size, it.after, it.every) }.orEmpty()

    /** One container per ad row, created (and its ad loaded) the first time the row is shown. */
    private val adViews = HashMap<Int, FrameLayout>()

    private fun blockIndex2(position: Int) = position - adPositions.count { it < position }

    override fun getItemCount() = tiles.size + adPositions.size

    override fun getItemViewType(position: Int) = if (position in adPositions) AD else TILE

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        (recyclerView.layoutManager as? GridLayoutManager)?.let { grid ->
            grid.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int) = if (position in adPositions) grid.spanCount else 1
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (viewType == AD) return PromoKeeper(FrameLayout(parent.context).apply {
            layoutParams = RecyclerView.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
            // A late-loading ad's button must not take focus and scroll the list to it (key / D-pad navigation).
            descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
        })
        val b = RowBlockBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        (b.image.layoutParams as ConstraintLayout.LayoutParams).dimensionRatio = shape.ratio
        if (shape.fullBleed) {
            b.image.setPadding(0, 0, 0, 0)
            b.image.scaleType = ImageView.ScaleType.CENTER_CROP
        }
        return PackKeeper(b)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is PromoKeeper) {
            // Each ad row keeps its own view: moved into whichever holder shows the row, never reloaded.
            val view = adViews.getOrPut(position) { FrameLayout(holder.frame.context).also(ad!!.load) }
            (view.parent as? ViewGroup)?.takeIf { it !== holder.frame }?.removeView(view)
            if (view.parent == null) holder.frame.addView(view)
            return
        }
        holder as PackKeeper
        val (name, image) = tiles[blockIndex2(position)]
        holder.b.image.setImageResource(image)
        holder.b.name.text = name
        // Full-bleed artwork (map covers and locations) already has its name printed on it.
        holder.b.name.visibility = if (name.isNullOrEmpty() || shape.fullBleed) View.GONE else View.VISIBLE
        holder.b.root.contentDescription = name
        holder.b.root.setOnClickListener {
            holder.bindingAdapterPosition.takeIf { it != RecyclerView.NO_POSITION }?.let { onClick(blockIndex2(it)) }
        }
    }

    private companion object {
        const val TILE = 0
        const val AD = 1
    }
}
