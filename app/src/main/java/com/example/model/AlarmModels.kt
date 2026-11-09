package com.example.model

enum class AlarmRecurrence(val displayName: String) {
    ONCE("Once"),
    DAILY("Every day"),
    WEEKDAYS("Weekdays"),
    WEEKENDS("Weekends"),
    CUSTOM("Custom")
}

enum class AlarmMode(val displayName: String) {
    NORMAL("Normal Alarm"),
    WAKE_UP("Wake-Up Alarm"),
    HARD_MODE("Hard Mode")
}

enum class ChallengeCategory(val displayName: String) {
    MATH("Mathematics"),
    SENTENCE_TYPING("Sentence Typing"),
    COLOR_RECOGNITION("Color Recognition"),
    MEMORY("Memory Sequence"),
    SEQUENCE("Logical Sequence")
}

enum class ChallengeDifficulty(val displayName: String) {
    EASY("Easy"),
    MEDIUM("Medium"),
    HARD("Hard")
}

data class ChallengeConfig(
    val category: ChallengeCategory = ChallengeCategory.MATH,
    val difficulty: ChallengeDifficulty = ChallengeDifficulty.MEDIUM,
    val requiredConsecutive: Int = 3,
    val isRandomizedCategory: Boolean = false,
    val combineMultipleChallenges: Boolean = false
)

data class RingtoneOption(
    val id: String,
    val title: String,
    val uriString: String? = null,
    val isSynthetic: Boolean = true
)

data class AlarmItem(
    val id: String,
    val hour: Int, // 0..23
    val minute: Int, // 0..59
    val name: String = "Alarm",
    val isEnabled: Boolean = true,
    val recurrence: AlarmRecurrence = AlarmRecurrence.DAILY,
    val selectedDays: Set<Int> = setOf(1, 2, 3, 4, 5), // 1=Mon, 2=Tue, 3=Wed, 4=Thu, 5=Fri, 6=Sat, 7=Sun
    val specificDateMillis: Long? = null,
    val ringtoneId: String = "zen_crystal",
    val ringtoneName: String = "Zen Crystal Chime",
    val ringtoneUriString: String? = null,
    val vibrationEnabled: Boolean = true,
    val snoozeDurationMinutes: Int = 10,
    val alarmMode: AlarmMode = AlarmMode.NORMAL,
    val challengeConfig: ChallengeConfig = ChallengeConfig()
)
