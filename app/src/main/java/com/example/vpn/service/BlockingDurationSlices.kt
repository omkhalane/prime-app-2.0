package com.example.vpn.service

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

internal data class DailyBlockingDuration(
    val dateKey: String,
    val durationMillis: Long
)

/**
 * Splits a measured enforcement interval at local midnight without losing or
 * double-counting milliseconds. Kept Android-independent so boundary behavior
 * can be covered by JVM unit tests.
 */
internal object BlockingDurationSlices {
    fun split(
        startMillis: Long,
        endMillis: Long,
        timeZone: TimeZone = TimeZone.getDefault()
    ): List<DailyBlockingDuration> {
        if (endMillis <= startMillis) return emptyList()

        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            this.timeZone = timeZone
        }
        val slices = mutableListOf<DailyBlockingDuration>()
        var cursor = startMillis

        while (cursor < endMillis) {
            val dayStart = Calendar.getInstance(timeZone).apply {
                timeInMillis = cursor
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val nextDay = (dayStart.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, 1)
            }.timeInMillis
            val sliceEnd = minOf(endMillis, nextDay)
            if (sliceEnd <= cursor) return emptyList()

            slices += DailyBlockingDuration(
                dateKey = formatter.format(Date(cursor)),
                durationMillis = sliceEnd - cursor
            )
            cursor = sliceEnd
        }

        return slices
    }
}
