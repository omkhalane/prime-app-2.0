package com.example.productivity.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "productivity_tasks", indices = [Index("dueAt"), Index("completedAt"), Index("projectId")])
data class ProductivityTaskEntity(
    @PrimaryKey val id: String,
    val title: String,
    val notes: String = "",
    val priority: Int = 2,
    val dueAt: Long? = null,
    val recurrence: String = "NONE",
    val projectId: String? = null,
    val completedAt: Long? = null,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(tableName = "productivity_projects", indices = [Index(value = ["name"], unique = true)])
data class ProductivityProjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val colorArgb: Long,
    val createdAt: Long
)

@Entity(tableName = "productivity_habits", indices = [Index("archived")])
data class ProductivityHabitEntity(
    @PrimaryKey val id: String,
    val title: String,
    /** Bit mask: bit 0 = Sunday ... bit 6 = Saturday. */
    val activeDaysMask: Int,
    val createdAt: Long,
    val archived: Boolean = false
)

@Entity(
    tableName = "productivity_habit_checkins",
    primaryKeys = ["habitId", "localDate"],
    indices = [Index("localDate")],
)
data class ProductivityHabitCheckInEntity(
    val habitId: String,
    /** ISO local date (yyyy-MM-dd), intentionally not a UTC instant. */
    val localDate: String,
    val completedAt: Long
)
