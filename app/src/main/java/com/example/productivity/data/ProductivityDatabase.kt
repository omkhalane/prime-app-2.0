package com.example.productivity.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ProductivityTaskEntity::class,
        ProductivityProjectEntity::class,
        ProductivityHabitEntity::class,
        ProductivityHabitCheckInEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class ProductivityDatabase : RoomDatabase() {
    abstract fun productivityDao(): ProductivityDao

    companion object {
        @Volatile private var instance: ProductivityDatabase? = null

        fun get(context: Context): ProductivityDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    ProductivityDatabase::class.java,
                    "prime_productivity.db"
                ).build().also { instance = it }
            }
    }
}
