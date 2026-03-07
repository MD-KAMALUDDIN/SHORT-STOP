package com.example.shortstop.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "hourly_interventions",
    indices = [Index(value = ["hourKey"], unique = true)]
)
data class HourlyInterventionEntity(
    @PrimaryKey val hourKey: String,
    val interventionCount: Int
)
