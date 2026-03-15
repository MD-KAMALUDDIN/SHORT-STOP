package com.kamaluddin.shortstop.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

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
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .build()
                INSTANCE = instance
                instance
            }
        }
        // No structural changes between these versions — safe no-op migrations
        // that preserve all existing user data.
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {}
        }
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {}
        }
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {}
        }
    }
}
