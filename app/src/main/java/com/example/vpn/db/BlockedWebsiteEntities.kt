package com.example.vpn.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "blocked_domains")
data class BlockedDomainEntity(
    @PrimaryKey val domain: String,
    val category: String,
    val isCustom: Boolean = false,
    val isEnabled: Boolean = true,
    val isFavorite: Boolean = false,
    val isPermanent: Boolean = false,
    val addedAt: Long = System.currentTimeMillis(),
    /** 0 means no expiry; otherwise this is an absolute epoch-millis deadline. */
    @ColumnInfo(defaultValue = "0") val expiresAtMillis: Long = 0L,
    val previousExpiryMillis: Long? = null
)

@Entity(tableName = "website_categories")
data class CategoryEntity(
    @PrimaryKey val name: String,
    val description: String,
    val iconName: String,
    val colorHex: Long,
    val isEnabled: Boolean = true,
    val isCustom: Boolean = false,
    val domainCount: Int = 0
)

@Entity(tableName = "blocking_schedules")
data class BlockingScheduleEntity(
    @PrimaryKey val id: String,
    val title: String,
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int,
    val activeDaysCsv: String,
    val blockedCategoriesCsv: String,
    val customDomainsCsv: String = "",
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    /** Inclusive local dates in yyyy-MM-dd format; null means no date boundary. */
    val startDateIso: String? = null,
    val endDateIso: String? = null
)

@Entity(tableName = "blocked_events")
data class BlockedWebsiteEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val domain: String,
    val category: String,
    val timestamp: Long = System.currentTimeMillis(),
    val sessionId: String? = null,
    val ruleId: String? = null,
    val blockingReason: String = "Rule matched",
    val requestType: String = "A"
)
