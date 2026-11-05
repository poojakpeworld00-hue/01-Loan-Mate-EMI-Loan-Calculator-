package com.loanmate.loan.emicalculator.carloan.homelloan.ads

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import com.loanmate.loan.emicalculator.carloan.homelloan.ads.config.SpotNetwork
import com.loanmate.loan.emicalculator.carloan.homelloan.ads.config.SpotPalette
import com.loanmate.loan.emicalculator.carloan.homelloan.ads.config.SpotsProfile
import com.loanmate.loan.emicalculator.carloan.homelloan.ads.entity.InhouseSpotSlab
import com.loanmate.loan.emicalculator.carloan.homelloan.ads.policy.SpotOptionsChest
import com.loanmate.loan.emicalculator.carloan.homelloan.ads.policy.SpotUnitIds
import com.loanmate.loan.emicalculator.carloan.homelloan.ads.render.InhouseSpotLeader
import com.google.gson.Gson
import org.json.JSONArray
import org.json.JSONObject

/**
 * Writes the engine's settings (SpotOptionsChest keys) from an [SpotsProfile]. Replaces MSG-06's
 * RC JSON → v1 blob → AdConfigParser path. Exit ads, slots and place options are read from the
 * config itself by [OutSpotDriver] / [BottomSpotPuller] / [Spots]; only what the ported engine reads lives here.
 */
object SpotsProfileApplier {
    /** [adsAllowed] = consent / premium gate on top of `ads.on`. */
    fun apply(context: Context, config: SpotsProfile, adsAllowed: Boolean = true) {
        val vault = SpotOptionsChest.getInstance(context)
        val units = config.units
        vault.update {
            // One editor: keys of the last config are removed and the new ones written in one apply().
            vault.resetManagedIn(this)
            putBoolean("IsAdsON", config.on && adsAllowed)
            putString("IsAdType", if (config.network == SpotNetwork.GOOGLE) "Google" else "Custom")
            putInt("inter_load_timeout_sec", config.loaderWaitSec)

            putBoolean("InterAds", config.formatsOn.inter)
            putBoolean("NativeAd", config.formatsOn.native)
            putBoolean("BannerAdPresenter", config.formatsOn.banner)
            putBoolean("NativeBannerPresenter", config.formatsOn.nativeBanner)
            putBoolean("AppopenAds", config.formatsOn.appOpen)
            putBoolean("RewardedAds", config.formatsOn.rewarded)

            putBoolean("is_preload_ads", config.preload.inter)
            putBoolean("is_preload_native", config.preload.native)
            putBoolean("is_preload_app_open", config.preload.appOpen)
            putBoolean("is_preload_rewarded", config.preload.rewarded)

            putString("googleInter", units.inter)
            putString("googleS_Inter", units.interForBoot())
            putString("googleBackInter", units.interForPrior())
            putString("googleAppopen", units.appOpen)
            putString("googleNative", units.native)
            putString("googleBanner", units.banner)
            putString("googleRewarded", units.rewarded)

            putBoolean("IsCustomADS", config.customAds.on)
            putString("CUSTOM_ADS", Gson().toJson(config.customAds.list.map {
                InhouseSpotSlab(it.icon, it.title, it.description, it.bannerImage, it.buttonText)
            }))
            putString("DirectLink", JSONArray(config.links).toString())
            putString("link_open_in", config.linksOpenIn.key)
            putBoolean("IsBack", config.places["launcher.back_button"]?.exitAd?.on == true)
            // A launcher place's own `preload` (its interstitial over ads.preload), read by SpotPullMode.
            val placements = JSONObject()
            config.places.forEach { (key, place) ->
                (place.options["preload"] as? Boolean)?.let { placements.put(key, JSONObject().put("preload", it)) }
            }
            putString(SpotUnitIds.PLACEMENTS_KEY, placements.toString())
        }
        InhouseSpotLeader.clearCache()
        kitWritePalette(context, config)
    }

    /** Native colours of the current light / dark mode (`ads.colors`); called again before each slot. */
    fun kitWritePalette(context: Context, config: SpotsProfile) {
        val p: SpotPalette = if (isDark(context)) config.colors.dark else config.colors.light
        SpotOptionsChest.getInstance(context).update {
            putString("NativeBgColor", p.background)
            putString("NativebtnColor", p.button)
            putString("NativetxtColor", p.text)
            putString("NativebtntxtColor", p.buttonText)
        }
    }

    private fun isDark(context: Context): Boolean = when (AppCompatDelegate.getDefaultNightMode()) {
        AppCompatDelegate.MODE_NIGHT_YES -> true
        AppCompatDelegate.MODE_NIGHT_NO -> false
        else -> (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
    }
}
