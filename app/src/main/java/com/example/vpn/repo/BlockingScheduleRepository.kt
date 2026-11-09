package com.example.vpn.repo

import android.content.Context
import com.example.vpn.db.BlockingScheduleDao
import com.example.vpn.db.BlockingScheduleEntity
import com.example.vpn.db.WebsiteBlockDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.Calendar

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
        activeDays: Set<Int>, // 0=Sun..6=Sat
        categories: Set<String>,
        customDomains: Set<String> = emptySet()
    ) = withContext(Dispatchers.IO) {
        val schedule = BlockingScheduleEntity(
            id = "sched_${System.currentTimeMillis()}",
            title = title,
            startHour = startHour,
            startMinute = startMinute,
            endHour = endHour,
            endMinute = endMinute,
            activeDaysCsv = activeDays.joinToString(","),
            blockedCategoriesCsv = categories.joinToString(","),
            customDomainsCsv = customDomains.joinToString(","),
            isEnabled = true
        )
        scheduleDao.insertSchedule(schedule)
    }

    suspend fun toggleScheduleEnabled(id: String, isEnabled: Boolean) = withContext(Dispatchers.IO) {
        scheduleDao.updateScheduleEnabled(id, isEnabled)
    }

    suspend fun deleteSchedule(id: String) = withContext(Dispatchers.IO) {
        scheduleDao.deleteSchedule(id)
    }

    suspend fun getCurrentlyActiveSchedules(): List<BlockingScheduleEntity> = withContext(Dispatchers.IO) {
        val all = scheduleDao.getActiveSchedulesSync()
        val cal = Calendar.getInstance()
        val currentDay = cal.get(Calendar.DAY_OF_WEEK) - 1 // 0=Sun..6=Sat
        val currentMinutesOfDay = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)

        all.filter { schedule ->
            val days = schedule.activeDaysCsv.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
            if (!days.contains(currentDay)) return@filter false

            val startMin = schedule.startHour * 60 + schedule.startMinute
            val endMin = schedule.endHour * 60 + schedule.endMinute

            if (startMin <= endMin) {
                currentMinutesOfDay in startMin..endMin
            } else {
                // Crosses midnight (e.g. 22:00 to 06:00)
                currentMinutesOfDay >= startMin || currentMinutesOfDay <= endMin
            }
        }
    }
}
