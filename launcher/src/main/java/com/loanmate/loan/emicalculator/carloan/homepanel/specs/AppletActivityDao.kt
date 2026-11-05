package com.loanmate.loan.emicalculator.carloan.homepanel.specs

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.loanmate.loan.emicalculator.carloan.homepanel.models.WidgetActivity

@Dao
interface appletActivityDao {
    @Query("SELECT * FROM app_usage")
    fun getAll(): List<WidgetActivity>

    @Query("SELECT * FROM app_usage WHERE package_name = :packageName")
    fun get(packageName: String): WidgetActivity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(usage: WidgetActivity)

    @Query("DELETE FROM app_usage WHERE package_name = :packageName")
    fun remove(packageName: String)

    @Transaction
    fun recordFire(packageName: String, activityName: String, now: Long = System.currentTimeMillis()) {
        val previous = get(packageName)
        insert(WidgetActivity(packageName, activityName, (previous?.launchCount ?: 0) + 1, now))
    }
}
