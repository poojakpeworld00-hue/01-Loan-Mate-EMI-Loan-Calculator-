package com.loanmate.loan.emicalculator.carloan.homelloan.launcher.storage

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.loanmate.loan.emicalculator.carloan.homelloan.launcher.support.Transformers
import com.loanmate.loan.emicalculator.carloan.homelloan.launcher.contracts.appletHomebasesDao
import com.loanmate.loan.emicalculator.carloan.homelloan.launcher.contracts.appletActivityDao
import com.loanmate.loan.emicalculator.carloan.homelloan.launcher.contracts.hiddenGlyphsDao
import com.loanmate.loan.emicalculator.carloan.homelloan.launcher.contracts.atriumViewerGridRecordsDao
import com.loanmate.loan.emicalculator.carloan.homelloan.launcher.entities.GadgetHub
import com.loanmate.loan.emicalculator.carloan.homelloan.launcher.entities.GadgetActivity
import com.loanmate.loan.emicalculator.carloan.homelloan.launcher.entities.HiddenSymbol
import com.loanmate.loan.emicalculator.carloan.homelloan.launcher.entities.LobbyLookerMatrixDatum

@Database(
    entities = [GadgetHub::class, LobbyLookerMatrixDatum::class, HiddenSymbol::class, GadgetActivity::class],
    version = 6
)
@TypeConverters(Transformers::class)
abstract class GadgetsDb : RoomDatabase() {
    abstract fun appletHomebasesDao(): appletHomebasesDao

    abstract fun atriumViewerGridRecordsDao(): atriumViewerGridRecordsDao

    abstract fun hiddenGlyphsDao(): hiddenGlyphsDao

    abstract fun appletActivityDao(): appletActivityDao

    companion object {
        private var db: GadgetsDb? = null

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `app_usage` (`package_name` TEXT NOT NULL, `activity_name` TEXT NOT NULL, " +
                        "`launch_count` INTEGER NOT NULL, `last_launched` INTEGER NOT NULL, PRIMARY KEY(`package_name`))"
                )
            }
        }

        fun getInstance(context: Context): GadgetsDb {
            if (db == null) {
                synchronized(GadgetsDb::class) {
                    if (db == null) {
                        db = Room.databaseBuilder(
                            context.applicationContext,
                            GadgetsDb::class.java,
                            "lnch_apps.db" // own name: the host app shares the databases dir
                        ).addMigrations(MIGRATION_5_6).build()
                    }
                }
            }
            return db!!
        }
    }
}
