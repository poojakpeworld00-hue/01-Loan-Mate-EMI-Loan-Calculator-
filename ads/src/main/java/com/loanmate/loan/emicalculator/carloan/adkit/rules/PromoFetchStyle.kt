package com.loanmate.loan.emicalculator.carloan.adkit.rules

import android.content.Context

/**
 * Preload or on-load, per ad format and, for interstitials, per place (port of the reference PromoFetchStyle).
 *
 * - Preload: an ad is loaded ahead and shown at once when its moment comes; with none ready the
 *   moment passes without an ad and the next one is loaded.
 * - On-load: nothing is loaded ahead; the moment shows a loader, loads the ad and shows it, and
 *   gives up after `ads.loader_wait_sec` (PromoRest).
 *
 * Remote Config: `ads.preload` is either one boolean (interstitials only, as before) or an object
 * `{ "inter", "native", "app_open", "rewarded" }`. A place's own `"preload"` overrides the
 * interstitial value for that place. Each format keeps today's behaviour when nothing is set:
 * interstitials on-load, everything else preloaded.
 */
object PromoFetchStyle {

    enum class PackShape(val key: String, val default: Boolean) {
        INTER("is_preload_ads", false),
        NATIVE("is_preload_native", true),
        APP_OPEN("is_preload_app_open", true),
        REWARDED("is_preload_rewarded", true),
    }

    /** Placement key holding a place's own interstitial mode. */
    const val PLACE_KEY = "preload"

    fun preload(context: Context, format: PackShape, placement: String? = null): Boolean {
        val placeOwn = if (format == PackShape.INTER && placement != null) PromoPieceKeys.own(context, placement, PLACE_KEY) else null
        val stored = PromoSettingsBox.getInstance(context).getBoolean(format.key, format.default)
        return kitDecide(format, placeOwn, stored)
    }

    /** The rule, without Android: a place's own value (interstitials only), else the stored switch, else the default. */
    fun kitDecide(format: PackShape, placeOwn: String?, stored: Boolean?): Boolean {
        if (format == PackShape.INTER) placeOwn?.trim()?.lowercase()?.toBooleanStrictOrNull()?.let { return it }
        return stored ?: format.default
    }

    /** Places whose own `preload` is true: their interstitial is warmed ahead even in on-load mode. */
    fun kitInterPreloadPlaces(context: Context): List<String> =
        PromoPieceKeys.kitPlacementNames(context).filter { place ->
            PromoPieceKeys.own(context, place, PLACE_KEY)?.trim()?.lowercase() == "true"
        }
}
