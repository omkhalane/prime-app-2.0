package com.example.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 8 Progression Tiers based on Daily Score / 7-Day Rolling Average.
 */
enum class PrimeRankTier(
    val tierNumber: String,
    val title: String,
    val subtitle: String,
    val award: String,
    val minScore: Int,
    val maxScore: Int,
    val primaryColor: Color,
    val secondaryColor: Color
) {
    AWAKENING(
        tierNumber = "01",
        title = "Awakening",
        subtitle = "The journey begins.",
        award = "Beginner",
        minScore = 0,
        maxScore = 199,
        primaryColor = Color(0xFF90A4AE),
        secondaryColor = Color(0xFF546E7A)
    ),
    EXPLORER(
        tierNumber = "02",
        title = "Explorer",
        subtitle = "Taking the first steps.",
        award = "Bronze",
        minScore = 200,
        maxScore = 349,
        primaryColor = Color(0xFFCD7F32),
        secondaryColor = Color(0xFF8D5524)
    ),
    INITIATE(
        tierNumber = "03",
        title = "Initiate",
        subtitle = "Learning discipline.",
        award = "Bronze+",
        minScore = 350,
        maxScore = 499,
        primaryColor = Color(0xFFE6953B),
        secondaryColor = Color(0xFFA65B15)
    ),
    DISCIPLINED(
        tierNumber = "04",
        title = "Disciplined",
        subtitle = "Building healthy habits.",
        award = "Silver",
        minScore = 500,
        maxScore = 599,
        primaryColor = Color(0xFFCFD8DC),
        secondaryColor = Color(0xFF90A4AE)
    ),
    FOCUSED(
        tierNumber = "05",
        title = "Focused",
        subtitle = "Taking control.",
        award = "Silver+",
        minScore = 600,
        maxScore = 699,
        primaryColor = Color(0xFF80DEEA),
        secondaryColor = Color(0xFF00ACC1)
    ),
    ELITE(
        tierNumber = "06",
        title = "Elite",
        subtitle = "Strong digital discipline.",
        award = "Gold",
        minScore = 700,
        maxScore = 799,
        primaryColor = Color(0xFFFFD54F),
        secondaryColor = Color(0xFFFFA000)
    ),
    MASTER(
        tierNumber = "07",
        title = "Master",
        subtitle = "Exceptional consistency.",
        award = "Platinum",
        minScore = 800,
        maxScore = 899,
        primaryColor = Color(0xFF80CBC4),
        secondaryColor = Color(0xFF00BFA5)
    ),
    PRIME(
        tierNumber = "08",
        title = "PRIME",
        subtitle = "Operating at your best.",
        award = "Diamond",
        minScore = 900,
        maxScore = 1000,
        primaryColor = Color(0xFF76F2BF),
        secondaryColor = Color(0xFFE040FB)
    );

    companion object {
        fun fromScore(score: Int): PrimeRankTier {
            val clamped = score.coerceIn(0, 1000)
            return entries.firstOrNull { clamped in it.minScore..it.maxScore } ?: PRIME
        }
    }
}

/**
 * Breakdown of the 3 PRIME Score factors.
 * Default values are 0 (never hardcoded fallbacks).
 */
data class PrimeScoreBreakdown(
    val screenTimeScore: Float = 0f,
    val focusScore: Float = 0f,
    val blockingScore: Float = 0f,
    val totalScore: Int = 0,
    val actualScreenMinutes: Int = 0,
    val targetScreenMinutes: Int = 180,
    val actualFocusMinutes: Int = 0,
    val targetFocusMinutes: Int = 120,
    val actualBlockingMinutes: Int = 0,
    val targetBlockingMinutes: Int = 180,
    val rolling7DayAverage: Int = 0,
    val currentRank: PrimeRankTier = PrimeRankTier.AWAKENING,
    val hasPermission: Boolean = false,
    val errorMessage: String? = null
)

/**
 * Personalized daily targets.
 */
data class PrimeTargets(
    val screenTimeTargetMinutes: Int = 180,
    val focusTargetMinutes: Int = 120,
    val blockingTargetMinutes: Int = 180
)

