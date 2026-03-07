package com.example.shortstop.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ShortStopDao {
    
    @Query("SELECT * FROM user_stats WHERE id = 1")
    fun getUserStats(): Flow<UserStatsEntity?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateUserStats(stats: UserStatsEntity)
    
    @Query("SELECT * FROM blocked_apps WHERE isBlocked = 1")
    fun getBlockedApps(): Flow<List<BlockedAppEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlockedApp(app: BlockedAppEntity)
    
    @Query("DELETE FROM blocked_apps WHERE packageName = :packageName")
    suspend fun deleteBlockedApp(packageName: String)
    
    @Query("SELECT * FROM app_usage WHERE date >= :startDate ORDER BY date DESC")
    fun getUsageHistory(startDate: String): Flow<List<AppUsageEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppUsage(usage: AppUsageEntity)
    
    @Query("SELECT SUM(interventions) FROM app_usage WHERE packageName = :pkg AND date >= :startDate")
    suspend fun getTotalInterventions(pkg: String, startDate: String): Int?
    
    @Query("SELECT * FROM blocked_apps WHERE packageName = :packageName")
    suspend fun getBlockedApp(packageName: String): BlockedAppEntity?
    
    @Query("SELECT * FROM blocked_apps WHERE isStudyMode = 1")
    fun getStudyApps(): Flow<List<BlockedAppEntity>>
    
    @Query("UPDATE blocked_apps SET isStudyMode = :isStudy, studyStartTime = :startTime WHERE packageName = :packageName")
    suspend fun updateStudyMode(packageName: String, isStudy: Boolean, startTime: Long)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHourlyIntervention(intervention: HourlyInterventionEntity)
    
    @Query("SELECT * FROM hourly_interventions WHERE hourKey = :hourKey")
    suspend fun getHourlyIntervention(hourKey: String): HourlyInterventionEntity?
}
