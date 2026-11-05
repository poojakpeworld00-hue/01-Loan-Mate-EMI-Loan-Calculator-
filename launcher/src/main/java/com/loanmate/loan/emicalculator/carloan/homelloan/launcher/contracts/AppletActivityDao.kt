package com.loanmate.loan.emicalculator.carloan.homelloan.launcher.contracts

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.loanmate.loan.emicalculator.carloan.homelloan.launcher.entities.GadgetActivity

@Dao
interface appletActivityDao {
    @Query("SELECT * FROM app_usage")
    fun getAll(): List<GadgetActivity>

    @Query("SELECT * FROM app_usage WHERE package_name = :packageName")
    fun get(packageName: String): GadgetActivity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(usage: GadgetActivity)

    @Query("DELETE FROM app_usage WHERE package_name = :packageName")
    fun remove(packageName: String)

    @Transaction
    fun recordFire(packageName: String, activityName: String, now: Long = System.currentTimeMillis()) {
        val previous = get(packageName)
        insert(GadgetActivity(packageName, activityName, (previous?.launchCount ?: 0) + 1, now))
    }
}
