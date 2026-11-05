package com.loanmate.loan.emicalculator.carloan.homelloan.loan.calc

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.loanmate.loan.emicalculator.carloan.homelloan.R
import kotlin.math.min

/**
 * Minimal donut. Shows the foreground arc swept proportionally to [percentage]
 * (0..100) over a soft track. Stroke width scales with the view bounds.
 */
class LoanDonutView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = ContextCompat.getColor(context, R.color.ln_brand_primary_light)
    }
    private val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        color = ContextCompat.getColor(context, R.color.ln_brand_primary)
    }
    private val ovalRect = RectF()

    /** 0..100 */
    var percentage: Float = 0f
        set(value) {
            field = value.coerceIn(0f, 100f)
            invalidate()
        }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val side = min(width, height).toFloat()
        if (side <= 0) return

        val stroke = side * 0.10f
        trackPaint.strokeWidth = stroke
        arcPaint.strokeWidth = stroke

        val inset = stroke / 2f + paddingLeft
        ovalRect.set(inset, inset, side - inset, side - inset)

        canvas.drawArc(ovalRect, 0f, 360f, false, trackPaint)
        if (percentage > 0f) {
            val sweep = 360f * (percentage / 100f)
            canvas.drawArc(ovalRect, -90f, sweep, false, arcPaint)
        }
    }
}
