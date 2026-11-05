package com.loanmate.loan.emicalculator.carloan.adkit.rules

import android.app.Activity
import android.content.Context
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.gms.ads.AdSize
import org.json.JSONObject
import com.loanmate.loan.emicalculator.carloan.adkit.BuildConfig
import com.loanmate.loan.emicalculator.carloan.adkit.draw.StripRenderer
import com.loanmate.loan.emicalculator.carloan.adkit.draw.StripDims
import com.loanmate.loan.emicalculator.carloan.adkit.draw.StripLifeTap
import com.loanmate.loan.emicalculator.carloan.adkit.draw.StripLook
import com.loanmate.loan.emicalculator.carloan.adkit.draw.InlinePromoRenderer
import com.loanmate.loan.emicalculator.carloan.adkit.draw.InlineBarRenderer

object ScreenPromoMapper {
    private const val TAG = "ScreenAdPlanner"

    data class ScreenPromoPlan(
        val show: Boolean,
        val type: String,
        val bannerId: String,
        val bannerType: String,
        val nativeId: String,
        val nativeType: String
    ) {
        val isNative: Boolean get() = type.equals("native", ignoreCase = true)
    }



    fun embeddedUnitId(context: Context): String {
        val pref = PromoSettingsBox.getInstance(context)
        val globalNative = pref.getString("googleNative").orEmpty()
        val screenWise = pref.getBoolean("screen_wise_ad")

        val result: String
        val source: String
        if (!screenWise) {
            result = globalNative
            source = "googleNative (screen_wise_ad=false)"
        } else {
            val root = runCatching {
                JSONObject(pref.getString("ScreenAds", "{}") ?: "{}")
            }.getOrNull()
            val entryNative = root?.optJSONObject("default")?.optString("native").orEmpty()
            if (entryNative.isNotBlank()) {
                result = entryNative
                source = "ScreenAds.default.native"
            } else {
                result = globalNative
                source = "googleNative (default.native blank/missing)"
            }
        }
        if (BuildConfig.DEBUG) Log.d(TAG, "nativeUnitId() = " + result + "   <- " + source)
        return result
    }




}
