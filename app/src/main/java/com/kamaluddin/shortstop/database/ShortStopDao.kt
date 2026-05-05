package com.kamaluddin.shortstop.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ShortStopDao {
    
    @Query("SELECT * FROM user_stats WHERE id = 1")
    fun getUserStats(): Flow<UserStatsEntity?>

    @Query("SELECT * FROM user_stats WHERE id = 1")
    suspend fun getUserStatsOnce(): UserStatsEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateUserStats(stats: UserStatsEntity)
    
    @Query("SELECT * FROM blocked_apps WHERE isBlocked = 1")
    fun getBlockedApps(): Flow<List<BlockedAppEntity>>

    @Query("SELECT * FROM blocked_apps WHERE isBlocked = 1")
    suspend fun getBlockedAppsOnce(): List<BlockedAppEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlockedApp(app: BlockedAppEntity)
    
    @Query("DELETE FROM blocked_apps WHERE packageName = :packageName")
    suspend fun deleteBlockedApp(packageName: String)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppUsage(usage: AppUsageEntity)
    
    @Query("SELECT * FROM blocked_apps WHERE packageName = :packageName")
    suspend fun getBlockedApp(packageName: String): BlockedAppEntity?
    
    @Query("SELECT * FROM blocked_apps WHERE isStudyMode = 1")
    fun getStudyApps(): Flow<List<BlockedAppEntity>>
    
    @Query("UPDATE blocked_apps SET isStudyMode = :isStudy, studyStartTime = :startTime WHERE packageName = :packageName")
    suspend fun updateStudyMode(packageName: String, isStudy: Boolean, startTime: Long)

    @Query("UPDATE blocked_apps SET cleanExitDeadline = :deadline WHERE packageName = :packageName")
    suspend fun updateCleanExitDeadline(packageName: String, deadline: Long)
    
    @Query("UPDATE user_stats SET pendingRewards = pendingRewards + :amount WHERE id = 1")
    suspend fun addToPendingRewards(amount: Int)

    @Query("UPDATE user_stats SET points = points + pendingRewards, totalPointsEarned = totalPointsEarned + pendingRewards, pendingRewards = 0 WHERE id = 1 AND pendingRewards > 0")
    suspend fun claimPendingRewards()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHourlyIntervention(intervention: HourlyInterventionEntity)

    @Query("SELECT * FROM hourly_interventions WHERE hourKey = :hourKey")
    suspend fun getHourlyIntervention(hourKey: String): HourlyInterventionEntity?
}
