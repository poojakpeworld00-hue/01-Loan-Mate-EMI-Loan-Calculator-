package com.loanmate.loan.emicalculator.carloan.homelloan.logic.repos

import androidx.annotation.DrawableRes

interface DocsStore {
    /** Saves the image to the gallery (Pictures/LoanMate) as "<title>.png"; false if it failed. */
    fun saveToGallery(@DrawableRes image: Int, title: String): Boolean
}
