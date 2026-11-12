package com.example.vpn.repo

import android.content.Context
import com.example.vpn.db.BlockedEventDao
import com.example.vpn.db.BlockedWebsiteEventEntity
import com.example.vpn.db.CategoryAttemptCount
import com.example.vpn.db.DomainAttemptCount
import com.example.vpn.db.WebsiteBlockDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.concurrent.ConcurrentLinkedQueue

class BlockingAnalyticsRepository(
    private val context: Context,
    private val database: WebsiteBlockDatabase = WebsiteBlockDatabase.getInstance(context)
) {
    private val eventDao: BlockedEventDao = database.blockedEventDao()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Bounded in-memory event queue for batched DB writes
    private val eventQueue = ConcurrentLinkedQueue<BlockedWebsiteEventEntity>()
    private val flushMutex = Mutex()
    private var batchFlushJob: Job? = null

    init {
        startBatchFlusher()
    }

    private fun startBatchFlusher() {
        batchFlushJob?.cancel()
        batchFlushJob = scope.launch {
            while (true) {
                delay(2000L) // Flush batched logs every 2 seconds
                flushEventQueue()
            }
        }
    }

    fun logBlockedAttempt(
        domain: String,
        category: String,
        reason: String = "PRIME Rule Matched",
        sessionId: String? = null,
        ruleId: String? = null
    ) {
        val event = BlockedWebsiteEventEntity(
            domain = domain,
            category = category,
            timestamp = System.currentTimeMillis(),
            sessionId = sessionId,
            ruleId = ruleId,
            blockingReason = reason
        )
        while (eventQueue.size >= 2000) eventQueue.poll()
        eventQueue.offer(event)
        if (eventQueue.size >= 10) {
            scope.launch { flushEventQueue() }
        }
    }

    private suspend fun flushEventQueue() = withContext(Dispatchers.IO) {
        if (eventQueue.isEmpty()) return@withContext
        val batch = mutableListOf<BlockedWebsiteEventEntity>()
        while (eventQueue.isNotEmpty() && batch.size < 50) {
            eventQueue.poll()?.let { batch.add(it) }
        }
        if (batch.isNotEmpty()) {
            try {
                eventDao.insertEvents(batch)
            } catch (_: Exception) {
                batch.forEach(eventQueue::offer)
            }
        }
    }

    fun getTotalBlockedCount(): Flow<Int> = eventDao.getTotalBlockedCount()

    fun getTodayBlockedCount(): Flow<Int> {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return eventDao.getBlockedCountSince(cal.timeInMillis)
    }

    fun getUniqueDomainsCount(): Flow<Int> = eventDao.getUniqueBlockedDomainsCount()

    fun getTopBlockedDomains(limit: Int = 8): Flow<List<DomainAttemptCount>> = eventDao.getTopBlockedDomains(limit)

    fun getCategoryDistribution(): Flow<List<CategoryAttemptCount>> = eventDao.getCategoryDistribution()

    fun getRecentEvents(limit: Int = 50): Flow<List<BlockedWebsiteEventEntity>> = eventDao.getRecentEvents(limit)

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        flushMutex.withLock {
            eventQueue.clear()
            eventDao.clearAllEvents()
        }
    }
}
