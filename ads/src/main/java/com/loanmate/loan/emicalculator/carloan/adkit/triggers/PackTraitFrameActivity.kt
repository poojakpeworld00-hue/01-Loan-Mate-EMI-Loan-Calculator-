package com.loanmate.loan.emicalculator.carloan.adkit.triggers

import android.os.Bundle
import android.view.LayoutInflater
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding
import com.loanmate.loan.emicalculator.carloan.adkit.metrics.MetricsTwo
import com.loanmate.loan.emicalculator.carloan.adkit.metrics.Views

/** Base of the module's event screens (MSG-06 base without its app locale and back interstitial). */
abstract class PackTraitFrameActivity<VB : ViewBinding> : AppCompatActivity() {

    protected lateinit var binding: VB
        private set

    protected abstract fun inflateBinding(inflater: LayoutInflater): VB

    /** The `screen` name of this event screen. */
    protected abstract val screenName: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = inflateBinding(layoutInflater)
        setContentView(binding.root)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                MetricsTwo.hotkeyPress("back")
                performBack()
            }
        })

        initView()
    }

    override fun onResume() {
        super.onResume()
        Views.show(screenName)
    }

    protected open fun performBack() {
        if (!isFinishing) finish()
    }

    protected open fun initView() {}
}
