package com.loanmate.loan.emicalculator.carloan.homelloan.launcher.ui

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View

class LobbyLookerMatrixDrawingArea @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        (parent as LobbyLookerMatrix).drawInto(canvas)
    }
}
