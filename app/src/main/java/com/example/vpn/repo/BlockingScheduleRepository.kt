package com.example.vpn.repo

import android.content.Context
import com.example.vpn.db.BlockingScheduleDao
import com.example.vpn.db.BlockingScheduleEntity
import com.example.vpn.db.WebsiteBlockDatabase
import com.example.vpn.dns.DomainMatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.text.ParsePosition

class BlockingScheduleRepository(
    private val context: Context,
    private val database: WebsiteBlockDatabase = WebsiteBlockDatabase.getInstance(context)
) {
    private val scheduleDao: BlockingScheduleDao = database.scheduleDao()
    fun getAllSchedules(): Flow<List<BlockingScheduleEntity>> = scheduleDao.getAllSchedules()

    suspend fun addSchedule(
        title: String,
        startHour: Int,
        startMinute: Int,
        endHour: Int,
        endMinute: Int,
        activeDays: Set<Int>,
        categories: Set<String>,
        customDomains: Set<String> = emptySet(),
        startDateIso: String? = null,
        endDateIso: String? = null
    ) = withContext(Dispatchers.IO) {
        require(startHour in 0..23 && endHour in 0..23)
        require(startMinute in 0..59 && endMinute in 0..59)
        require(activeDays.isNotEmpty()) { "Select at least one active day" }
        require(activeDays.all { it in 0..6 })
        require(categories.isNotEmpty() || customDomains.isNotEmpty()) { "Select at least one blocking category or domain" }
        require(categories.none { it.contains(',') }) { "Category names cannot contain commas" }
        require(customDomains.all { DomainMatcher.isValidDomain(it) }) { "A scheduled domain is invalid" }
        require(startDateIso == null || isValidDate(startDateIso))
        require(endDateIso == null || isValidDate(endDateIso))
        require(startDateIso == null || endDateIso == null || startDateIso <= endDateIso)
        scheduleDao.insertSchedule(
            BlockingScheduleEntity(
                id = "sched_${UUID.randomUUID()}",
                title = title.trim().ifBlank { "Website blocking" },
                startHour = startHour,
                startMinute = startMinute,
                endHour = endHour,
                endMinute = endMinute,
                activeDaysCsv = activeDays.sorted().joinToString(","),
                blockedCategoriesCsv = categories.joinToString(","),
                customDomainsCsv = customDomains.joinToString(","),
                startDateIso = startDateIso,
                endDateIso = endDateIso,
                isEnabled = true
            )
        )
    }

    suspend fun toggleScheduleEnabled(id: String, isEnabled: Boolean) = withContext(Dispatchers.IO) {
        scheduleDao.updateScheduleEnabled(id, isEnabled)
    }

    suspend fun deleteSchedule(id: String) = withContext(Dispatchers.IO) {
        scheduleDao.deleteSchedule(id)
    }

    suspend fun getCurrentlyActiveSchedules(): List<BlockingScheduleEntity> = withContext(Dispatchers.IO) {
        val now = Calendar.getInstance()
        val currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val today = dateKey(now.time)
        val todayDay = now.get(Calendar.DAY_OF_WEEK) - 1
        val previous = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
        val previousDay = previous.get(Calendar.DAY_OF_WEEK) - 1
        val previousDate = dateKey(previous.time)

        scheduleDao.getActiveSchedulesSync().filter { schedule ->
            val start = schedule.startHour * 60 + schedule.startMinute
            val end = schedule.endHour * 60 + schedule.endMinute
            val overnight = end <= start

            val matchesDate: (String) -> Boolean = { date ->
                (schedule.startDateIso == null || date >= schedule.startDateIso) &&
                    (schedule.endDateIso == null || date <= schedule.endDateIso)
            }
            val days = schedule.activeDaysCsv.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()

            val activeFromToday = days.contains(todayDay) && matchesDate(today) &&
                if (overnight) currentMinutes >= start else currentMinutes in start until end
            val activeFromYesterday = overnight && currentMinutes < end &&
                days.contains(previousDay) && matchesDate(previousDate)
            activeFromToday || activeFromYesterday
        }
    }

    private fun dateKey(date: Date): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(date)

    private fun isValidDate(value: String): Boolean = try {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
        val position = ParsePosition(0)
        val parsed = format.parse(value, position)
        parsed != null && position.index == value.length
    } catch (_: Exception) {
        false
    }
}
