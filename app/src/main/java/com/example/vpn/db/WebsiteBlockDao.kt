package com.example.vpn.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockedWebsiteDao {

    @Query("SELECT * FROM blocked_domains ORDER BY isFavorite DESC, domain ASC")
    fun getAllBlockedDomains(): Flow<List<BlockedDomainEntity>>

    @Query("SELECT * FROM blocked_domains WHERE isEnabled = 1")
    fun getActiveBlockedDomains(): Flow<List<BlockedDomainEntity>>

    @Query("SELECT * FROM blocked_domains WHERE isEnabled = 1")
    suspend fun getActiveBlockedDomainsSync(): List<BlockedDomainEntity>

    @Query("SELECT * FROM blocked_domains WHERE category = :category ORDER BY domain ASC")
    fun getDomainsByCategory(category: String): Flow<List<BlockedDomainEntity>>

    @Query("SELECT * FROM blocked_domains WHERE domain = :domain LIMIT 1")
    suspend fun getDomainByName(domain: String): BlockedDomainEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDomain(domain: BlockedDomainEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDomains(domains: List<BlockedDomainEntity>)

    @Query("DELETE FROM blocked_domains WHERE domain = :domain")
    suspend fun deleteDomain(domain: String)

    @Query("UPDATE blocked_domains SET isEnabled = :isEnabled, expiresAtMillis = CASE WHEN :isEnabled = 1 AND expiresAtMillis > 0 AND expiresAtMillis <= :now THEN :renewedUntil ELSE expiresAtMillis END WHERE domain = :domain")
    suspend fun updateDomainEnabled(domain: String, isEnabled: Boolean, now: Long, renewedUntil: Long)

    @Query("UPDATE blocked_domains SET isFavorite = :isFavorite WHERE domain = :domain")
    suspend fun updateDomainFavorite(domain: String, isFavorite: Boolean)

    @Query("UPDATE blocked_domains SET isPermanent = 1, previousExpiryMillis = expiresAtMillis WHERE domain = :domain AND isPermanent = 0")
    suspend fun markDomainPermanent(domain: String)

    @Query("UPDATE blocked_domains SET isPermanent = 0, expiresAtMillis = CASE WHEN previousExpiryMillis IS NOT NULL THEN previousExpiryMillis WHEN isCustom = 1 AND expiresAtMillis = 0 THEN :renewedUntil ELSE expiresAtMillis END, previousExpiryMillis = NULL WHERE domain = :domain")
    suspend fun clearDomainPermanent(domain: String, renewedUntil: Long)

    @Query("SELECT COUNT(*) FROM blocked_domains")
    suspend fun getDomainCount(): Int
}

@Dao
interface WebsiteCategoryDao {

    @Query("SELECT * FROM website_categories ORDER BY isCustom ASC, name ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM website_categories WHERE isEnabled = 1")
    fun getActiveCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM website_categories WHERE isEnabled = 1")
    suspend fun getActiveCategoriesSync(): List<CategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Query("UPDATE website_categories SET isEnabled = :isEnabled WHERE name = :name")
    suspend fun updateCategoryEnabled(name: String, isEnabled: Boolean)

    @Query("UPDATE blocked_domains SET category = 'Custom Websites' WHERE category = :name")
    suspend fun moveDomainsToDefaultCategory(name: String)

    @Query("DELETE FROM website_categories WHERE name = :name AND isCustom = 1")
    suspend fun deleteCustomCategory(name: String)

    @Query("SELECT * FROM website_categories WHERE name = :name LIMIT 1")
    suspend fun getCategoryByName(name: String): CategoryEntity?

    @Query("SELECT COUNT(*) FROM website_categories")
    suspend fun getCategoryCount(): Int
}

@Dao
interface BlockingScheduleDao {

    @Query("SELECT * FROM blocking_schedules ORDER BY startHour ASC, startMinute ASC")
    fun getAllSchedules(): Flow<List<BlockingScheduleEntity>>

    @Query("SELECT * FROM blocking_schedules WHERE isEnabled = 1")
    fun getActiveSchedules(): Flow<List<BlockingScheduleEntity>>

    @Query("SELECT * FROM blocking_schedules WHERE isEnabled = 1")
    suspend fun getActiveSchedulesSync(): List<BlockingScheduleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(schedule: BlockingScheduleEntity)

    @Query("DELETE FROM blocking_schedules WHERE id = :id")
    suspend fun deleteSchedule(id: String)

    @Query("UPDATE blocking_schedules SET isEnabled = :isEnabled WHERE id = :id")
    suspend fun updateScheduleEnabled(id: String, isEnabled: Boolean)
}

data class DomainAttemptCount(
    val domain: String,
    val count: Int,
    val category: String
)

data class CategoryAttemptCount(
    val category: String,
    val count: Int
)

data class DailyAttemptCount(
    val dayTimestamp: Long,
    val count: Int
)

@Dao
interface BlockedEventDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: BlockedWebsiteEventEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<BlockedWebsiteEventEntity>)

    @Query("SELECT * FROM blocked_events ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentEvents(limit: Int = 100): Flow<List<BlockedWebsiteEventEntity>>

    @Query("SELECT COUNT(*) FROM blocked_events")
    fun getTotalBlockedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM blocked_events")
    suspend fun getTotalBlockedCountSync(): Int

    @Query("SELECT COUNT(*) FROM blocked_events WHERE timestamp >= :sinceTimestamp")
    fun getBlockedCountSince(sinceTimestamp: Long): Flow<Int>

    @Query("SELECT COUNT(DISTINCT domain) FROM blocked_events")
    fun getUniqueBlockedDomainsCount(): Flow<Int>

    @Query("SELECT domain, COUNT(*) as count, category FROM blocked_events GROUP BY domain ORDER BY count DESC LIMIT :limit")
    fun getTopBlockedDomains(limit: Int = 10): Flow<List<DomainAttemptCount>>

    @Query("SELECT category, COUNT(*) as count FROM blocked_events GROUP BY category ORDER BY count DESC")
    fun getCategoryDistribution(): Flow<List<CategoryAttemptCount>>

    @Query("DELETE FROM blocked_events")
    suspend fun clearAllEvents()
}
