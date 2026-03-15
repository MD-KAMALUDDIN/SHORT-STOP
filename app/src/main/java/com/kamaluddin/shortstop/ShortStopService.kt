package com.kamaluddin.shortstop

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.kamaluddin.shortstop.database.BlockedAppEntity
import com.kamaluddin.shortstop.database.ShortStopRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first

class ShortStopService : Service() {

    private lateinit var repository: ShortStopRepository
    private lateinit var windowManager: WindowManager
    private lateinit var usageStatsManager: UsageStatsManager

    private val handler = Handler(Looper.getMainLooper())
    private val serviceScope = CoroutineScope(
        Dispatchers.Main + SupervisorJob() + CoroutineExceptionHandler { _, t ->
            Log.e(TAG, "Coroutine error: ${t.message}", t)
        }
    )

    private var overlayView: View? = null
    private var currentApp: String? = null
    private var appStartTime = 0L
    private var accumulatedTime = 0L
    private var isWaitingForExit = false
    private var blurRunnable: Runnable? = null
    private var pollingRunnable: Runnable? = null

    private var blockedAppsList = emptyList<BlockedAppEntity>()
    private var studyAppsList = emptyList<BlockedAppEntity>()
    private val appLastExitTime = mutableMapOf<String, Long>()

    companion object {
        var instance: ShortStopService? = null
        private val _isServiceRunning = MutableStateFlow(false)
        val isServiceRunning: StateFlow<Boolean> = _isServiceRunning

        private const val TAG = "ShortStop"
        private const val PREFS_NAME = "shortstop_prefs"
        private const val CHANNEL_ID = "shortstop_service"
        private const val NOTIFICATION_ID = 1

        const val TRIGGER_THRESHOLD_MS = 7 * 1000L
        const val OVERLAY_DURATION_MS = 30 * 1000L
        const val STUDY_MODE_DURATION_MS = 25 * 60 * 1000L
        const val COOLDOWN_PERIOD_MS = 3 * 60 * 1000L

        private const val POLL_INTERVAL_MS = 1000L
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        _isServiceRunning.value = true
        repository = ShortStopRepository(this)
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification(0))

        serviceScope.launch(Dispatchers.IO) {
            repository.blockedApps.collect { apps ->
                blockedAppsList = apps
                studyAppsList = apps.filter { it.isStudyMode }
                updateNotification(apps.size)
            }
        }

