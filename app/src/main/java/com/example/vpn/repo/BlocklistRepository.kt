package com.example.vpn.repo

import android.content.Context
import com.example.vpn.db.BlockedDomainEntity
import com.example.vpn.db.CategoryEntity
import com.example.vpn.db.WebsiteBlockDatabase
import com.example.vpn.dns.DomainMatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BlocklistRepository(
    private val context: Context,
    private val database: WebsiteBlockDatabase = WebsiteBlockDatabase.getInstance(context)
) {
    private val domainDao = database.blockedWebsiteDao()
    private val categoryDao = database.categoryDao()
    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        scope.launch {
            seedInitialDataIfEmpty()
        }
    }

    private suspend fun seedInitialDataIfEmpty() = withContext(Dispatchers.IO) {
        StarterWebsiteBlocklist.STARTER_CATEGORIES.forEach { category ->
            if (categoryDao.getCategoryByName(category.name) == null) categoryDao.insertCategory(category)
        }
        if (domainDao.getDomainCount() == 0) {
            domainDao.insertDomains(StarterWebsiteBlocklist.STARTER_DOMAINS)
        }
    }

    fun getAllDomains(): Flow<List<BlockedDomainEntity>> = domainDao.getAllBlockedDomains()

    fun getActiveBlockedDomains(): Flow<List<BlockedDomainEntity>> = domainDao.getActiveBlockedDomains()

    fun getDomainsByCategory(category: String): Flow<List<BlockedDomainEntity>> = domainDao.getDomainsByCategory(category)

    fun getAllCategories(): Flow<List<CategoryEntity>> = categoryDao.getAllCategories()

    fun getActiveCategories(): Flow<List<CategoryEntity>> = categoryDao.getActiveCategories()

    suspend fun addCustomDomain(
        rawDomain: String,
        category: String = "Custom Websites",
        isPermanent: Boolean = false,
        durationMillis: Long = 24L * 60L * 60L * 1000L
    ): Boolean = withContext(Dispatchers.IO) {
        val norm = DomainMatcher.normalize(rawDomain)
        if (!DomainMatcher.isValidDomain(rawDomain)) return@withContext false
        if (!isPermanent && durationMillis <= 0L) return@withContext false

        val entity = BlockedDomainEntity(
            domain = norm,
            category = category,
            isCustom = true,
            isEnabled = true,
            isFavorite = true,
            isPermanent = isPermanent,
            expiresAtMillis = if (isPermanent) 0L else System.currentTimeMillis() + durationMillis.coerceAtMost(365L * 24L * 60L * 60L * 1000L)
        )
        domainDao.insertDomain(entity)
        true
    }

    suspend fun deleteDomain(domain: String) = withContext(Dispatchers.IO) {
        domainDao.deleteDomain(DomainMatcher.normalize(domain))
    }

    suspend fun toggleDomainEnabled(domain: String, isEnabled: Boolean) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        domainDao.updateDomainEnabled(DomainMatcher.normalize(domain), isEnabled, now, now + 24L * 60L * 60L * 1000L)
    }

    suspend fun toggleDomainFavorite(domain: String, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        domainDao.updateDomainFavorite(DomainMatcher.normalize(domain), isFavorite)
    }

    suspend fun toggleDomainPermanent(domain: String, isPermanent: Boolean) = withContext(Dispatchers.IO) {
        val normalized = DomainMatcher.normalize(domain)
        if (isPermanent) {
            domainDao.markDomainPermanent(normalized)
        } else {
            val renewedUntil = System.currentTimeMillis() + 24L * 60L * 60L * 1000L
            domainDao.clearDomainPermanent(normalized, renewedUntil)
        }
    }

    suspend fun toggleCategoryEnabled(categoryName: String, isEnabled: Boolean) = withContext(Dispatchers.IO) {
        categoryDao.updateCategoryEnabled(categoryName, isEnabled)
    }

    suspend fun addCustomCategory(name: String, description: String, colorHex: Long) = withContext(Dispatchers.IO) {
        require(name.isNotBlank())
        require(!name.contains(',')) { "Category names cannot contain commas" }
        require(categoryDao.getCategoryByName(name.trim()) == null) { "A category with this name already exists" }
        val cat = CategoryEntity(
            name = name.trim(),
            description = description.trim(),
            iconName = "custom",
            colorHex = colorHex,
            isEnabled = true,
            isCustom = true,
            domainCount = 0
        )
        categoryDao.insertCategory(cat)
    }

    suspend fun deleteCustomCategory(name: String) = withContext(Dispatchers.IO) {
        categoryDao.moveDomainsToDefaultCategory(name)
        categoryDao.deleteCustomCategory(name)
    }

    /**
     * Resolves the current complete active blocking rules (domain -> category).
     * Includes:
     * 1. Enabled domains belonging to enabled categories
     * 2. Individual explicitly enabled domains
     * 3. Session-specific blocked domains
     * 4. Permanent blocked domains
     */
    suspend fun resolveActiveBlockingMap(
        sessionDomains: Set<String> = emptySet(),
        sessionCategories: Set<String> = emptySet()
    ): Map<String, String> = withContext(Dispatchers.IO) {
        seedInitialDataIfEmpty()

        val activeCategories = categoryDao.getActiveCategoriesSync().map { it.name }.toSet() + sessionCategories
        val allDomains = domainDao.getAllBlockedDomains().first()

        val resultMap = mutableMapOf<String, String>()

        for (domainEntity in allDomains) {
            val isCatActive = activeCategories.contains(domainEntity.category)
            val isSessionExplicit = sessionDomains.contains(domainEntity.domain)
            val isPermanent = domainEntity.isPermanent
            val isNotExpired = domainEntity.isPermanent || domainEntity.expiresAtMillis == 0L || domainEntity.expiresAtMillis > System.currentTimeMillis()

            if (isNotExpired && (isPermanent || isSessionExplicit || (domainEntity.isEnabled && isCatActive))) {
                resultMap[domainEntity.domain] = domainEntity.category
            }
        }

        // Include any session domains that may not be in the database
        for (sessionDomain in sessionDomains) {
            val norm = DomainMatcher.normalize(sessionDomain)
            if (norm.isNotBlank() && !resultMap.containsKey(norm)) {
                resultMap[norm] = "Focus Session"
            }
        }

        resultMap
    }
}
