package com.loanmate.loan.emicalculator.carloan.homepanel.vault

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.loanmate.loan.emicalculator.carloan.homepanel.aid.Transformers
import com.loanmate.loan.emicalculator.carloan.homepanel.specs.appletHomebasesDao
import com.loanmate.loan.emicalculator.carloan.homepanel.specs.appletActivityDao
import com.loanmate.loan.emicalculator.carloan.homepanel.specs.hiddenGlyphsDao
import com.loanmate.loan.emicalculator.carloan.homepanel.specs.atriumViewerGridRecordsDao
import com.loanmate.loan.emicalculator.carloan.homepanel.models.WidgetCenter
import com.loanmate.loan.emicalculator.carloan.homepanel.models.WidgetActivity
import com.loanmate.loan.emicalculator.carloan.homepanel.models.MaskedIcon
import com.loanmate.loan.emicalculator.carloan.homepanel.models.DeskViewerGridItem

@Database(
    entities = [WidgetCenter::class, DeskViewerGridItem::class, MaskedIcon::class, WidgetActivity::class],
    version = 6
)
@TypeConverters(Transformers::class)
abstract class WidgetsBase : RoomDatabase() {
    abstract fun appletHomebasesDao(): appletHomebasesDao

    abstract fun atriumViewerGridRecordsDao(): atriumViewerGridRecordsDao

    abstract fun hiddenGlyphsDao(): hiddenGlyphsDao

    abstract fun appletActivityDao(): appletActivityDao

    companion object {
        private var db: WidgetsBase? = null

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `app_usage` (`package_name` TEXT NOT NULL, `activity_name` TEXT NOT NULL, " +
                        "`launch_count` INTEGER NOT NULL, `last_launched` INTEGER NOT NULL, PRIMARY KEY(`package_name`))"
                )
            }
        }

        fun getInstance(context: Context): WidgetsBase {
            if (db == null) {
                synchronized(WidgetsBase::class) {
                    if (db == null) {
                        db = Room.databaseBuilder(
                            context.applicationContext,
                            WidgetsBase::class.java,
                            "lnch_apps.db" // own name: the host app shares the databases dir
                        ).addMigrations(MIGRATION_5_6).build()
                    }
                }
            }
            return db!!
        }
    }
}
