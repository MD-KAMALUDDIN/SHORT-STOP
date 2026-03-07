package com.example.shortstop.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [UserStatsEntity::class, BlockedAppEntity::class, AppUsageEntity::class, HourlyInterventionEntity::class],
    version = 4,
    exportSchema = false
)
abstract class ShortStopDatabase : RoomDatabase() {
    
    abstract fun dao(): ShortStopDao
    
    companion object {
        @Volatile
        private var INSTANCE: ShortStopDatabase? = null
        
        fun getDatabase(context: Context): ShortStopDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ShortStopDatabase::class.java,
                    "shortstop_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
