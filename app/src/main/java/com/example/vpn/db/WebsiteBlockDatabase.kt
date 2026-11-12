package com.example.vpn.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        BlockedDomainEntity::class,
        CategoryEntity::class,
        BlockingScheduleEntity::class,
        BlockedWebsiteEventEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class WebsiteBlockDatabase : RoomDatabase() {
    abstract fun blockedWebsiteDao(): BlockedWebsiteDao
    abstract fun categoryDao(): WebsiteCategoryDao
    abstract fun scheduleDao(): BlockingScheduleDao
    abstract fun blockedEventDao(): BlockedEventDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE blocked_domains ADD COLUMN expiresAtMillis INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE blocking_schedules ADD COLUMN startDateIso TEXT")
                db.execSQL("ALTER TABLE blocking_schedules ADD COLUMN endDateIso TEXT")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE blocked_domains ADD COLUMN previousExpiryMillis INTEGER")
            }
        }

        @Volatile private var INSTANCE: WebsiteBlockDatabase? = null

        fun getInstance(context: Context): WebsiteBlockDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WebsiteBlockDatabase::class.java,
                    "prime_website_block.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