/**
 * Badge Rarity Levels.
 */
enum class BadgeRarity(
    val label: String,
    val material: String,
    val color: Color,
    val xpValue: Int
) {
    COMMON("Common", "Bronze", Color(0xFFCD7F32), 100),
    UNCOMMON("Uncommon", "Silver", Color(0xFFCFD8DC), 250),
    RARE("Rare", "Gold", Color(0xFFFFD700), 300),
    EPIC("Epic", "Platinum", Color(0xFF80DEEA), 600),
    LEGENDARY("Legendary", "Diamond", Color(0xFFB388FF), 1000),
    MYTHIC("Mythic", "Animated Cosmic", Color(0xFFFF4081), 2000)
}

/**
 * Badge level in the 3-tier hierarchy.
 */
enum class BadgeLevel(val roman: String, val title: String, val xpValue: Int) {
    LEVEL_I("I", "Bronze · Starter", 100),
    LEVEL_II("II", "Silver · Advanced", 250),
    LEVEL_III("III", "Gold · Master", 500)
}

/**
 * The 12 Core Achievement Categories.
 */
enum class BadgeCategory(
    val code: String,
    val title: String,
    val description: String
) {
    CATEGORY_A("A", "Screen-time discipline", "Reward: Reducing unnecessary device usage"),
    CATEGORY_B("B", "Focus mastery", "Reward: Deep work and uninterrupted concentration"),
    CATEGORY_C("C", "App & website blocking", "Reward: Taking control of digital distractions"),
    CATEGORY_D("D", "Streaks & consistency", "Reward: Showing up every day"),
    CATEGORY_E("E", "Pomodoro mastery", "Reward: Rhythm and time architecture"),
    CATEGORY_F("F", "Digital detox", "Reward: Reconnecting with the physical world"),
    CATEGORY_G("G", "Morning & night discipline", "Reward: Phone-free morning and bedtime boundaries"),
    CATEGORY_H("H", "Goal achievement", "Reward: Execution and mission completion"),
    CATEGORY_I("I", "Stopwatch & timer", "Reward: Precision timekeeping"),
    CATEGORY_J("J", "Weekly digital wellbeing", "Reward: Holistic balance and mindful breaks"),
    CATEGORY_K("K", "Social & community", "Reward: Healthy competition and challenges"),
    CATEGORY_L("L", "Overall PRIME journey", "Reward: Reaching and maintaining peak discipline"),
    SPECIAL("S", "Special & Rare", "Exclusive milestones and prestige awards")
}

/**
 * A collectible badge in the PRIME achievement system.
 */
data class PrimeBadge(
    val id: String,
    val name: String,
    val category: BadgeCategory,
    val level: BadgeLevel,
    val rarity: BadgeRarity,
    val xpValue: Int,
    val description: String,
    val currentProgress: Float,
    val targetRequirement: Float,
    val unit: String,
    val isUnlocked: Boolean = false,
    val unlockedDate: String? = null,
    val isSpecial: Boolean = false,
    val iconType: String = "shield"
)

/**
 * Daily Quest item for actionable engagement.
 */
data class DailyQuest(
    val id: String,
    val title: String,
    val description: String,
    val currentProgress: Int,
    val targetProgress: Int,
    val unit: String,
    val xpReward: Int,
    val isCompleted: Boolean = false,
    val isClaimed: Boolean = false
)

/**
 * Weekly Mission item.
 */
data class WeeklyMission(
    val id: String,
    val title: String,
    val description: String,
    val currentProgress: Int,
    val targetProgress: Int,
    val unit: String,
    val xpReward: Int,
    val isCompleted: Boolean = false,
    val isClaimed: Boolean = false
)

/**
 * Overall Progression State.
 */
data class PrimeProgression(
    val totalXp: Int = 0,
    val primeLevel: Int = 1,
    val currentLevelXp: Int = 0,
    val xpForNextLevel: Int = 400,
    val pinnedBadgeIds: List<String> = emptyList(),
    val totalBadgesCount: Int = 42,
    val unlockedBadgesCount: Int = 0,
    val completionPercentage: Float = 0f
)
