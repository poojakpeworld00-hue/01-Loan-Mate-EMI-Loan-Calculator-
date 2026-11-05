package com.loanmate.loan.emicalculator.carloan.homepanel.sheets

import android.content.Context
import android.util.AttributeSet
import android.widget.RelativeLayout
import androidx.viewbinding.ViewBinding
import com.loanmate.loan.emicalculator.carloan.homepanel.pages.XCenterScene

abstract class CenterPanel<BINDING : ViewBinding>(
    context: Context,
    attributeSet: AttributeSet
) : RelativeLayout(context, attributeSet) {
    protected var activity: XCenterScene? = null
    protected lateinit var binding: BINDING

    abstract fun setupFragment(activity: XCenterScene)
}
