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
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.kamaluddin.shortstop.database.BlockedAppEntity
import com.kamaluddin.shortstop.database.ShortStopRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ShortStopService : Service() {

    private lateinit var repository: ShortStopRepository
    private lateinit var windowManager: WindowManager
    private lateinit var usageStatsManager: UsageStatsManager

    private val handler = Handler(Looper.getMainLooper())
    private val serviceScope = CoroutineScope(
        Dispatchers.Main + SupervisorJob() + CoroutineExceptionHandler { _, t ->
            AppLogger.e(TAG, "Coroutine error: ${t.message}", t)
        }
    )

    private var overlayView: View? = null
    private var currentApp: String? = null
    private var appStartTime = 0L
    private var accumulatedTime = 0L
    private var interventionTriggered = false  // true while overlay is showing or being shown

    // ── All tracked runnables — cancelled together via cancelAllOverlayJobs() ─
    private var pollingRunnable: Runnable? = null
    private var monitoringRunnable: Runnable? = null
    private var overlayAutoHideRunnable: Runnable? = null   // hides overlay after 30s
    private var studyEndRunnable: Runnable? = null          // fires when study session ends

    // ── Per-app reward deadlines — checked on every poll tick ─────────────
    // No coroutine jobs needed — deadlines are persisted in DB

    private var blockedAppsList = emptyList<BlockedAppEntity>()
    private var studyAppsList = emptyList<BlockedAppEntity>()
    private val appLastExitTime = mutableMapOf<String, Long>()

    companion object {
        private val _isServiceRunning = MutableStateFlow(false)
        val isServiceRunning: StateFlow<Boolean> = _isServiceRunning

        private const val TAG = "ShortStop"
        private const val CHANNEL_ID = "shortstop_service"
        private const val NOTIFICATION_ID = 1

        const val TRIGGER_THRESHOLD_MS = 7L * 1000
        const val OVERLAY_DURATION_MS = 30L * 1000
        const val STUDY_MODE_DURATION_MS = 25L * 60 * 1000
        const val COOLDOWN_PERIOD_MS = 3L * 60 * 1000

        private const val POLL_INTERVAL_MS = 3L * 1000
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
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
        AppLogger.d(TAG, "Service started")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        super.onDestroy()
        _isServiceRunning.value = false
        stopPolling()
        handler.removeCallbacksAndMessages(null)
        serviceScope.cancel()
        removeOverlayView()
    }

    // ── Foreground app detection ─────────────────────────────────────────────

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
                checkPendingRewardDeadlines()
                handler.postDelayed(this, POLL_INTERVAL_MS)
            }
        }
        handler.post(pollingRunnable!!)
    }

    private fun stopPolling() {
        pollingRunnable?.let { handler.removeCallbacks(it) }
        pollingRunnable = null
    }

    // ── App switch logic ─────────────────────────────────────────────────────

    private fun onForegroundAppChanged(packageName: String) {
        if (packageName == this.packageName || packageName == "android") return
        if (packageName.startsWith("com.android.") && !packageName.contains("youtube")) return

        val now = System.currentTimeMillis()
        val blockedApps = blockedAppsList.map { it.packageName }.toSet()
        val studyApps = studyAppsList.map { it.packageName }.toSet()

        if (packageName != currentApp) {

            stopMonitoring()        // accumulates elapsed time into accumulatedTime
            cancelAllOverlayJobs()
            removeOverlayView()

            if (currentApp != null && blockedApps.contains(currentApp)) {
                val exitedApp = currentApp!!
                appLastExitTime[exitedApp] = now
                serviceScope.launch { repository.updateLastExitTime(exitedApp, now) }
            }

            // Reset accumulatedTime for the incoming app
            if (!blockedApps.contains(packageName)) {
                // Not a blocked app — full reset
                accumulatedTime = 0L
            } else {
                val timeSinceExit = now - (appLastExitTime[packageName] ?: 0L)
                // In cooldown: pre-fill threshold so overlay fires immediately on re-open
                accumulatedTime = if (timeSinceExit < COOLDOWN_PERIOD_MS) TRIGGER_THRESHOLD_MS else 0L
            }
            interventionTriggered = false

            currentApp = packageName
            appStartTime = now

            if (blockedApps.contains(packageName)) {
                if (studyApps.contains(packageName)) {
                    val studyApp = studyAppsList.find { it.packageName == packageName }
                    val studyStartTime = studyApp?.studyStartTime ?: 0L
                    if (studyStartTime > 0 && (now - studyStartTime) < STUDY_MODE_DURATION_MS) {
                        val remaining = STUDY_MODE_DURATION_MS - (now - studyStartTime)
                        val pkgSnapshot = packageName  // snapshot before delay
                        studyEndRunnable = Runnable {
                            studyEndRunnable = null
                            if (currentApp == pkgSnapshot) {
                                serviceScope.launch { repository.clearStudyMode(pkgSnapshot) }
                                triggerOverlay(pkgSnapshot)
                            }
                        }
                        handler.postDelayed(studyEndRunnable!!, remaining)
                        return
                    } else {
                        serviceScope.launch { repository.clearStudyMode(packageName) }
                    }
                }
                startMonitoring(packageName)
            }
        }
    }

    // ── Monitoring ───────────────────────────────────────────────────────────

    private var isMonitoring = false

    private fun startMonitoring(pkg: String) {
        if (isMonitoring) return
        isMonitoring = true
        interventionTriggered = false
        appStartTime = System.currentTimeMillis()
        val monitoredPkg = pkg  // snapshot — never changes for this session

        monitoringRunnable = object : Runnable {
            override fun run() {
                // Bail out if monitoring was stopped or pkg is no longer current
                if (!isMonitoring || currentApp != monitoredPkg) return
                // Bail out if an intervention is already in progress
                if (interventionTriggered) return

                val totalTime = accumulatedTime + (System.currentTimeMillis() - appStartTime)
                if (totalTime >= TRIGGER_THRESHOLD_MS && overlayView == null) {
                    interventionTriggered = true
                    stopMonitoring()   // stops the runnable, does NOT reset accumulatedTime
                    resetSessionTime() // explicit reset — next session starts fresh

                    val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
                    val currentHour = java.text.SimpleDateFormat("yyyy-MM-dd-HH", java.util.Locale.getDefault()).format(java.util.Date())
                    serviceScope.launch {
                        try {
                            repository.recordIntervention(monitoredPkg, today, totalTime)
                            repository.incrementHourlyIntervention(currentHour)
                        } catch (e: Exception) {
                            AppLogger.e(TAG, "Failed to record intervention")
                        }
                    }
                    triggerOverlay(monitoredPkg)
                    return
                }
                handler.postDelayed(this, POLL_INTERVAL_MS)
            }
        }
        handler.post(monitoringRunnable!!)
    }

    private fun stopMonitoring() {
        if (isMonitoring && appStartTime > 0) {
            accumulatedTime += System.currentTimeMillis() - appStartTime
        }
        isMonitoring = false
        interventionTriggered = false
        monitoringRunnable?.let { handler.removeCallbacks(it) }
        monitoringRunnable = null
    }

    /** Called when an intervention fires — resets accumulated time for the next session. */
    private fun resetSessionTime() {
        accumulatedTime = 0L
        appStartTime = System.currentTimeMillis()
    }

    // ── Overlay ──────────────────────────────────────────────────────────────

    /**
     * Single entry point for showing an overlay.
     * Cancels any existing overlay job before starting a new one.
     */
    private fun triggerOverlay(pkg: String) {
        cancelAllOverlayJobs()
        showOverlay()
        val pkgSnapshot = pkg  // snapshot before delay
        overlayAutoHideRunnable = Runnable {
            overlayAutoHideRunnable = null
            removeOverlayView()
            if (currentApp == pkgSnapshot) {
                interventionTriggered = false
                startMonitoring(pkgSnapshot)
            }
        }
        handler.postDelayed(overlayAutoHideRunnable!!, OVERLAY_DURATION_MS)
    }

    private fun showOverlay() {
        if (overlayView != null) return
        if (!canDrawOverlays(this)) {
            AppLogger.w(TAG, "Overlay permission not granted")
            return
        }
        serviceScope.launch(Dispatchers.IO) {
            val points = repository.dao.getUserStatsOnce()?.points ?: 0
            val canAffordExit = points >= 50
            withContext(Dispatchers.Main) {
                if (overlayView != null) return@withContext  // guard: another overlay appeared while we were reading DB
                try {
                    val params = WindowManager.LayoutParams(
                        WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                        PixelFormat.TRANSLUCENT
                    ).apply {
                        gravity = Gravity.CENTER
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            layoutInDisplayCutoutMode =
                                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                        }
                    }
                    overlayView = InterventionOverlay(
                        this@ShortStopService,
                        canAffordExit = canAffordExit,
                        onDismiss = { onOverlayDismissed() },
                        onEmergencyExit = { handleEmergencyExit() }
                    )
                    windowManager.addView(overlayView, params)
                } catch (e: Exception) {
                    AppLogger.e(TAG, "Failed to show overlay")
                    overlayView = null
                }
            }
        }
    }

    private fun onOverlayDismissed() {
        val pkgSnapshot = currentApp ?: return  // snapshot at dismiss time
        cancelAllOverlayJobs()
        removeOverlayView()
        interventionTriggered = false
        if (blockedAppsList.any { it.packageName == pkgSnapshot }) startMonitoring(pkgSnapshot)
    }

    /** Removes the view from WindowManager. Does NOT schedule anything. */
    private fun removeOverlayView() {
        overlayView?.let {
            try { windowManager.removeView(it) } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to remove overlay")
            }
            overlayView = null
        }
    }

    /** Cancels the auto-hide timer and study-end timer. Does NOT touch the view. */
    private fun cancelAllOverlayJobs() {
        overlayAutoHideRunnable?.let { handler.removeCallbacks(it) }
        overlayAutoHideRunnable = null
        studyEndRunnable?.let { handler.removeCallbacks(it) }
        studyEndRunnable = null
    }

    private fun handleEmergencyExit() {
        serviceScope.launch {
            try { repository.recordEmergencyExit() } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to apply emergency exit penalty")
            }
        }
        cancelAllOverlayJobs()
        removeOverlayView()
        interventionTriggered = false
        resetSessionTime()
        // Resume monitoring so the next 7s triggers a new overlay
        val pkg = currentApp ?: return
        if (blockedAppsList.any { it.packageName == pkg }) startMonitoring(pkg)
    }

    // ── Clean exit reward ────────────────────────────────────────────────────

    /** Called on every poll tick — checks all blocked apps for expired deadlines. */
    private fun checkPendingRewardDeadlines() {
        val now = System.currentTimeMillis()
        val activeApp = currentApp  // snapshot — user is currently in this app
        serviceScope.launch(Dispatchers.IO) {
            try {
                val apps = repository.dao.getBlockedAppsOnce()
                apps.filter {
                    it.cleanExitDeadline > 0L &&
                    now >= it.cleanExitDeadline &&
                    it.isBlocked &&
                    it.packageName != activeApp  // don't reward if user is still in the app
                }.forEach { app ->
                    repository.addPendingRewards(10)
                    repository.updateLastExitTime(app.packageName, 0L)
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to check reward deadlines")
            }
        }
    }

    // ── Notification ─────────────────────────────────────────────────────────

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
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify(NOTIFICATION_ID, buildNotification(blockedCount))
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
