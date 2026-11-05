package com.loanmate.loan.emicalculator.carloan.homepanel.specs

import androidx.room.*
import com.loanmate.loan.emicalculator.carloan.homepanel.models.MaskedIcon

@Dao
interface hiddenGlyphsDao {
    @Query("SELECT * FROM hidden_icons")
    fun pullMaskedGlyphs(): List<MaskedIcon>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(hiddenIconUi: MaskedIcon): Long

    @Delete
    fun dropMaskedGlyphs(icons: List<MaskedIcon>)
}
