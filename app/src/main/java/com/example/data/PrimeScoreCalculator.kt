package com.example.data

import com.example.model.PrimeRankTier
import com.example.model.PrimeScoreBreakdown
import com.example.model.PrimeTargets
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Implements the normalized PRIME score calculation:
 * - Screen-time control: 0–300 points
 * - Focus time: 0–350 points
 * - Blocking discipline: 0–350 points
 * Total maximum: 1,000 points.
 */
object PrimeScoreCalculator {

    /**
     * Calculate Screen-time score:
     * S_c = 300 * max(0, 1 - max(0, S - T) / T)
     *
     * At or below target: 300 points.
     * At 1.5x target: 150 points.
     * At 2x target: 0 points.
     */
    fun calculateScreenTimeScore(
        actualMinutes: Int,
        targetMinutes: Int
    ): Float {
        val t = if (targetMinutes <= 0) 180f else targetMinutes.toFloat()
        val s = max(0, actualMinutes).toFloat()
        val overage = max(0f, s - t)
        val score = 300f * max(0f, 1f - (overage / t))
        return score.coerceIn(0f, 300f)
    }

    /**
     * Calculate Focus score:
     * F_c = 350 * min(1, F / F_t)
     */
    fun calculateFocusScore(
        verifiedFocusMinutes: Int,
        targetFocusMinutes: Int
    ): Float {
        val ft = if (targetFocusMinutes <= 0) 120f else targetFocusMinutes.toFloat()
        val f = max(0, verifiedFocusMinutes).toFloat()
        val ratio = min(1f, f / ft)
        return (350f * ratio).coerceIn(0f, 350f)
    }

    /**
     * Calculate Blocking discipline score:
     * B_c = 350 * min(1, B / B_t)
     */
    fun calculateBlockingScore(
        verifiedBlockedMinutes: Int,
        targetBlockedMinutes: Int
    ): Float {
        val bt = if (targetBlockedMinutes <= 0) 180f else targetBlockedMinutes.toFloat()
        val b = max(0, verifiedBlockedMinutes).toFloat()
        val ratio = min(1f, b / bt)
        return (350f * ratio).coerceIn(0f, 350f)
    }

    /**
     * Compute the full PRIME score breakdown.
     */
    fun computeScore(
        actualScreenMinutes: Int,
        verifiedFocusMinutes: Int,
        verifiedBlockedMinutes: Int,
        targets: PrimeTargets = PrimeTargets(),
        past7DayScores: List<Int> = emptyList(),
        hasPermission: Boolean = false,
        errorMessage: String? = null
    ): PrimeScoreBreakdown {
        if (!hasPermission) {
            return PrimeScoreBreakdown(
                screenTimeScore = 0f,
                focusScore = 0f,
                blockingScore = 0f,
                totalScore = 0,
                actualScreenMinutes = 0,
                targetScreenMinutes = targets.screenTimeTargetMinutes,
                actualFocusMinutes = 0,
                targetFocusMinutes = targets.focusTargetMinutes,
                actualBlockingMinutes = 0,
                targetBlockingMinutes = targets.blockingTargetMinutes,
                rolling7DayAverage = 0,
                currentRank = PrimeRankTier.AWAKENING,
                hasPermission = false,
                errorMessage = errorMessage ?: "Usage access permission required"
            )
        }

        val screenScore = calculateScreenTimeScore(
            actualMinutes = actualScreenMinutes,
            targetMinutes = targets.screenTimeTargetMinutes
        )
        val focusScore = calculateFocusScore(
            verifiedFocusMinutes = verifiedFocusMinutes,
            targetFocusMinutes = targets.focusTargetMinutes
        )
        val blockingScore = calculateBlockingScore(
            verifiedBlockedMinutes = verifiedBlockedMinutes,
            targetBlockedMinutes = targets.blockingTargetMinutes
        )

        val totalScore = (screenScore + focusScore + blockingScore).roundToInt().coerceIn(0, 1000)

        // Rolling 7-day average computation: strictly based on real scores
        val rollingAverage = if (past7DayScores.isNotEmpty()) {
            val all = past7DayScores.takeLast(6) + totalScore
            (all.sum().toFloat() / all.size).roundToInt().coerceIn(0, 1000)
        } else {
            totalScore
        }

        // Rank tier determined by 7-day rolling average (or current day if early)
        val rank = PrimeRankTier.fromScore(rollingAverage)

        return PrimeScoreBreakdown(
            screenTimeScore = screenScore,
            focusScore = focusScore,
            blockingScore = blockingScore,
            totalScore = totalScore,
            actualScreenMinutes = actualScreenMinutes,
            targetScreenMinutes = targets.screenTimeTargetMinutes,
            actualFocusMinutes = verifiedFocusMinutes,
            targetFocusMinutes = targets.focusTargetMinutes,
            actualBlockingMinutes = verifiedBlockedMinutes,
            targetBlockingMinutes = targets.blockingTargetMinutes,
            rolling7DayAverage = rollingAverage,
            currentRank = rank,
            hasPermission = true,
            errorMessage = null
        )
    }
}
