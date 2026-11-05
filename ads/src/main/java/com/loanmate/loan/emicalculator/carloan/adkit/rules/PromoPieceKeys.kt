package com.loanmate.loan.emicalculator.carloan.adkit.rules

import android.content.Context
import org.json.JSONObject

object PromoPieceKeys {
    const val PLACEMENTS_KEY = "placements"

    private var placementsRaw: String? = null
    private var placementsJson: JSONObject? = null

    private fun normalize(placement: String?): String? = when (placement) {
        "leftSwipe", "leftPanel" -> "leftPanel"
        "rightSwipe", "rightPanel", "panelMid", "callerPanel" -> "rightPanel"
        "appLaunch", "drawerOpen", "appDrawer", "drawer" -> "drawer"
        else -> placement
    }

    private fun names(placement: String?): List<String> {
        val name = normalize(placement)?.takeIf { it.isNotBlank() } ?: return emptyList()
        return if (name.startsWith("onboarding_")) listOf(name, "onboarding") else listOf(name)
    }

    private fun kitPlacementsBlock(pref: PromoSettingsBox): JSONObject? {
        val raw = runCatching { pref.getString(PLACEMENTS_KEY) }.getOrNull().orEmpty()
        synchronized(this) {
            if (raw != placementsRaw) {
                placementsRaw = raw
                placementsJson = raw.takeIf { it.isNotBlank() }
                    ?.let { runCatching { JSONObject(it) }.getOrNull() }
            }
            return placementsJson
        }
    }

    /** Every place that has a block of its own in `placements`. */
    fun kitPlacementNames(context: Context): List<String> =
        kitPlacementsBlock(PromoSettingsBox.getInstance(context))?.keys()?.asSequence()?.toList().orEmpty()

    private fun nested(pref: PromoSettingsBox, name: String, key: String): String? =
        kitPlacementsBlock(pref)?.optJSONObject(name)
            ?.takeIf { it.has(key) && !it.isNull(key) }
            ?.get(key)?.toString()?.takeIf { it.isNotBlank() }

    fun own(pref: PromoSettingsBox, placement: String?, key: String): String? =
        names(placement).firstNotNullOfOrNull { name ->
            nested(pref, name, key)
                ?: runCatching { pref.getString("${name}_$key") }.getOrNull()?.takeIf { it.isNotBlank() }
        }

    fun own(context: Context, placement: String?, key: String): String? =
        own(PromoSettingsBox.getInstance(context), placement, key)

    fun resolve(context: Context, placement: String?, globalKey: String): String =
        resolve(PromoSettingsBox.getInstance(context), placement, globalKey)

    fun resolve(pref: PromoSettingsBox, placement: String?, globalKey: String): String =
        own(pref, placement, globalKey) ?: pref.getString(globalKey).orEmpty()

    fun pickFullEmbedded(context: Context, placement: String?): String {
        val pref = PromoSettingsBox.getInstance(context)
        return own(pref, placement, "googleFullNative") ?: pref.getString("googleNative").orEmpty()
    }

    fun kitSlotEnabled(context: Context, placement: String?): Boolean {
        val pref = PromoSettingsBox.getInstance(context)
        for (name in names(placement)) {
            nested(pref, name, "ads_on")?.let { return it.trim().lowercase() != "false" }
            if (runCatching { pref.getBoolean("${name}_ads_on", true) }.getOrDefault(true).not()) return false
        }
        return true
    }
}
