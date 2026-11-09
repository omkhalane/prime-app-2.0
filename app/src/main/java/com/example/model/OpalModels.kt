package com.example.model

enum class NavigationTab(val title: String) {
    HOME("Home"),
    MY_APPS("Shields"),
    TIMER("Timer")
}

data class BlockedAppItem(
    val id: String,
    val name: String,
    val packageName: String = "",
    val isWebsite: Boolean = false,
    val usageToday: String = "Not used today",
    val usageMillis: Long = 0L,
    val category: String = "Distracting",
    val wellbeingCategory: String = "Other",
    val isBlocked: Boolean = true,
    val brandColorHex: Long = 0xFF76F2BF
)

data class BlockRule(
    val id: String,
    val title: String,
    val startsInText: String,
    val fromTime: String = "9:00 am",
    val toTime: String = "5:00 pm",
    val daysLabel: String = "Weekdays",
    val activeDays: Set<Int> = setOf(0, 1, 2, 3, 4), // 0=M, 1=T, 2=W, 3=T, 4=F, 5=S, 6=S
    val blockedAppIds: List<String> = emptyList(),
    val isHardMode: Boolean = false,
    val allowBreaks: Boolean = true,
    val isPaused: Boolean = false,
    val isCustom: Boolean = false
)

data class TimerPreset(
    val id: String,
    val title: String,
    val durationMinutes: Int,
    val formattedDuration: String,
    val description: String = "",
    val category: String = "Focus",
    val drawableResId: Int = com.example.R.drawable.img_deep_study,
    val themeColorHex: Long = 0xFF76F2BF
)

data class UserProfile(
    val name: String = "omk",
    val streakDays: Int = 0,
    val bestStreakDays: Int = 0,
    val isActiveToday: Boolean = false,
    val weekDaysActive: List<Boolean> = listOf(false, false, false, false, false, false, false),
    val score: Int = 0,
    val sleepScore: Int = 0,
    val focusScore: Int = 0,
    val restScore: Int = 0,
    val screenTimeToday: String = "0m",
    val screenTimeMillis: Long = 0L,
    val isPro: Boolean = false,
    val emergencyPassesRemaining: Int = 1,
    val hasUsagePermission: Boolean = false,
    val primeScoreBreakdown: PrimeScoreBreakdown = PrimeScoreBreakdown(),
    val primeTargets: PrimeTargets = PrimeTargets(),
    val primeProgression: PrimeProgression = PrimeProgression()
)

enum class OnboardingStep {
    WELCOME_ROCK,
    NAME_INPUT,
    AUTH_CHECK,
    AUTH_OPTIONS,
    GOAL_SELECTION,
    SCREEN_TIME_ESTIMATE,
    BAD_NEWS_STAT,
    GOOD_NEWS_STAT,
    PERMISSIONS_INTRO,
    HOW_IT_WORKS_1,
    HOW_IT_WORKS_2,
    HOW_IT_WORKS_3,
    NOTIFICATIONS_PROMPT,
    ACCESSIBILITY_GUIDE,
    SELECT_DISTRACTIONS,
    FINAL_FIST_BUMP
}
