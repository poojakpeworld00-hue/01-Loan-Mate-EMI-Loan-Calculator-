package com.loanmate.loan.emicalculator.carloan.homepanel.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_usage")
data class WidgetActivity(
    @PrimaryKey @ColumnInfo(name = "package_name") val packageName: String,
    @ColumnInfo(name = "activity_name") val activityName: String,
    @ColumnInfo(name = "launch_count") val launchCount: Int,
    @ColumnInfo(name = "last_launched") val lastLaunched: Long,
)
