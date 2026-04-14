package com.kamaluddin.shortstop.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ShortStopDaoTest {

    private lateinit var db: ShortStopDatabase
    private lateinit var dao: ShortStopDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // In-memory DB — no SQLCipher passphrase needed for tests
        db = Room.inMemoryDatabaseBuilder(context, ShortStopDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.dao()
    }

    @After
    fun closeDb() = db.close()

    // ── UserStats ─────────────────────────────────────────────────────────────

    @Test
    fun insertAndReadUserStats() = runTest {
        val stats = UserStatsEntity(
            points = 100, currentStreak = 3, lastInterventionDate = "2025-07-14",
            totalInterventions = 5, totalTimeSaved = 50000L,
            successfulStudySessions = 2, totalPointsEarned = 150
        )
        dao.updateUserStats(stats)
        val loaded = dao.getUserStats().first()
        assertNotNull(loaded)
        assertEquals(100, loaded!!.points)
        assertEquals(3, loaded.currentStreak)
        assertEquals(5, loaded.totalInterventions)
    }

    @Test
    fun updatePendingRewards_reflectedInFlow() = runTest {
        dao.updateUserStats(UserStatsEntity(
            points = 50, currentStreak = 0, lastInterventionDate = "",
            totalInterventions = 0, totalTimeSaved = 0,
            successfulStudySessions = 0, totalPointsEarned = 0
        ))
        dao.updatePendingRewards(30)
        val loaded = dao.getUserStats().first()
        assertEquals(30, loaded!!.pendingRewards)
    }

    // ── BlockedApps ───────────────────────────────────────────────────────────

    @Test
    fun insertAndQueryBlockedApp() = runTest {
        val app = BlockedAppEntity("com.example.tiktok", true, 0L, 0, 0L)
        dao.insertBlockedApp(app)
        val list = dao.getBlockedApps().first()
        assertEquals(1, list.size)
        assertEquals("com.example.tiktok", list[0].packageName)
    }

    @Test
    fun deleteBlockedApp_removesFromList() = runTest {
        dao.insertBlockedApp(BlockedAppEntity("com.example.tiktok", true, 0L, 0, 0L))
        dao.deleteBlockedApp("com.example.tiktok")
        val list = dao.getBlockedApps().first()
        assertTrue(list.isEmpty())
    }

    @Test
    fun getBlockedApp_returnsNullForUnknown() = runTest {
        val result = dao.getBlockedApp("com.example.unknown")
        assertNull(result)
    }

    @Test
    fun updateStudyMode_setsFieldsCorrectly() = runTest {
        dao.insertBlockedApp(BlockedAppEntity("com.example.instagram", true, 0L, 0, 0L))
        dao.updateStudyMode("com.example.instagram", true, 1234567890L)
        val app = dao.getBlockedApp("com.example.instagram")
        assertNotNull(app)
        assertTrue(app!!.isStudyMode)
        assertEquals(1234567890L, app.studyStartTime)
    }

    @Test
    fun getStudyApps_onlyReturnsStudyModeApps() = runTest {
        dao.insertBlockedApp(BlockedAppEntity("com.example.a", true, 0L, 0, 0L))
        dao.insertBlockedApp(BlockedAppEntity("com.example.b", true, 0L, 0, 0L))
        dao.updateStudyMode("com.example.a", true, 999L)
        val studyApps = dao.getStudyApps().first()
        assertEquals(1, studyApps.size)
        assertEquals("com.example.a", studyApps[0].packageName)
    }

    // ── AppUsage ──────────────────────────────────────────────────────────────

    @Test
    fun insertAppUsage_queryableByDate() = runTest {
        dao.insertAppUsage(AppUsageEntity(
            packageName = "com.example.tiktok",
            date = "2025-07-14",
            interventions = 3,
            timeSaved = 90000L,
            studySessions = 1
        ))
        val history = dao.getUsageHistory("2025-07-01").first()
        assertEquals(1, history.size)
        assertEquals(3, history[0].interventions)
    }

    // ── HourlyInterventions ───────────────────────────────────────────────────

    @Test
    fun insertAndReadHourlyIntervention() = runTest {
        dao.insertHourlyIntervention(HourlyInterventionEntity("2025-07-14-09", 4))
        val result = dao.getHourlyIntervention("2025-07-14-09")
        assertNotNull(result)
        assertEquals(4, result!!.interventionCount)
    }

    @Test
    fun getHourlyIntervention_returnsNullForMissingKey() = runTest {
        val result = dao.getHourlyIntervention("2025-07-14-23")
        assertNull(result)
    }

    @Test
    fun replaceHourlyIntervention_updatesCount() = runTest {
        dao.insertHourlyIntervention(HourlyInterventionEntity("2025-07-14-10", 2))
        dao.insertHourlyIntervention(HourlyInterventionEntity("2025-07-14-10", 7))
        val result = dao.getHourlyIntervention("2025-07-14-10")
        assertEquals(7, result!!.interventionCount)
    }
}
