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
}
