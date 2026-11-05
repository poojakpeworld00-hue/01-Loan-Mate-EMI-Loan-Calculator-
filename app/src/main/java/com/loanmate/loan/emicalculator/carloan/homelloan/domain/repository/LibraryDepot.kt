package com.loanmate.loan.emicalculator.carloan.homelloan.domain.repository

import com.loanmate.loan.emicalculator.carloan.homelloan.domain.model.Guide2
import com.loanmate.loan.emicalculator.carloan.homelloan.domain.model.LibrarySort
import com.loanmate.loan.emicalculator.carloan.homelloan.domain.model.KitSection

interface LibraryDepot {
    fun section(type: LibrarySort): KitSection
    fun tips(): List<Guide2>
    fun diamondGuides(): List<Guide2>
}
