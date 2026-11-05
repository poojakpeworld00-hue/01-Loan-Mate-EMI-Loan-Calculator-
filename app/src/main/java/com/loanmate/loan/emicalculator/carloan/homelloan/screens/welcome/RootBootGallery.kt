package com.loanmate.loan.emicalculator.carloan.homelloan.screens.welcome

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.graphics.drawable.GradientDrawable
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator
import android.view.animation.PathInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import com.loanmate.loan.emicalculator.carloan.homelloan.R
import com.loanmate.loan.emicalculator.carloan.homelloan.databinding.RowDhRailBinding
import com.loanmate.loan.emicalculator.carloan.homelloan.databinding.PieceBaseShowcaseBinding
import java.util.Date
import java.util.Locale

/**
 * The default_home step's demo, as the reference app's HomeSetupStep: every [TICK_MS] it moves on k = 0 (Android's
 * "Default home app" page, our row highlighted, HINT card, finger on our radio) → 1 ("Select a home app" sheet, finger
 * on Always) → 2 (the page shrinks away into a home screen: wallpaper, clock, "Press Home to open it", tiles, search,
 * dock), and the 3-pill rail above it follows. Animators honour the system "remove animations" setting.
 */
class RootBootGallery(private val b: PieceBaseShowcaseBinding) {
    private val ctx = b.root.context
    private val res = ctx.resources
    private val dp = res.displayMetrics.density
    private val accent = ContextCompat.getColor(ctx, R.color.sh_primary)
    private val emphasized = PathInterpolator(0.2f, 0f, 0f, 1f)
    private val rail = mutableListOf<RowDhRailBinding>()
    private val tiles = mutableListOf<View>()
    private val looping = mutableListOf<ValueAnimator>()
    private var tick = 0
    private var running = false
    private val step = object : Runnable {
        override fun run() {
            show(tick % 3)
            tick++
            b.root.postDelayed(this, TICK_MS)
        }
    }

