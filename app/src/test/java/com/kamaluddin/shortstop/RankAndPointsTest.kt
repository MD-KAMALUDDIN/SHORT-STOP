package com.kamaluddin.shortstop

import com.kamaluddin.shortstop.database.UserStatsEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RankAndPointsTest {

    private fun stats(
        streak: Int = 0,
        interventions: Int = 0,
        points: Int = 0,
        pendingRewards: Int = 0,
        timeSaved: Long = 0L,
        studySessions: Int = 0
    ) = UserStatsEntity(
        points = points,
        currentStreak = streak,
        lastInterventionDate = "",
        totalInterventions = interventions,
        totalTimeSaved = timeSaved,
        successfulStudySessions = studySessions,
        totalPointsEarned = 0,
        pendingRewards = pendingRewards
    )

    // ── calculateRankScore ────────────────────────────────────────────────────
    // Formula: (streak × 25) + (studySessions × 15) + (timeSavedMinutes / 5) - (emergencyExits × 20), floor 0

    @Test
    fun rankScore_zero_for_fresh_user() {
        assertEquals(0, calculateRankScore(stats()))
    }

    @Test
    fun rankScore_streak_weighted_by_25() {
        // streak=4 → 4×25 = 100
        assertEquals(100, calculateRankScore(stats(streak = 4)))
    }

    @Test
    fun rankScore_study_sessions_weighted_by_15() {
        // studySessions=3 → 3×15 = 45
        assertEquals(45, calculateRankScore(stats(studySessions = 3)))
    }

    @Test
    fun rankScore_time_saved_divided_by_5() {
        // 100 minutes saved → 100/5 = 20
        assertEquals(20, calculateRankScore(stats(timeSaved = 100 * 60000L)))
    }

    @Test
    fun rankScore_emergency_exits_penalised_by_20() {
        // streak=4 → 100, emergencyExits=2 → -40, total=60
        val s = UserStatsEntity(
            points = 0, currentStreak = 4, lastInterventionDate = "",
            totalInterventions = 0, totalTimeSaved = 0,
            successfulStudySessions = 0, totalPointsEarned = 0,
            pendingRewards = 0, totalEmergencyExits = 2
        )
        assertEquals(60, calculateRankScore(s))
    }

    @Test
    fun rankScore_floors_at_zero() {
        // emergencyExits=10 → -200, nothing else → coerced to 0
        val s = UserStatsEntity(
            points = 0, currentStreak = 0, lastInterventionDate = "",
            totalInterventions = 0, totalTimeSaved = 0,
            successfulStudySessions = 0, totalPointsEarned = 0,
            pendingRewards = 0, totalEmergencyExits = 10
        )
        assertEquals(0, calculateRankScore(s))
    }

    @Test
    fun rankScore_combined() {
        // streak=4 → 100, studySessions=3 → 45, timeSaved=100min → 20, emergencyExits=2 → -40, total=125
        val s = UserStatsEntity(
            points = 0, currentStreak = 4, lastInterventionDate = "",
            totalInterventions = 0, totalTimeSaved = 100 * 60000L,
            successfulStudySessions = 3, totalPointsEarned = 0,
            pendingRewards = 0, totalEmergencyExits = 2
        )
        assertEquals(125, calculateRankScore(s))
    }

    // ── getRankFromScore ──────────────────────────────────────────────────────

    @Test
    fun rank_sprout_at_zero() {
        assertTrue(getRankFromScore(0).contains("Sprout"))
    }

    @Test
    fun rank_apprentice_at_100() {
        assertTrue(getRankFromScore(100).contains("Apprentice"))
    }

    @Test
    fun rank_focused_at_300() {
        assertTrue(getRankFromScore(300).contains("Focused"))
    }

    @Test
    fun rank_monk_at_750() {
        assertTrue(getRankFromScore(750).contains("Monk"))
    }

    @Test
    fun rank_sentinel_at_1500() {
        assertTrue(getRankFromScore(1500).contains("Sentinel"))
    }

    @Test
    fun rank_sovereign_at_3000() {
        assertTrue(getRankFromScore(3000).contains("Sovereign"))
    }

    // ── deductPoints logic (pure, no DB) ─────────────────────────────────────

    @Test
    fun deductPoints_floors_at_zero() {
        val current = stats(points = 30)
        val newPoints = (current.points - 50).coerceAtLeast(0)
        assertEquals(0, newPoints)
    }

    @Test
    fun deductPoints_normal_deduction() {
        val current = stats(points = 100)
        val newPoints = (current.points - 50).coerceAtLeast(0)
        assertEquals(50, newPoints)
    }

    // ── pendingRewards ────────────────────────────────────────────────────────

    @Test
    fun claimPendingRewards_adds_to_points() {
        val current = stats(points = 40, pendingRewards = 25)
        val updated = current.copy(
            points = current.points + current.pendingRewards,
            totalPointsEarned = current.totalPointsEarned + current.pendingRewards,
            pendingRewards = 0
        )
        assertEquals(65, updated.points)
        assertEquals(25, updated.totalPointsEarned)
        assertEquals(0, updated.pendingRewards)
    }

    @Test
    fun claimPendingRewards_noop_when_zero() {
        val current = stats(points = 40, pendingRewards = 0)
        // guard: pendingRewards <= 0 → no update
        val shouldUpdate = current.pendingRewards > 0
        assertTrue(!shouldUpdate)
        assertEquals(40, current.points)
    }
}
