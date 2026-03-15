package com.kamaluddin.shortstop.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "blocked_apps",
    indices = [Index(value = ["packageName"], unique = true)]
)
data class BlockedAppEntity(
    @PrimaryKey val packageName: String,
    val isBlocked: Boolean,
    val lastExitTime: Long,
    val totalInterventions: Int,
    val totalTimeSaved: Long,
    val isStudyMode: Boolean = false,
    val studyStartTime: Long = 0L
)
