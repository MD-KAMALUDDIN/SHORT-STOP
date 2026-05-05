package com.kamaluddin.shortstop.database

import android.content.Context
import com.google.gson.Gson
import com.kamaluddin.shortstop.SessionGuard
import kotlinx.coroutines.flow.Flow
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

    // ── Single source of truth: always returns a valid row ───────────────────
    private suspend fun getOrCreateUserStats(): UserStatsEntity {
        return dao.getUserStatsOnce() ?: UserStatsEntity()
            .also { dao.updateUserStats(it) }
    }

    // ── Points ───────────────────────────────────────────────────────────────
    suspend fun updatePoints(points: Int) {
        pointsMutex.withLock {
            val current = getOrCreateUserStats()
            dao.updateUserStats(current.copy(points = current.points + points))
        }
    }

    suspend fun deductPoints(amount: Int) {
        pointsMutex.withLock {
            val current = getOrCreateUserStats()
            dao.updateUserStats(current.copy(points = (current.points - amount).coerceAtLeast(0)))
        }
    }

    suspend fun recordEmergencyExit() {
        pointsMutex.withLock {
            val current = getOrCreateUserStats()
            dao.updateUserStats(
                current.copy(
                    points = (current.points - 50).coerceAtLeast(0),
                    totalEmergencyExits = current.totalEmergencyExits + 1
                )
            )
        }
    }

    // ── Interventions ────────────────────────────────────────────────────────
    suspend fun recordIntervention(packageName: String, date: String, elapsedMs: Long = 10000L) {
        pointsMutex.withLock {
            val current = getOrCreateUserStats()

            val newStreak = if (current.lastInterventionDate == date) {
                current.currentStreak
            } else {
                val yesterday = getYesterdayDate()
                if (current.lastInterventionDate == yesterday) current.currentStreak + 1 else 1
            }

            val isNewStreakDay = current.lastInterventionDate != date

            dao.updateUserStats(current.copy(
                totalInterventions = current.totalInterventions + 1,
                totalTimeSaved = current.totalTimeSaved + elapsedMs,
                lastInterventionDate = date,
                currentStreak = newStreak,
                pendingRewards = if (isNewStreakDay) current.pendingRewards + 10 else current.pendingRewards
            ))

            dao.insertAppUsage(AppUsageEntity(
                packageName = packageName,
                date = date,
                interventions = 1,
                timeSaved = elapsedMs,
                studySessions = 0
            ))
        }
    }

    // ── Blocked apps ─────────────────────────────────────────────────────────
    suspend fun toggleBlockedApp(packageName: String, isBlocked: Boolean) {
        if (isBlocked) {
            dao.insertBlockedApp(BlockedAppEntity(packageName, true, 0))
        } else {
            // Clear any pending reward deadline before removing
            dao.updateCleanExitDeadline(packageName, 0L)
            dao.deleteBlockedApp(packageName)
        }
    }

    suspend fun updateLastExitTime(packageName: String, exitTime: Long) {
        val app = dao.getBlockedApp(packageName) ?: return
        val deadline = if (exitTime > 0L) exitTime + 10L * 60 * 1000 else 0L
        dao.insertBlockedApp(app.copy(lastExitTime = exitTime, cleanExitDeadline = deadline))
    }

    // ── Study mode ───────────────────────────────────────────────────────────
    suspend fun setStudyMode(packageName: String, startTime: Long) {
        dao.updateStudyMode(packageName, true, startTime)
    }

    suspend fun clearStudyMode(packageName: String) {
        dao.updateStudyMode(packageName, false, 0L)
        pointsMutex.withLock {
            val current = getOrCreateUserStats()
            dao.updateUserStats(current.copy(
                successfulStudySessions = current.successfulStudySessions + 1,
                points = current.points + 5,
                totalPointsEarned = current.totalPointsEarned + 5
            ))
        }
    }

    // ── Rewards ──────────────────────────────────────────────────────────────
    suspend fun addPendingRewards(points: Int) {
        dao.addToPendingRewards(points)
    }

    suspend fun claimAllRewards() {
        dao.claimPendingRewards()
    }

    // ── Hourly interventions ─────────────────────────────────────────────────
    suspend fun incrementHourlyIntervention(hourKey: String) {
        val current = dao.getHourlyIntervention(hourKey)
        val newCount = (current?.interventionCount ?: 0) + 1
        dao.insertHourlyIntervention(HourlyInterventionEntity(hourKey, newCount))
    }

    // ── Export (critical — requires authorization) ───────────────────────────
    suspend fun exportData(outputStream: OutputStream) {
        if (!SessionGuard.isAuthorized(context)) return
        val data = mapOf(
            "userStats" to getOrCreateUserStats(),
            "blockedApps" to dao.getBlockedAppsOnce(),
            "exportDate" to System.currentTimeMillis()
        )
        val json = Gson().toJson(data)
        outputStream.write(json.toByteArray())
        outputStream.flush()
    }

    // ── Helpers ──────────────────────────────────────────────────────────────
    private fun getYesterdayDate(): String {
        return java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(java.util.Date(System.currentTimeMillis() - 24L * 60 * 60 * 1000))
    }
}