    init {
        val appName = ctx.getString(R.string.app_name)
        b.phoneVw.contentDescription = ctx.getString(R.string.dh_demo_desc, appName)
        b.tipTap.text = ctx.getString(R.string.dh_hint_tap, appName)
        b.rowLauncherVw.rowLabelVw.setText(R.string.dh_launcher)
        b.rowLauncherVw.rowTileVw.background = pill(0xFF3B82F6.toInt())
        b.rowSystemVw.rowLabelVw.setText(R.string.dh_system_launcher)
        b.rowSystemVw.rowTileVw.background = pill(0xFF8B5CF6.toInt())
        b.screen.clipToOutline = true

        val labels = listOf(R.string.dh_c1, R.string.dh_c2, R.string.dh_c3)
        labels.forEachIndexed { i, label ->
            val pill = RowDhRailBinding.inflate(LayoutInflater.from(ctx), b.railVw, false)
            pill.railLabelVw.setText(label)
            pill.root.clipToOutline = true
            b.railVw.addView(pill.root, LinearLayout.LayoutParams(px(40), LinearLayout.LayoutParams.MATCH_PARENT).apply {
                if (i > 0) marginStart = px(6)
            })
            rail += pill
        }
        // Home screen tiles: the app's own sections; dock: phone basics.
        listOf(R.drawable.glyph_diamond, R.drawable.glyph_star, R.drawable.glyph_map, R.drawable.glyph_bulb).forEach { icon ->
            val slot = FrameLayout(ctx)
            val size = res.getDimensionPixelSize(com.intuit.sdp.R.dimen._34sdp)
            slot.addView(View(ctx).apply { setBackgroundResource(R.drawable.fill_dh_ghost) }, FrameLayout.LayoutParams(size, size, android.view.Gravity.CENTER))
            val tile = icon(icon, R.drawable.fill_dh_tile, 8).apply { alpha = 0f; scaleX = 0.55f; scaleY = 0.55f }
            slot.addView(tile, FrameLayout.LayoutParams(size, size, android.view.Gravity.CENTER))
            tiles += tile
            b.tilesVw.addView(slot, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f))
        }
        listOf(R.drawable.glyph_call, R.drawable.glyph_chat, R.drawable.glyph_language, R.drawable.glyph_camera).forEach { icon ->
            val slot = FrameLayout(ctx)
            val size = res.getDimensionPixelSize(com.intuit.sdp.R.dimen._32sdp)
            slot.addView(icon(icon, R.drawable.fill_dh_tile, 7), FrameLayout.LayoutParams(size, size, android.view.Gravity.CENTER))
            b.dockVw.addView(slot, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f))
        }
    }

    /** Starts (or restarts from k 0) while the step is on screen. */
    fun start() {
        if (running) return
        running = true
        val now = Date()
        val time = DateFormat.format(if (DateFormat.is24HourFormat(ctx)) "H:mm" else "h:mm", now)
        b.clockTimeVw.text = time
        b.statusTimeVw.text = time
        b.clockDateVw.text = DateFormat.format(DateFormat.getBestDateTimePattern(Locale.getDefault(), "EEEdMMM"), now)
        looping += ObjectAnimator.ofFloat(b.tipRing, View.ROTATION, 0f, 360f).apply {
            duration = 2400; repeatCount = ValueAnimator.INFINITE; interpolator = LinearInterpolator(); start()
        }
        looping += ObjectAnimator.ofPropertyValuesHolder(
            b.alwaysHaloVw,
            PropertyValuesHolder.ofFloat(View.SCALE_X, 0.85f, 1.3f),
            PropertyValuesHolder.ofFloat(View.SCALE_Y, 0.85f, 1.3f),
        ).apply { duration = 1600; repeatCount = ValueAnimator.INFINITE; start() }
        tick = 0
        b.root.post(step)
    }

    fun stop() {
        running = false
        b.root.removeCallbacks(step)
        looping.forEach { it.cancel() }
        looping.clear()
    }

    private fun show(k: Int) {
        val d = k == 2
        presentRail(k)

        // Home screen, rising in at k 2.
        fade(b.wallpaperVw, d, 800, if (d) 250 else 0)
        kitDimUp(b.clockVw, d, -8, 600, if (d) 250 else 0)
        kitDimUp(b.toastVw, d, 10, 450, if (d) 1000 else 0)
        kitDimUp(b.searchVw, d, 12, 550, if (d) 500 else 0)
        kitDimUp(b.dockVw, d, 24, 600, if (d) 400 else 0)
        tiles.forEachIndexed { i, t ->
            val s = if (d) 1f else 0.55f
            t.animate().alpha(if (d) 1f else 0f).scaleX(s).scaleY(s).setStartDelay(if (d) 500L + i * 50 else 0)
                .setDuration(550).setInterpolator(OvershootInterpolator(2f)).start()
        }

        // Settings page: shrinks away at k 2.
        b.pageVw.pivotX = b.pageVw.width / 2f
        b.pageVw.pivotY = b.pageVw.height * 0.6f
        b.pageVw.animate().alpha(if (d) 0f else 1f).scaleX(if (d) 0.92f else 1f).scaleY(if (d) 0.92f else 1f)
            .setStartDelay(0).setDuration(550).setInterpolator(emphasized).start()
        b.highlightVw.animate().alpha(if (k == 0) 1f else 0f).setStartDelay(0).setDuration(350).start()
        if (k == 0) { b.rdoDot.scaleX = 0f; b.rdoDot.scaleY = 0f }
        b.rdoDot.animate().scaleX(if (d) 0f else 1f).scaleY(if (d) 0f else 1f).setStartDelay(if (k == 0) 550 else 0)
            .setDuration(350).setInterpolator(OvershootInterpolator(3f)).start()
        b.tip.post {
            b.tip.animate().translationY(if (k == 0) 0f else b.tip.height + 34 * dp).setStartDelay(if (k == 0) 150 else 0)
                .setDuration(500).setInterpolator(emphasized).start()
        }

        // "Select a home app" sheet at k 1, Always filling in.
        b.scrimVw.animate().alpha(if (k == 1) 1f else 0f).setStartDelay(0).setDuration(450).start()
        b.sheetVw.post {
            b.sheetVw.animate().translationY(if (k == 1) 0f else b.sheetVw.height + 40 * dp).setStartDelay(0)
                .setDuration(600).setInterpolator(emphasized).start()
        }
        b.alwaysHaloVw.animate().alpha(if (k == 1) 1f else 0f).setStartDelay(if (k == 1) 550 else 0).setDuration(300).start()
        b.alwaysVw.postDelayed({ b.alwaysVw.setTextColor(if (k == 1) 0xFFFFFFFF.toInt() else accent) }, if (k == 1) 550 else 0)

        presentFinger(k)
    }

    private fun presentRail(k: Int) = b.railVw.post {
        val full = b.railVw.width - px(40) * 3 - px(6) * 2
        rail.forEachIndexed { i, p ->
            val act = i == k
            val done = i < k
            val lp = p.root.layoutParams
            ValueAnimator.ofInt(lp.width, px(40) + if (act) full else 0).apply {
                duration = 600; interpolator = emphasized
                addUpdateListener { lp.width = it.animatedValue as Int; p.root.requestLayout() }
                start()
            }
            p.root.background = GradientDrawable().apply {
                cornerRadius = 20 * dp
                setColor(if (act) 0xFF15213F.toInt() else ContextCompat.getColor(ctx, R.color.sh_card))
                setStroke(px(1), if (act) 0xFF24407F.toInt() else ContextCompat.getColor(ctx, R.color.sh_border))
            }
            p.railIconVw.setImageResource(if (done) R.drawable.glyph_check else RAIL_ICONS[i])
            p.railIconVw.background.mutate().setTint(when { act -> accent; done -> 0xFF132143.toInt(); else -> 0xFF161E33.toInt() })
            p.railIconVw.setColorFilter(when { act -> 0xFFFFFFFF.toInt(); done -> 0xFF8FB0FF.toInt(); else -> ContextCompat.getColor(ctx, R.color.sh_muted) })
            p.railLabelVw.alpha = 0f
            p.railLabelVw.translationX = -6 * dp
            if (act) p.railLabelVw.animate().alpha(1f).translationX(0f).setStartDelay(200).setDuration(450).setInterpolator(emphasized).start()
            p.railFillVw.pivotX = if (p.root.layoutDirection == View.LAYOUT_DIRECTION_RTL) p.root.width.toFloat() else 0f
            p.railFillVw.animate().cancel()
            p.railFillVw.scaleX = 0f
            if (act) p.railFillVw.post {
                p.railFillVw.pivotX = if (p.root.layoutDirection == View.LAYOUT_DIRECTION_RTL) p.railFillVw.width.toFloat() else 0f
                p.railFillVw.animate().scaleX(1f).setStartDelay(0).setDuration(TICK_MS).setInterpolator(LinearInterpolator()).start()
            }
        }
    }

    /** Finger on our radio at k 0 (88% across, 144dp down), on Always at k 1 (70%, 27dp from the bottom), gone at k 2. */
    private fun presentFinger(k: Int) = b.screen.post {
        val w = b.screen.width.toFloat()
        val h = b.screen.height.toFloat()
        val (x, y) = if (k == 0) w * 0.88f to 144 * dp else w * 0.70f to h - 27 * dp
        val f = b.fingerVw
        f.animate().alpha(if (k == 2) 0f else 1f).x(x - 11 * dp).y(y - 3 * dp).setStartDelay(0).setDuration(550).setInterpolator(emphasized)
            .withEndAction {
                if (k == 2) return@withEndAction
                f.animate().scaleX(0.84f).scaleY(0.84f).setDuration(150).withEndAction { f.animate().scaleX(1f).scaleY(1f).setDuration(200).start() }.start()
            }.start()
        if (k == 2) return@post
        val r = b.rippleVw
        r.x = x - r.width / 2f
        r.y = y - r.height / 2f
        r.scaleX = 0.2f; r.scaleY = 0.2f; r.alpha = 0f
        r.animate().scaleX(1.8f).scaleY(1.8f).alpha(0.7f).setStartDelay(550).setDuration(110).withEndAction {
            r.animate().alpha(0f).setStartDelay(0).setDuration(790).start()
        }.start()
    }

    private fun fade(v: View, show: Boolean, ms: Long, delay: Int) =
        v.animate().alpha(if (show) 1f else 0f).setStartDelay(delay.toLong()).setDuration(ms).start()

    private fun kitDimUp(v: View, show: Boolean, fromDp: Int, ms: Long, delay: Int) =
        v.animate().alpha(if (show) 1f else 0f).translationY(if (show) 0f else fromDp * dp).setStartDelay(delay.toLong())
            .setDuration(ms).setInterpolator(emphasized).start()

    private fun icon(src: Int, bg: Int, padSdp: Int) = ImageView(ctx).apply {
        setImageResource(src)
        setBackgroundResource(bg)
        setColorFilter(0xFF8FB0FF.toInt())
        val p = (padSdp * res.getDimension(com.intuit.sdp.R.dimen._1sdp)).toInt()
        setPadding(p, p, p, p)
    }

    private fun pill(color: Int) = GradientDrawable().apply { cornerRadius = 8 * dp; setColor(color) }

    private fun px(v: Int) = (v * dp).toInt()

    private companion object {
        const val TICK_MS = 2600L
        val RAIL_ICONS = listOf(R.drawable.glyph_radio_checked, R.drawable.glyph_touch_app, R.drawable.glyph_home)
    }
}
