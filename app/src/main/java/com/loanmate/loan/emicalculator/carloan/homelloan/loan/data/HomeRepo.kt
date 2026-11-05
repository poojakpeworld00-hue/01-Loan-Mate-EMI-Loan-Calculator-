package com.loanmate.loan.emicalculator.carloan.homelloan.loan.data

import com.loanmate.loan.emicalculator.carloan.homelloan.R
import com.loanmate.loan.emicalculator.carloan.homelloan.loan.data.BankCard
import com.loanmate.loan.emicalculator.carloan.homelloan.loan.data.CalcCard

class HomeRepo {

    private val calcTiles = listOf(
        CalcCard("emi", R.string.ln_home_calc_emi, R.string.ln_home_calc_emi_subtitle, R.drawable.ln_home_loan_emi_cal),
        CalcCard("business", R.string.ln_home_calc_business, R.string.ln_home_calc_business_subtitle, R.drawable.ln_home_businessloan_cal),
        CalcCard("auto", R.string.ln_home_calc_auto, R.string.ln_home_calc_auto_subtitle, R.drawable.ln_home_auto_loan_cal),
        CalcCard("home_loan", R.string.ln_home_calc_home_loan, R.string.ln_home_calc_home_loan_subtitle, R.drawable.ln_home_homeloan_cal),
    )

    private val bankingTiles = listOf(
        BankCard("rd", R.string.ln_home_grid_rd, R.drawable.ln_home_rd_cal, R.drawable.ln_bg_grid_purple, R.color.ln_grid_purple_fg),
        BankCard("fd", R.string.ln_home_grid_fd, R.drawable.ln_home_fd_cal, R.drawable.ln_bg_grid_dark, R.color.ln_grid_dark_fg),
        BankCard("roi", R.string.ln_home_grid_roi, R.drawable.ln_home_roi_cal, R.drawable.ln_bg_grid_pink, R.color.ln_grid_pink_fg),
        BankCard("ppf", R.string.ln_home_grid_ppf, R.drawable.ln_home_ppf_cal, R.drawable.ln_bg_grid_blue, R.color.ln_grid_blue_fg),
        BankCard("compound", R.string.ln_home_grid_compound, R.drawable.ln_home_compound_cal, R.drawable.ln_bg_grid_yellow, R.color.ln_grid_yellow_fg),
        BankCard("cash", R.string.ln_home_grid_cash, R.drawable.ln_home_cash_cal, R.drawable.ln_bg_grid_green, R.color.ln_grid_green_fg),
        BankCard("quiz", R.string.ln_home_grid_quiz, R.drawable.ln_home_calc_quiz, R.drawable.ln_bg_grid_red, R.color.ln_grid_red_fg),
        BankCard("play", R.string.ln_home_grid_play, R.drawable.ln_home_quiz, R.drawable.ln_bg_grid_amber, R.color.ln_grid_amber_fg),
    )

    fun getCalcTiles(): List<CalcCard> = calcTiles
    fun getBankingTiles(): List<BankCard> = bankingTiles
}
