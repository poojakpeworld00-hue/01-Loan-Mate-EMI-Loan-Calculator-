package com.fffskintools.skin.tools.ads.policy

import android.content.Context
import org.json.JSONObject

object SpotUnitIds {
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

    private fun kitPlacementsBlock(pref: SpotOptionsChest): JSONObject? {
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
        kitPlacementsBlock(SpotOptionsChest.getInstance(context))?.keys()?.asSequence()?.toList().orEmpty()

    private fun nested(pref: SpotOptionsChest, name: String, key: String): String? =
        kitPlacementsBlock(pref)?.optJSONObject(name)
            ?.takeIf { it.has(key) && !it.isNull(key) }
            ?.get(key)?.toString()?.takeIf { it.isNotBlank() }

    fun own(pref: SpotOptionsChest, placement: String?, key: String): String? =
        names(placement).firstNotNullOfOrNull { name ->
            nested(pref, name, key)
                ?: runCatching { pref.getString("${name}_$key") }.getOrNull()?.takeIf { it.isNotBlank() }
        }

    fun own(context: Context, placement: String?, key: String): String? =
        own(SpotOptionsChest.getInstance(context), placement, key)

    fun resolve(context: Context, placement: String?, globalKey: String): String =
        resolve(SpotOptionsChest.getInstance(context), placement, globalKey)

    fun resolve(pref: SpotOptionsChest, placement: String?, globalKey: String): String =
        own(pref, placement, globalKey) ?: pref.getString(globalKey).orEmpty()

    fun pickFullEmbedded(context: Context, placement: String?): String {
        val pref = SpotOptionsChest.getInstance(context)
        return own(pref, placement, "googleFullNative") ?: pref.getString("googleNative").orEmpty()
    }

    fun kitSlotEnabled(context: Context, placement: String?): Boolean {
        val pref = SpotOptionsChest.getInstance(context)
        for (name in names(placement)) {
            nested(pref, name, "ads_on")?.let { return it.trim().lowercase() != "false" }
            if (runCatching { pref.getBoolean("${name}_ads_on", true) }.getOrDefault(true).not()) return false
        }
        return true
    }
}
