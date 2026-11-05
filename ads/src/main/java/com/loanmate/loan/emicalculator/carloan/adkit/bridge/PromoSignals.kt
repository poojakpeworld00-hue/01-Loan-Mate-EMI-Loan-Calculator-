package com.loanmate.loan.emicalculator.carloan.adkit.bridge

import com.loanmate.loan.emicalculator.carloan.adkit.metrics.MetricsTwo
import com.loanmate.loan.emicalculator.carloan.adkit.metrics.Views
import com.loanmate.loan.emicalculator.carloan.adkit.setup.PromoStep
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdValue
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.ResponseInfo

/**
 * The `ad` and `ad_impression` events of every renderer, both sent through [MetricsTwo].
 * `ad_place` is the RC place key the ad runs for: [place] while an exit_ad runs ([com.loanmate.loan.emicalculator.carloan.adkit.ExitPromoRunner]),
 * the slot's key for bottom / list slots, and left out ("") for background preloads.
 */
object PromoSignals {
    const val BANNER = "banner"
    const val NATIVE = "native"
    const val INTER = "inter"
    const val REWARDED = "rewarded"
    const val APP_OPEN = "app_open"
    const val CUSTOM = "custom"

    /** Place key of the exit_ad run in progress; "" outside one. */
    @Volatile var place = ""

    fun send(format: String, step: String, place: String, errorCode: Int? = null) = MetricsTwo.log(
        MetricsTwo.AD,
        "screen" to Views.current,
        "ad_place" to place.ifEmpty { null },
        "format" to format,
        "step" to step,
        "error_code" to errorCode,
    )

    fun fetchFailed(format: String, code: Int, place: String) = send(format, failStep(code), place, code)

    /** AdMob error code 3 (ERROR_CODE_NO_FILL) is `no_fill`, any other code `failed`. */
    fun failStep(code: Int) = if (code == 3) "no_fill" else "failed"

    /** AdMob paid event → `ad_impression`. [label] = engine placement (banner, native_big, inter_chain, app_open_resume…). */
    fun paid(value: AdValue, label: String, unitId: String?, info: ResponseInfo?, place: String) = MetricsTwo.log(
        MetricsTwo.AD_IMPRESSION,
        "ad_platform" to "admob",
        "ad_source" to (runCatching { info?.loadedAdapterResponseInfo?.adSourceName }.getOrNull()?.takeIf { it.isNotBlank() } ?: "unknown"),
        "ad_format" to kitStandardFormat(label),
        "ad_unit_name" to (unitId?.takeIf { it.isNotBlank() } ?: "unknown"),
        "value" to value.valueMicros / 1_000_000.0,
        "currency" to (value.currencyCode.takeUnless { it.isNullOrBlank() } ?: "USD"),
        "ad_placement" to label,
        "ad_screen" to Views.current,
        "user_segment" to MetricsTwo.segment(),
        "ad_place" to place.ifEmpty { null },
    )

    /** Engine label → Firebase ad_format (banner / native / interstitial / rewarded / app_open). */
    fun kitStandardFormat(label: String): String = when {
        label.startsWith("app_open") -> "app_open"
        label.startsWith("rewarded") -> "rewarded"
        label.startsWith("inter") -> "interstitial"
        label.startsWith("native") -> "native"
        label.startsWith("banner") -> "banner"
        else -> label
    }

    /** `format` of an exit_ad step. */
    fun formatOf(step: PromoStep): String = when (step) {
        PromoStep.INTER -> INTER
        PromoStep.REWARDED -> REWARDED
        PromoStep.APP_OPEN -> APP_OPEN
        PromoStep.FULL_NATIVE -> NATIVE
        PromoStep.CUSTOM, PromoStep.LINK -> CUSTOM
    }

    /** Banner / native AdListener: failed or no_fill, impression, click. Overrides must call super. */
    open class PackListener(private val format: String, var place: String) : AdListener() {
        override fun onAdFailedToLoad(error: LoadAdError) = fetchFailed(format, error.code, place)
        override fun onAdImpression() = send(format, "impression", place)
        override fun onAdClicked() = send(format, "click", place)
    }

    /** Full-screen callback: show, show_failed, impression, click, close. Overrides must call super. */
    open class WholeScreen(private val format: String, private val place: String) : FullScreenContentCallback() {
        override fun onAdShowedFullScreenContent() = send(format, "show", place)
        override fun onAdFailedToShowFullScreenContent(error: AdError) = send(format, "show_failed", place, error.code)
        override fun onAdImpression() = send(format, "impression", place)
        override fun onAdClicked() = send(format, "click", place)
        override fun onAdDismissedFullScreenContent() = send(format, "close", place)
    }
}
