package com.example.model

data class StopwatchLap(
    val lapNumber: Int,
    val lapTimeMillis: Long,
    val totalTimeMillis: Long
)

data class StopwatchState(
    val isRunning: Boolean = false,
    val elapsedMillis: Long = 0L,
    val laps: List<StopwatchLap> = emptyList()
)
