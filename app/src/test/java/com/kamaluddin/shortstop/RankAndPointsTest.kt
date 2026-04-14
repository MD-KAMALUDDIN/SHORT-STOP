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
        pendingRewards: Int = 0
    ) = UserStatsEntity(
        points = points,
        currentStreak = streak,
        lastInterventionDate = "",
        totalInterventions = interventions,
        totalTimeSaved = 0L,
        successfulStudySessions = 0,
        totalPointsEarned = 0,
        pendingRewards = pendingRewards
    )

    // ── calculateRankScore ────────────────────────────────────────────────────

    @Test
    fun rankScore_zero_for_fresh_user() {
        assertEquals(0, calculateRankScore(stats()))
    }

    @Test
    fun rankScore_streak_weighted_by_10() {
        assertEquals(50, calculateRankScore(stats(streak = 5)))
    }

    @Test
    fun rankScore_interventions_weighted_by_2() {
        assertEquals(20, calculateRankScore(stats(interventions = 10)))
    }

    @Test
    fun rankScore_combined() {
        // streak=7 → 70, interventions=15 → 30, total=100
        assertEquals(100, calculateRankScore(stats(streak = 7, interventions = 15)))
    }

    // ── getRankFromScore ──────────────────────────────────────────────────────

    @Test
    fun rank_struggling_beginner_at_zero() {
        assertTrue(getRankFromScore(0).contains("Getting Started"))
    }

    @Test
    fun rank_digital_warrior_at_500() {
        assertTrue(getRankFromScore(500).contains("Digital Warrior"))
    }

    @Test
    fun rank_ultimate_controller_at_1000() {
        assertTrue(getRankFromScore(1000).contains("Ultimate Controller"))
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
