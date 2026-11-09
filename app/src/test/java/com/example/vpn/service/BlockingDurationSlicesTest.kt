package com.example.vpn.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class BlockingDurationSlicesTest {

    private val utc = TimeZone.getTimeZone("UTC")
    private val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).apply {
        timeZone = utc
        isLenient = false
    }

    private fun instant(value: String): Long = requireNotNull(formatter.parse(value)).time

    @Test
    fun splitsIntervalAcrossMidnightWithoutLosingTime() {
        val slices = BlockingDurationSlices.split(
            startMillis = instant("2026-11-08 23:59:30"),
            endMillis = instant("2026-11-09 00:01:00"),
            timeZone = utc
        )

        assertEquals(
            listOf(
                DailyBlockingDuration("2026-11-08", 30_000L),
                DailyBlockingDuration("2026-11-09", 60_000L)
            ),
            slices
        )
        assertEquals(90_000L, slices.sumOf { it.durationMillis })
    }

    @Test
    fun keepsSameDayIntervalInOneSlice() {
        val slices = BlockingDurationSlices.split(
            startMillis = instant("2026-11-09 10:00:00"),
            endMillis = instant("2026-11-09 10:12:34"),
            timeZone = utc
        )

        assertEquals(listOf(DailyBlockingDuration("2026-11-09", 754_000L)), slices)
    }

    @Test
    fun returnsNoSlicesForEmptyOrReversedIntervals() {
        assertTrue(
            BlockingDurationSlices.split(
                instant("2026-11-09 10:00:00"),
                instant("2026-11-09 10:00:00"),
                utc
            ).isEmpty()
        )
        assertTrue(
            BlockingDurationSlices.split(
                instant("2026-11-09 10:01:00"),
                instant("2026-11-09 10:00:00"),
                utc
            ).isEmpty()
        )
    }

    @Test
    fun splitsMultiDayIntervalAndPreservesExactTotal() {
        val start = instant("2026-11-08 23:00:00")
        val end = instant("2026-11-10 01:30:00")
        val slices = BlockingDurationSlices.split(start, end, utc)

        assertEquals(
            listOf("2026-11-08", "2026-11-09", "2026-11-10"),
            slices.map { it.dateKey }
        )
        assertEquals(end - start, slices.sumOf { it.durationMillis })
    }
}
