package com.fffskintools.skin.tools.ads.entity

/** Spot network of the engine (vault `IsAdType`). Facebook Audience Network is not part of this module. */
enum class SpotSlotKind {
    GOOGLE,
    CUSTOM,
    UNKNOWN;

    companion object {
        fun fromString(value: String?): SpotSlotKind = when (value?.lowercase()) {
            "google" -> GOOGLE
            "custom" -> CUSTOM
            else -> UNKNOWN
        }
    }
}
