package com.loanmate.loan.emicalculator.carloan.homelloan.logic.repos

import com.loanmate.loan.emicalculator.carloan.homelloan.logic.entity.TourTwo
import com.loanmate.loan.emicalculator.carloan.homelloan.logic.entity.ShelfOrder
import com.loanmate.loan.emicalculator.carloan.homelloan.logic.entity.PackPart

interface ShelfStore {
    fun section(type: ShelfOrder): PackPart
    fun tips(): List<TourTwo>
    fun diamondGuides(): List<TourTwo>
}
