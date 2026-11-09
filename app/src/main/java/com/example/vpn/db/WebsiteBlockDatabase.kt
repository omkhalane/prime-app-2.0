package com.example.vpn.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        BlockedDomainEntity::class,
        CategoryEntity::class,
        BlockingScheduleEntity::class,
        BlockedWebsiteEventEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class WebsiteBlockDatabase : RoomDatabase() {

    abstract fun blockedWebsiteDao(): BlockedWebsiteDao
    abstract fun categoryDao(): WebsiteCategoryDao
    abstract fun scheduleDao(): BlockingScheduleDao
    abstract fun blockedEventDao(): BlockedEventDao

    companion object {
        @Volatile
        private var INSTANCE: WebsiteBlockDatabase? = null

        fun getInstance(context: Context): WebsiteBlockDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WebsiteBlockDatabase::class.java,
                    "prime_website_block.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
