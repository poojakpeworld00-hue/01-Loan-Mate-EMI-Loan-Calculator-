package com.loanmate.loan.emicalculator.carloan.homelloan.loan.data

import android.content.Context
import android.content.SharedPreferences

class LoanPrefs(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    var selectedLanguage: String
        get() = prefs.getString(KEY_SELECTED_LANGUAGE, "") ?: ""
        set(value) = prefs.edit().putString(KEY_SELECTED_LANGUAGE, value).apply()

    var selectedTheme: String
        get() = prefs.getString(KEY_SELECTED_THEME, THEME_DARK) ?: THEME_DARK
        set(value) = prefs.edit().putString(KEY_SELECTED_THEME, value).apply()

    var isFirstLaunch: Boolean
        get() = prefs.getBoolean(KEY_FIRST_LAUNCH, true)
        set(value) = prefs.edit().putBoolean(KEY_FIRST_LAUNCH, value).apply()

    var termsAccepted: Boolean
        get() = prefs.getBoolean(KEY_TERMS_ACCEPTED, false)
        set(value) = prefs.edit().putBoolean(KEY_TERMS_ACCEPTED, value).apply()

    var languageSelected: Boolean
        get() = prefs.getBoolean(KEY_LANGUAGE_SELECTED, false)
        set(value) = prefs.edit().putBoolean(KEY_LANGUAGE_SELECTED, value).apply()

    var onboardingComplete: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_COMPLETE, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETE, value).apply()

    /** True once the user has passed through [PermissionActivity] (granted or skipped). */
    var permissionHandled: Boolean
        get() = prefs.getBoolean(KEY_PERMISSION_HANDLED, false)
        set(value) = prefs.edit().putBoolean(KEY_PERMISSION_HANDLED, value).apply()

    /** Flips to true after the user "watches" the reward ad gating the CIBIL guide. */
    var cibilGuideUnlocked: Boolean
        get() = prefs.getBoolean(KEY_CIBIL_GUIDE_UNLOCKED, false)
        set(value) = prefs.edit().putBoolean(KEY_CIBIL_GUIDE_UNLOCKED, value).apply()

    var selectedCurrency: String
        get() = prefs.getString(KEY_SELECTED_CURRENCY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_SELECTED_CURRENCY, value).apply()

    /** Sets the app currency (an ISO code from [CurrencyRepo], e.g. "INR"); unset means USD. */
    fun setCurrencyCode(code: String) {
        selectedCurrency = code
        currencySelected = true
    }

    var currencySelected: Boolean
        get() = prefs.getBoolean(KEY_CURRENCY_SELECTED, false)
        set(value) = prefs.edit().putBoolean(KEY_CURRENCY_SELECTED, value).apply()

    /** ATM locator — last-used country (ISO 3166-1 alpha-2). Empty = use default. */
    var atmCountry: String
        get() = prefs.getString(KEY_ATM_COUNTRY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_ATM_COUNTRY, value).apply()

    fun clearScoreHistory() {
        prefs.edit().remove(KEY_SCORE_HISTORY).apply()
    }

    fun putString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    fun getString(key: String, default: String = ""): String =
        prefs.getString(key, default) ?: default

    fun putBoolean(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
    }

    fun getBoolean(key: String, default: Boolean = false): Boolean =
        prefs.getBoolean(key, default)

    fun putInt(key: String, value: Int) {
        prefs.edit().putInt(key, value).apply()
    }

    fun getInt(key: String, default: Int = 0): Int =
        prefs.getInt(key, default)

    fun clear() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREF_NAME = "loan_mate_calc_prefs"

        private const val KEY_SELECTED_LANGUAGE = "selected_language"
        private const val KEY_SELECTED_THEME = "selected_theme"
        private const val KEY_FIRST_LAUNCH = "first_launch"
        private const val KEY_TERMS_ACCEPTED = "terms_accepted"
        private const val KEY_LANGUAGE_SELECTED = "language_selected"
        private const val KEY_ONBOARDING_COMPLETE = "onboarding_complete"
        private const val KEY_PERMISSION_HANDLED = "permission_handled"
        private const val KEY_CIBIL_GUIDE_UNLOCKED = "cibil_guide_unlocked"
        private const val KEY_SELECTED_CURRENCY = "selected_currency"
        private const val KEY_CURRENCY_SELECTED = "currency_selected"
        private const val KEY_SCORE_HISTORY = "score_history"
        private const val KEY_ATM_COUNTRY = "atm_country"
        private const val HISTORY_MAX = 50

        const val THEME_LIGHT = "light"
        const val THEME_DARK = "dark"
        const val THEME_SYSTEM = "system"
    }
}