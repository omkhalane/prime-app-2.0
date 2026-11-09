package com.example.vpn.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "blocked_domains")
data class BlockedDomainEntity(
    @PrimaryKey
    val domain: String,
    val category: String,
    val isCustom: Boolean = false,
    val isEnabled: Boolean = true,
    val isFavorite: Boolean = false,
    val isPermanent: Boolean = false,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "website_categories")
data class CategoryEntity(
    @PrimaryKey
    val name: String,
    val description: String,
    val iconName: String,
    val colorHex: Long,
    val isEnabled: Boolean = true,
    val isCustom: Boolean = false,
    val domainCount: Int = 0
)

@Entity(tableName = "blocking_schedules")
data class BlockingScheduleEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int,
    val activeDaysCsv: String, // e.g. "0,1,2,3,4" (0=Sun..6=Sat)
    val blockedCategoriesCsv: String, // e.g. "Social Media,Gaming"
    val customDomainsCsv: String = "",
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "blocked_events")
data class BlockedWebsiteEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val domain: String,
    val category: String,
    val timestamp: Long = System.currentTimeMillis(),
    val sessionId: String? = null,
    val ruleId: String? = null,
    val blockingReason: String = "Rule matched",
    val requestType: String = "A"
)