        startPolling()
        Log.d(TAG, "Service started")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        _isServiceRunning.value = false
        stopPolling()
        handler.removeCallbacksAndMessages(null)
        serviceScope.cancel()
        hideOverlay()
    }

    // ── Foreground app detection via UsageStatsManager ──────────────────────

    private fun getForegroundApp(): String? {
        val now = System.currentTimeMillis()
        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY, now - 5000, now
        ) ?: return null
        return stats.maxByOrNull { it.lastTimeUsed }
            ?.takeIf { it.lastTimeUsed > 0 }
            ?.packageName
    }

    private fun startPolling() {
        pollingRunnable = object : Runnable {
            override fun run() {
                val pkg = getForegroundApp()
                if (pkg != null) onForegroundAppChanged(pkg)
                handler.postDelayed(this, POLL_INTERVAL_MS)
            }
        }
        handler.post(pollingRunnable!!)
    }

    private fun stopPolling() {
        pollingRunnable?.let { handler.removeCallbacks(it) }
        pollingRunnable = null
    }

    // ── App switch logic (same as before, just moved from onAccessibilityEvent) ──

    private fun onForegroundAppChanged(packageName: String) {
        // Ignore system UI and our own app
        if (packageName == this.packageName || packageName == "android") return
        if (packageName.startsWith("com.android.") && !packageName.contains("youtube")) return

        val now = System.currentTimeMillis()
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val blockedApps = blockedAppsList.map { it.packageName }.toSet()
        val studyApps = studyAppsList.map { it.packageName }.toSet()

        if (packageName != currentApp) {
            Log.d(TAG, "App switched from '$currentApp' to '$packageName'")

            stopMonitoring()
            hideOverlay()
            cancelTimers()

            if (currentApp != null && blockedApps.contains(currentApp)) {
                appLastExitTime[currentApp!!] = now
                serviceScope.launch { repository.updateLastExitTime(currentApp!!, now) }
                handler.postDelayed({
                    serviceScope.launch { checkAndRewardCleanExit(currentApp!!, prefs) }
                }, 10 * 60 * 1000L)
            }

            accumulatedTime = if (!blockedApps.contains(packageName)) {
                isWaitingForExit = false
                0L
            } else {
                val timeSinceExit = now - (appLastExitTime[packageName] ?: 0L)
                if (timeSinceExit < COOLDOWN_PERIOD_MS) TRIGGER_THRESHOLD_MS else 0L
            }

            currentApp = packageName
            appStartTime = now

            if (blockedApps.contains(packageName)) {
                if (studyApps.contains(packageName)) {
                    val studyApp = studyAppsList.find { it.packageName == packageName }
                    val studyStartTime = studyApp?.studyStartTime ?: 0L
                    if (studyStartTime > 0 && (now - studyStartTime) < STUDY_MODE_DURATION_MS) {
                        val remaining = STUDY_MODE_DURATION_MS - (now - studyStartTime)
                        handler.postDelayed({
                            if (currentApp == packageName) {
                                serviceScope.launch { repository.clearStudyMode(packageName) }
                                showOverlay()
                                handler.postDelayed({
                                    hideOverlay()
                                    startMonitoring(packageName)
                                }, OVERLAY_DURATION_MS)
                            }
                        }, remaining)
                        return
                    } else {
                        serviceScope.launch { repository.clearStudyMode(packageName) }
                    }
                }
                startMonitoring(packageName)
            }
        } else {
            // Same app still in foreground — check if we need to trigger overlay
            if (blockedApps.contains(packageName) && overlayView == null) {
                val totalTime = accumulatedTime + (now - appStartTime)
                if (totalTime >= TRIGGER_THRESHOLD_MS) {
                    val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
                    val currentHour = java.text.SimpleDateFormat("yyyy-MM-dd-HH", java.util.Locale.getDefault()).format(java.util.Date())
                    serviceScope.launch {
                        try {
                            repository.recordIntervention(packageName, today)
                            repository.incrementHourlyIntervention(currentHour)
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to record intervention: ${e.message}")
                        }
                    }
                    showOverlay()
                    blurRunnable = Runnable { hideOverlay() }
                    handler.postDelayed(blurRunnable!!, OVERLAY_DURATION_MS)
                }
            }
        }
    }

    // ── Monitoring ───────────────────────────────────────────────────────────

    private var isMonitoring = false
    private var monitoringRunnable: Runnable? = null

    private fun startMonitoring(pkg: String) {
        if (isMonitoring) return
        isMonitoring = true
        appStartTime = System.currentTimeMillis()

        monitoringRunnable = object : Runnable {
            override fun run() {
                if (!isMonitoring) return
                val totalTime = accumulatedTime + (System.currentTimeMillis() - appStartTime)
                if (totalTime >= TRIGGER_THRESHOLD_MS && overlayView == null) {
                    val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
                    val currentHour = java.text.SimpleDateFormat("yyyy-MM-dd-HH", java.util.Locale.getDefault()).format(java.util.Date())
                    serviceScope.launch {
                        try {
                            repository.recordIntervention(pkg, today)
                            repository.incrementHourlyIntervention(currentHour)
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to record intervention: ${e.message}")
                        }
                    }
                    if (currentApp == pkg) showOverlay()
                    blurRunnable = Runnable { hideOverlay() }
                    handler.postDelayed(blurRunnable!!, OVERLAY_DURATION_MS)
                }
                handler.postDelayed(this, 1000)
            }
        }
        handler.post(monitoringRunnable!!)
    }

    private fun stopMonitoring() {
        if (isMonitoring && appStartTime > 0) {
            accumulatedTime += System.currentTimeMillis() - appStartTime
        }
        isMonitoring = false
        monitoringRunnable?.let { handler.removeCallbacks(it) }
        monitoringRunnable = null
    }

    // ── Overlay ──────────────────────────────────────────────────────────────

    private fun showOverlay() {
        if (overlayView != null) return
        try {
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            ).apply { gravity = Gravity.CENTER }

            overlayView = InterventionOverlay(this, { hideOverlay() }, { handleEmergencyExit() })
            windowManager.addView(overlayView, params)
            Log.d(TAG, "Overlay shown")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show overlay: ${e.message}", e)
            overlayView = null
        }
    }

    private fun hideOverlay() {
        overlayView?.let {
            try {
                windowManager.removeView(it)
                isWaitingForExit = true
                handler.postDelayed({
                    if (isWaitingForExit && currentApp != null) {
                        val blockedApps = blockedAppsList.map { a -> a.packageName }.toSet()
                        if (blockedApps.contains(currentApp)) showOverlay()
                    }
                }, 1000)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to hide overlay: ${e.message}", e)
            }
            overlayView = null
        }
    }

    private fun cancelTimers() {
        blurRunnable?.let { handler.removeCallbacks(it) }
        blurRunnable = null
    }

    private fun handleEmergencyExit() {
        serviceScope.launch {
            try {
                val userStats = repository.userStats.first()
                if (userStats != null && userStats.points >= 50) {
                    repository.updatePoints(userStats.points - 50)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to apply emergency exit penalty: ${e.message}")
            }
        }
        hideOverlay()
        accumulatedTime = 0L
        isWaitingForExit = false
    }

    private fun checkAndRewardCleanExit(pkg: String, @Suppress("UNUSED_PARAMETER") prefs: android.content.SharedPreferences) {
        serviceScope.launch {
            try {
                val app = repository.dao.getBlockedApp(pkg) ?: return@launch
                val exitTime = app.lastExitTime
                val now = System.currentTimeMillis()
                if (exitTime > 0 && (now - exitTime) >= 10 * 60 * 1000L) {
                    val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
                    val userStats = repository.userStats.first() ?: return@launch
                    val dailyExitCount = if (userStats.lastRewardDate == today) userStats.dailyExitCount else 0
                    val reward = when (dailyExitCount) { 0 -> 50; 1 -> 25; 2 -> 10; 3 -> 5; else -> 1 }
                    val currentHour = java.text.SimpleDateFormat("yyyy-MM-dd-HH", java.util.Locale.getDefault()).format(java.util.Date())
                    val multiplier = if (repository.getHourlyInterventionCount(currentHour) >= 5) 0.5 else 1.0
                    repository.claimReward((reward * multiplier).toInt(), today)
                    repository.updateLastExitTime(pkg, 0L)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to reward clean exit: ${e.message}")
            }
        }
    }

    fun getCurrentApp(): String? = currentApp

    // ── Notification (required for foreground service) ───────────────────────

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "ShortStop Monitor", NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Monitors app usage to trigger interventions" }
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }

    private fun updateNotification(blockedCount: Int) {
        val notification = buildNotification(blockedCount)
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(blockedCount: Int): android.app.Notification {
        val (title, text) = if (blockedCount > 0)
            "ShortStop is active" to "Protecting you from $blockedCount app${if (blockedCount > 1) "s" else ""} 🛡️"
        else
            "ShortStop is ready" to "Add apps to block to get started"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    this, 0,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            .build()
    }
}
