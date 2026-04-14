package com.kamaluddin.shortstop.database

import android.content.Context
import com.google.gson.Gson
import com.kamaluddin.shortstop.SessionGuard
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.OutputStream

class ShortStopRepository(private val context: Context) {

    private val database = ShortStopDatabase.getDatabase(context)
    val dao = database.dao()
    private val pointsMutex = Mutex()
    
    val userStats: Flow<UserStatsEntity?> = dao.getUserStats()
    val blockedApps: Flow<List<BlockedAppEntity>> = dao.getBlockedApps()
    val studyApps: Flow<List<BlockedAppEntity>> = dao.getStudyApps()
    
    suspend fun updatePoints(points: Int) {
        if (!SessionGuard.isAuthorized(context)) return
        pointsMutex.withLock {
            val current = dao.getUserStats().firstOrNull() ?: UserStatsEntity(
                points = 0, currentStreak = 0, lastInterventionDate = "",
                totalInterventions = 0, totalTimeSaved = 0, successfulStudySessions = 0, totalPointsEarned = 0
            )
            dao.updateUserStats(current.copy(points = current.points + points))
        }
    }

    suspend fun deductPoints(amount: Int) {
        if (!SessionGuard.isAuthorized(context)) return
        pointsMutex.withLock {
            val current = dao.getUserStats().firstOrNull() ?: return
            val newPoints = (current.points - amount).coerceAtLeast(0)
            dao.updateUserStats(current.copy(points = newPoints))
        }
    }
    
    suspend fun recordIntervention(packageName: String, date: String) {
        if (!SessionGuard.isAuthorized(context)) return
        val current = dao.getUserStats().firstOrNull() ?: UserStatsEntity(
            points = 0, currentStreak = 0, lastInterventionDate = "",
            totalInterventions = 0, totalTimeSaved = 0, successfulStudySessions = 0, totalPointsEarned = 0
        )
        
        val newStreak = if (current.lastInterventionDate == date) {
            current.currentStreak
        } else {
            val yesterday = getYesterdayDate()
            if (current.lastInterventionDate == yesterday) current.currentStreak + 1 else 1
        }
        
        dao.updateUserStats(current.copy(
            totalInterventions = current.totalInterventions + 1,
            totalTimeSaved = current.totalTimeSaved + 10000,
            lastInterventionDate = date,
            currentStreak = newStreak
        ))
        
        val usage = AppUsageEntity(
            packageName = packageName,
            date = date,
            interventions = 1,
            timeSaved = 10000,
            studySessions = 0
        )
        dao.insertAppUsage(usage)
    }
    
    suspend fun toggleBlockedApp(packageName: String, isBlocked: Boolean) {
        if (!SessionGuard.isAuthorized(context)) return
        if (isBlocked) {
            dao.insertBlockedApp(BlockedAppEntity(packageName, true, 0, 0, 0))
        } else {
            dao.deleteBlockedApp(packageName)
        }
    }
    
    suspend fun updateLastExitTime(packageName: String, exitTime: Long) {
        val app = dao.getBlockedApp(packageName) ?: return
        dao.insertBlockedApp(app.copy(lastExitTime = exitTime))
    }
    
    suspend fun setStudyMode(packageName: String, startTime: Long) {
        if (!SessionGuard.isAuthorized(context)) return
        dao.updateStudyMode(packageName, true, startTime)
    }
    
    suspend fun clearStudyMode(packageName: String) {
        if (!SessionGuard.isAuthorized(context)) return
        dao.updateStudyMode(packageName, false, 0L)
        val current = dao.getUserStats().firstOrNull() ?: return
        dao.updateUserStats(current.copy(
            successfulStudySessions = current.successfulStudySessions + 1,
            totalPointsEarned = current.totalPointsEarned + 5
        ))
    }

    suspend fun addPendingRewards(points: Int) {
        if (!SessionGuard.isAuthorized(context)) return

        val current = dao.getUserStats().firstOrNull() ?: return

        dao.updateUserStats(
            current.copy(
                pendingRewards = current.pendingRewards + points
            )
        )
    }
    suspend fun claimAllRewards() {
        if (!SessionGuard.isAuthorized(context)) return
        pointsMutex.withLock {
            val current = dao.getUserStats().firstOrNull() ?: return
            if (current.pendingRewards <= 0) return
            dao.updateUserStats(current.copy(
                points = current.points + current.pendingRewards,
                totalPointsEarned = current.totalPointsEarned + current.pendingRewards,
                pendingRewards = 0
            ))
        }
    }

    suspend fun claimReward(points: Int, date: String) {
        if (!SessionGuard.isAuthorized(context)) return
        val current = dao.getUserStats().firstOrNull() ?: UserStatsEntity(
            points = 0, currentStreak = 0, lastInterventionDate = "",
            totalInterventions = 0, totalTimeSaved = 0, successfulStudySessions = 0,
            totalPointsEarned = 0, dailyExitCount = 0, lastRewardDate = ""
        )

        val newExitCount = if (current.lastRewardDate == date) current.dailyExitCount + 1 else 1

        dao.updateUserStats(current.copy(
            points = current.points + points,
            totalPointsEarned = current.totalPointsEarned + points,
            dailyExitCount = newExitCount,
            lastRewardDate = date
        ))
    }
    
    suspend fun incrementHourlyIntervention(hourKey: String) {
        val current = dao.getHourlyIntervention(hourKey)
        val newCount = (current?.interventionCount ?: 0) + 1
        dao.insertHourlyIntervention(HourlyInterventionEntity(hourKey, newCount))
    }
    
    suspend fun getHourlyInterventionCount(hourKey: String): Int {
        return dao.getHourlyIntervention(hourKey)?.interventionCount ?: 0
    }
    
    private fun getYesterdayDate(): String {
        return java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(java.util.Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000L))
    }
    
    suspend fun exportData(outputStream: OutputStream) {
        if (!SessionGuard.isAuthorized(context)) return
        val data = mapOf(
            "userStats" to dao.getUserStats().firstOrNull(),
            "blockedApps" to dao.getBlockedApps().firstOrNull(),
            "exportDate" to System.currentTimeMillis()
        )
        val json = Gson().toJson(data)
        outputStream.write(json.toByteArray())
        outputStream.close()
    }
}
