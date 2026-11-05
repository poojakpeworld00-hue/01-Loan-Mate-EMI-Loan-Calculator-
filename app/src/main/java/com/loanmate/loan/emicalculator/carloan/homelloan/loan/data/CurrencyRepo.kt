package com.loanmate.loan.emicalculator.carloan.homelloan.loan.data

import com.loanmate.loan.emicalculator.carloan.homelloan.R
import com.loanmate.loan.emicalculator.carloan.homelloan.loan.data.CurrencyEntry
import com.loanmate.loan.emicalculator.carloan.homelloan.loan.data.LoanPrefs

class CurrencyRepo(private val pref: LoanPrefs) {

    private val currencies = listOf(
        CurrencyEntry("USD", R.string.ln_currency_usd, R.string.ln_currency_symbol_usd),
        CurrencyEntry("THB", R.string.ln_currency_thb, R.string.ln_currency_symbol_thb),
        CurrencyEntry("INR", R.string.ln_currency_inr, R.string.ln_currency_symbol_inr),
        CurrencyEntry("EUR", R.string.ln_currency_eur, R.string.ln_currency_symbol_eur),
        CurrencyEntry("CNY", R.string.ln_currency_cny, R.string.ln_currency_symbol_cny),
        CurrencyEntry("VND", R.string.ln_currency_vnd, R.string.ln_currency_symbol_vnd),
        CurrencyEntry("GBP", R.string.ln_currency_gbp, R.string.ln_currency_symbol_gbp),
        CurrencyEntry("JPY", R.string.ln_currency_jpy, R.string.ln_currency_symbol_jpy),
        CurrencyEntry("CAD", R.string.ln_currency_cad, R.string.ln_currency_symbol_cad),
        CurrencyEntry("IDR", R.string.ln_currency_idr, R.string.ln_currency_symbol_idr),
        CurrencyEntry("AUD", R.string.ln_currency_aud, R.string.ln_currency_symbol_aud),
        CurrencyEntry("KWD", R.string.ln_currency_kwd, R.string.ln_currency_symbol_kwd),
        CurrencyEntry("BHD", R.string.ln_currency_bhd, R.string.ln_currency_symbol_bhd),
        CurrencyEntry("OMR", R.string.ln_currency_omr, R.string.ln_currency_symbol_omr),
        CurrencyEntry("JOD", R.string.ln_currency_jod, R.string.ln_currency_symbol_jod),
        CurrencyEntry("CHF", R.string.ln_currency_chf, R.string.ln_currency_symbol_chf),
        CurrencyEntry("AED", R.string.ln_currency_aed, R.string.ln_currency_symbol_aed),
        CurrencyEntry("SAR", R.string.ln_currency_sar, R.string.ln_currency_symbol_sar),
    )

    fun getAll(): List<CurrencyEntry> = currencies

    fun getSavedCode(): String = pref.selectedCurrency.ifEmpty { "USD" }

    fun save(code: String) {
        pref.setCurrencyCode(code)
    }
}
