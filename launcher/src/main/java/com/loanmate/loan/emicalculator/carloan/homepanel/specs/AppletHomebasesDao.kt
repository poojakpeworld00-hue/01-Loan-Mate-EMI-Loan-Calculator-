package com.loanmate.loan.emicalculator.carloan.homepanel.specs

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.loanmate.loan.emicalculator.carloan.homepanel.models.WidgetCenter

@Dao
interface appletHomebasesDao {
    @Query("SELECT * FROM apps")
    fun pullAppletHomebases(): List<WidgetCenter>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertEvery(appLaunchers: List<WidgetCenter>)

    @Query("DELETE FROM apps WHERE package_name = :packageName")
    fun eraseApplet(packageName: String)

    @Query("DELETE FROM apps WHERE id = :id")
    fun eraseByKey(id: Long)
}
