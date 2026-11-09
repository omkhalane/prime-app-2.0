package com.example.model

enum class PomodoroSessionType(val displayName: String) {
    FOCUS("Focus"),
    SHORT_BREAK("Short Break"),
    LONG_BREAK("Long Break")
}

data class PomodoroSettings(
    val focusMinutes: Int = 25,
    val shortBreakMinutes: Int = 5,
    val longBreakMinutes: Int = 15,
    val roundsPerCycle: Int = 4,
    val totalCycles: Int = 1
)

data class PomodoroState(
    val sessionType: PomodoroSessionType = PomodoroSessionType.FOCUS,
    val currentRound: Int = 1,
    val currentCycle: Int = 1,
    val remainingSeconds: Int = 25 * 60,
    val totalSessionSeconds: Int = 25 * 60,
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val completedRoundsCount: Int = 0
)
