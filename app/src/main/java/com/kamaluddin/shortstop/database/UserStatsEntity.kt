package com.kamaluddin.shortstop.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_stats")
data class UserStatsEntity(
    @PrimaryKey val id: Int = 1,
    val points: Int = 0,
    val currentStreak: Int = 0,
    val lastInterventionDate: String = "",
    val totalInterventions: Int = 0,
    val totalTimeSaved: Long = 0L,
    val successfulStudySessions: Int = 0,
    val totalPointsEarned: Int = 0,
    val pendingRewards: Int = 0,
    val totalEmergencyExits: Int = 0
)
