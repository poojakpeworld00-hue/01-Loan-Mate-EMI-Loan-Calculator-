package com.loanmate.loan.emicalculator.carloan.homelloan.launcher.contracts

import androidx.room.*
import com.loanmate.loan.emicalculator.carloan.homelloan.launcher.entities.HiddenSymbol

@Dao
interface hiddenGlyphsDao {
    @Query("SELECT * FROM hidden_icons")
    fun pullHiddenGlyphs(): List<HiddenSymbol>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(hiddenIconUi: HiddenSymbol): Long

    @Delete
    fun dropHiddenGlyphs(icons: List<HiddenSymbol>)
}
