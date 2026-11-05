package com.fffskintools.skin.tools.emotes.presentation.calculator

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.fffskintools.skin.tools.emotes.R
import com.fffskintools.skin.tools.emotes.domain.model.ReckonYield
import com.fffskintools.skin.tools.emotes.domain.model.ReckonerSort
import java.util.Locale

@get:StringRes
val ReckonerSort.title: Int
    get() = when (this) {
        ReckonerSort.DIAMONDS_TO_DOLLARS -> R.string.calc_dollars
        ReckonerSort.ELITE_PASS -> R.string.calc_elite_pass
        ReckonerSort.DOLLARS_TO_DIAMONDS -> R.string.calc_diamonds
        ReckonerSort.DAILY -> R.string.calc_daily
    }

@get:StringRes
val ReckonerSort.inputHint: Int
    get() = when (this) {
        ReckonerSort.DIAMONDS_TO_DOLLARS -> R.string.calc_input_diamonds
        ReckonerSort.ELITE_PASS -> R.string.calc_input_passes
        ReckonerSort.DOLLARS_TO_DIAMONDS -> R.string.calc_input_dollars
        ReckonerSort.DAILY -> R.string.calc_input_days
    }

@get:DrawableRes
val ReckonerSort.image: Int
    get() = when (this) {
        ReckonerSort.DIAMONDS_TO_DOLLARS -> R.drawable.cal1
        ReckonerSort.ELITE_PASS -> R.drawable.cal5
        ReckonerSort.DOLLARS_TO_DIAMONDS -> R.drawable.cal7
        ReckonerSort.DAILY -> R.drawable.cal6
    }

fun ReckonYield.format(context: Context): String = when (this) {
    is ReckonYield.KitDollars -> String.format(Locale.US, "$%.2f", value)
    is ReckonYield.KitDiamonds -> context.getString(R.string.calc_diamonds_result, value)
}
