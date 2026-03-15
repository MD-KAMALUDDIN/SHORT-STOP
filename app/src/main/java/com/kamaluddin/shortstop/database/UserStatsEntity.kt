package com.kamaluddin.shortstop.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_stats")
data class UserStatsEntity(
    @PrimaryKey val id: Int = 1,
    val points: Int,
    val currentStreak: Int,
    val lastInterventionDate: String,
    val totalInterventions: Int,
    val totalTimeSaved: Long,
    val successfulStudySessions: Int,
    val totalPointsEarned: Int,
    val dailyExitCount: Int = 0,
    val lastRewardDate: String = ""
)
