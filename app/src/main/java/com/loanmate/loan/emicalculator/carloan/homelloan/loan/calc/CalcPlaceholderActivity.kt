package com.loanmate.loan.emicalculator.carloan.homelloan.loan.calc

import android.content.Context
import android.content.Intent
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.loanmate.loan.emicalculator.carloan.homelloan.screens.shared.kitEnableLightEdgeToEdge
import com.loanmate.loan.emicalculator.carloan.homelloan.screens.shared.padForSysBars
import com.loanmate.loan.emicalculator.carloan.homelloan.R
import com.loanmate.loan.emicalculator.carloan.homelloan.databinding.LnActivityCalcDetailBinding

/**
 * Generic placeholder destination used by Home tiles whose real screen has not
 * been built yet. Each caller passes a title (and optional icon) via
 * [newIntent]; the activity renders a top bar + a centred "Coming soon" body.
 */
class CalcPlaceholderActivity : AppCompatActivity() {

    private lateinit var binding: LnActivityCalcDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        kitEnableLightEdgeToEdge()
        binding = LnActivityCalcDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.padForSysBars()
        initView()
        initListeners()
    }

    private fun initView() {
        val titleRes = intent.getIntExtra(EXTRA_TITLE_RES, 0)
        if (titleRes != 0) binding.tvTitle.setText(titleRes)

        val iconRes = intent.getIntExtra(EXTRA_ICON_RES, 0)
        if (iconRes != 0) {
            binding.iconHolder.removeAllViews()
            val sizePx = (36 * resources.displayMetrics.density).toInt()
            val iv = ImageView(this).apply {
                layoutParams = FrameLayout.LayoutParams(sizePx, sizePx).apply {
                    gravity = Gravity.CENTER
                }
                setImageResource(iconRes)
                imageTintList = ContextCompat.getColorStateList(
                    this@CalcPlaceholderActivity,
                    R.color.ln_brand_primary
                )
                contentDescription = null
            }
            binding.iconHolder.addView(iv)
        }
    }

    private fun initListeners() {
        binding.btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    companion object {
        private const val EXTRA_TITLE_RES = "extra_title_res"
        private const val EXTRA_ICON_RES = "extra_icon_res"

        fun newIntent(
            context: Context,
            @StringRes titleRes: Int,
            @DrawableRes iconRes: Int = 0
        ): Intent = Intent(context, CalcPlaceholderActivity::class.java).apply {
            putExtra(EXTRA_TITLE_RES, titleRes)
            if (iconRes != 0) putExtra(EXTRA_ICON_RES, iconRes)
        }
    }
}
