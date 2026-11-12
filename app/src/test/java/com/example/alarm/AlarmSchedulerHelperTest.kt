package com.example.alarm

import com.example.model.AlarmItem
import com.example.model.AlarmRecurrence
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.util.Calendar

class AlarmSchedulerHelperTest {
    @Test fun customAlarmWithoutSelectedDaysIsNotScheduled() {
        val now = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 12, 8, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val alarm = AlarmItem(
            id = "empty-custom",
            hour = 9,
            minute = 0,
            recurrence = AlarmRecurrence.CUSTOM,
            selectedDays = emptySet()
        )
        assertNull(AlarmSchedulerHelper.calculateNextTriggerMillis(alarm, now))
    }

    @Test fun onceAlarmWithoutSpecificDateStillSchedulesNextOccurrence() {
        val now = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 12, 8, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val alarm = AlarmItem(id = "once", hour = 9, minute = 0, recurrence = AlarmRecurrence.ONCE)
        assertNotNull(AlarmSchedulerHelper.calculateNextTriggerMillis(alarm, now))
    }
    @Test fun weekdaysUseMondayThroughFridayIndexing() {
        val now = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 15, 8, 0, 0) // Sunday
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val alarm = AlarmItem(
            id = "weekdays",
            hour = 9,
            minute = 0,
            recurrence = AlarmRecurrence.WEEKDAYS,
            selectedDays = setOf(1, 2, 3, 4, 5)
        )
        val next = Calendar.getInstance().apply {
            timeInMillis = AlarmSchedulerHelper.calculateNextTriggerMillis(alarm, now)!!
        }
        assertEquals(Calendar.MONDAY, next.get(Calendar.DAY_OF_WEEK))
        assertEquals(16, next.get(Calendar.DAY_OF_MONTH))
    }

}
