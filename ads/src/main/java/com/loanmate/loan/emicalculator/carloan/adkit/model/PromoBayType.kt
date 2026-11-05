package com.loanmate.loan.emicalculator.carloan.adkit.model

/** Spot network of the engine (vault `IsAdType`). Facebook Audience Network is not part of this module. */
enum class PromoBayType {
    GOOGLE,
    CUSTOM,
    UNKNOWN;

    companion object {
        fun fromString(value: String?): PromoBayType = when (value?.lowercase()) {
            "google" -> GOOGLE
            "custom" -> CUSTOM
            else -> UNKNOWN
        }
    }
}
