package com.kamaluddin.shortstop.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "app_usage",
    indices = [
        Index(value = ["packageName"]),
        Index(value = ["date"])
    ]
)
data class AppUsageEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val packageName: String,
    val date: String,
    val interventions: Int,
    val timeSaved: Long,
    val studySessions: Int
)
