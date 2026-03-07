package com.example.shortstop

import android.util.Log
import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import androidx.core.content.edit
import com.example.shortstop.database.ShortStopRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

class ShortStopService : AccessibilityService() {
    private var monitoringRunnable: Runnable? = null
    private var isMonitoring = false
    private lateinit var repository: ShortStopRepository
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob() + CoroutineExceptionHandler { _, throwable ->
        Log.e("ShortStop", "Coroutine error: ${throwable.message}", throwable)
    })
    
    companion object {
        var instance: ShortStopService? = null
        private val _isServiceRunning = MutableStateFlow(false)
        val isServiceRunning: StateFlow<Boolean> = _isServiceRunning
        
        private const val PREFS_NAME = "shortstop_prefs"
        private const val KEY_BLOCKED_APPS = "blocked_apps"
        private const val KEY_STUDY_APPS = "study_apps"
        private const val KEY_LAST_EXIT_TIME_PREFIX = "last_exit_time_"
        private const val KEY_BLUR_COUNT_PREFIX = "blur_count_"
        private const val KEY_STUDY_START_TIME_PREFIX = "study_start_time_"
        private const val KEY_LAST_INTERVENTION_DATE = "last_intervention_date"
        private const val KEY_SUCCESSFUL_STUDY_SESSIONS = "successful_study_sessions"
        private const val KEY_APP_INTERVENTION_COUNT_PREFIX = "app_intervention_count_"
        private const val KEY_APP_TIME_SAVED_PREFIX = "app_time_saved_"
        private const val KEY_APP_STUDY_SESSIONS_PREFIX = "app_study_sessions_"
        private const val KEY_TOTAL_POINTS_EARNED = "total_points_earned"
        private const val KEY_APP_DAILY_INTERVENTIONS_PREFIX = "app_daily_interventions_"
        private const val KEY_APP_DAILY_TIME_SAVED_PREFIX = "app_daily_time_saved_"
        private const val KEY_APP_DAILY_STUDY_SESSIONS_PREFIX = "app_daily_study_sessions_"

        private const val TRIGGER_THRESHOLD_MS = 7 * 1000L  // 7s - Quick interception
        private const val OVERLAY_DURATION_MS = 30 * 1000L   // 30s - Pattern interrupt
        private const val STUDY_MODE_DURATION_MS = 25 * 60 * 1000L  // 25min - Pomodoro standard
        private const val COOLDOWN_PERIOD_MS = 3 * 60 * 1000L  // 3min - Anti-cheat cooldown
    }

    private lateinit var windowManager: WindowManager
    private val handler = Handler(Looper.getMainLooper())

    private var overlayView: View? = null
    private var currentApp: String? = null
    private var appStartTime = 0L
    private var accumulatedTime = 0L
    private var blurRunnable: Runnable? = null
    private var isWaitingForExit = false
    private var blockedAppsList = emptyList<com.example.shortstop.database.BlockedAppEntity>()
    private var studyAppsList = emptyList<com.example.shortstop.database.BlockedAppEntity>()
    private val appLastExitTime = mutableMapOf<String, Long>()  // Track exit times for cooldown
    private fun startMonitoring(pkg: String) {
        if (isMonitoring) return
        
        isMonitoring = true
        appStartTime = System.currentTimeMillis()
        Log.d("ShortStop", "Started monitoring $pkg at ${appStartTime}, accumulated: ${accumulatedTime}ms")

        monitoringRunnable = object : Runnable {
            override fun run() {
                if (!isMonitoring) return
                
                val currentSessionTime = System.currentTimeMillis() - appStartTime
                val totalTime = accumulatedTime + currentSessionTime
                Log.d("ShortStop", "Monitoring $pkg - session: ${currentSessionTime}ms, total: ${totalTime}ms")

                if (totalTime >= TRIGGER_THRESHOLD_MS && overlayView == null) {
                    Log.d("ShortStop", "Showing overlay for $pkg (total time: ${totalTime}ms)")
                    
                    val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
                    val currentHour = java.text.SimpleDateFormat("yyyy-MM-dd-HH", java.util.Locale.getDefault()).format(java.util.Date())
                    
                    serviceScope.launch {
                        try {
                            repository.recordIntervention(pkg, today)
                            repository.incrementHourlyIntervention(currentHour)
                        } catch (e: Exception) {
                            Log.e("ShortStop", "Failed to record intervention: ${e.message}")
                        }
                    }
                    
                    if (currentApp == pkg) {
                        showOverlay()
                    }

                    // Auto-hide after 30 seconds and reset timer
                    blurRunnable = Runnable {
                        Log.d("ShortStop", "Pattern interrupt completed, resetting timer")
                        hideOverlay()
                    }
                    handler.postDelayed(blurRunnable!!, OVERLAY_DURATION_MS)
                }

                // Continue monitoring every second
                handler.postDelayed(this, 1000)
            }
        }

        handler.post(monitoringRunnable!!)
    }

    private fun stopMonitoring() {
        if (isMonitoring && appStartTime > 0) {
            // Add current session time to accumulated time
            val sessionTime = System.currentTimeMillis() - appStartTime
            accumulatedTime += sessionTime
            Log.d("ShortStop", "Pausing monitoring - session: ${sessionTime}ms, total accumulated: ${accumulatedTime}ms")
        }
        
        isMonitoring = false
        monitoringRunnable?.let { handler.removeCallbacks(it) }
        monitoringRunnable = null
    }


    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        _isServiceRunning.value = true
        repository = ShortStopRepository(this)
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        Log.d("ShortStop", "Service connected - instance set")
        
        serviceScope.launch(Dispatchers.IO) {
            repository.blockedApps.collect { apps ->
                blockedAppsList = apps
                studyAppsList = apps.filter { it.isStudyMode }
                Log.d("ShortStop", "Blocked apps updated: ${apps.size} apps")
            }
        }
        
        handler.postDelayed({
            Log.d("ShortStop", "Service fully initialized")
        }, 1000)
    }
    
    fun testOverlay() {
        Log.d("ShortStop", "Direct test overlay called")
        handler.post {
            showOverlay()
            handler.postDelayed({ hideOverlay() }, 5000)
        }
    }
    
    fun getCurrentApp(): String? {
        return currentApp
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val e = event ?: return
        val packageName = event.packageName?.toString() ?: return
        
        Log.d("ShortStop", "Event: ${e.eventType} from $packageName")

        if (e.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return
        }

        val now = System.currentTimeMillis()
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        
        val blockedApps = blockedAppsList.map { it.packageName }.toSet()
        val studyApps = studyAppsList.map { it.packageName }.toSet()
        
        Log.d("ShortStop", "Blocked apps: $blockedApps")
        Log.d("ShortStop", "Study apps: $studyApps")
        Log.d("ShortStop", "Current package: $packageName")
            
            if (packageName.startsWith("com.android.") && 
                !packageName.contains("youtube") ||
                packageName == this@ShortStopService.packageName ||
                packageName == "android") {
                return
            }

            if (packageName != currentApp) {
                Log.d("ShortStop", "App switched from '$currentApp' to '$packageName'")
                
                stopMonitoring()
                hideOverlay()
                cancelTimers()

                if (currentApp != null && blockedApps.contains(currentApp)) {
                    appLastExitTime[currentApp!!] = now
                    Log.d("ShortStop", "Recorded exit time for $currentApp - 3min cooldown starts")
                    
                    serviceScope.launch {
                        repository.updateLastExitTime(currentApp!!, now)
                    }
                    
                    handler.postDelayed({
                        serviceScope.launch {
                            checkAndRewardCleanExit(currentApp!!, prefs)
                        }
                    }, 10 * 60 * 1000L)
                }

                if (!blockedApps.contains(packageName)) {
                    Log.d("ShortStop", "Switching to non-blocked app, resetting accumulated time")
                    accumulatedTime = 0L
                    isWaitingForExit = false
                } else {
                    val lastExit = appLastExitTime[packageName] ?: 0L
                    val timeSinceExit = now - lastExit
                    
                    if (timeSinceExit < COOLDOWN_PERIOD_MS) {
                        Log.d("ShortStop", "Re-entry within 3min cooldown! Showing overlay instantly")
                        accumulatedTime = TRIGGER_THRESHOLD_MS
                    } else {
                        Log.d("ShortStop", "Cooldown expired, resetting timer")
                        accumulatedTime = 0L
                    }
                }

                currentApp = packageName
                appStartTime = now

                if (blockedApps.contains(packageName)) {
                    
                    if (studyApps.contains(packageName)) {
                        val studyApp = studyAppsList.find { it.packageName == packageName }
                        val studyStartTime = studyApp?.studyStartTime ?: 0L
                        val currentTime = System.currentTimeMillis()
                        
                        if (studyStartTime > 0 && (currentTime - studyStartTime) < STUDY_MODE_DURATION_MS) {
                            val remainingTime = STUDY_MODE_DURATION_MS - (currentTime - studyStartTime)
                            Log.d("ShortStop", "📚 Study app $packageName has ${remainingTime/1000} seconds of free time remaining")
                            
                            handler.postDelayed({
                                if (currentApp == packageName && blockedApps.contains(packageName)) {
                                    Log.d("ShortStop", "📚 Study time expired for $packageName, showing intervention")
                                    serviceScope.launch {
                                        repository.clearStudyMode(packageName)
                                    }
                                    showOverlay()
                                    handler.postDelayed({
                                        hideOverlay()
                                        startMonitoring(packageName)
                                    }, OVERLAY_DURATION_MS)
                                }
                            }, remainingTime)
                            
                            return
                        } else {
                            Log.d("ShortStop", "📚 Study app $packageName free time expired, now monitoring")
                            serviceScope.launch {
                                repository.clearStudyMode(packageName)
                            }
                        }
                    }
                    
                    Log.d("ShortStop", "✅ Starting monitoring for blocked app: $packageName")
                    startMonitoring(packageName)
                } else {
                    Log.d("ShortStop", "❌ App $packageName is not blocked")
                }
            }
    }

    override fun onInterrupt() {}

    private fun showOverlay() {
        if (overlayView != null) {
            Log.d("ShortStop", "Overlay already exists, skipping")
            return
        }

        try {
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as? android.media.AudioManager
            audioManager?.requestAudioFocus(
                null,
                android.media.AudioManager.STREAM_MUSIC,
                android.media.AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
            )

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.CENTER
            }

            overlayView = InterventionOverlay(this, {
                hideOverlay()
            }, {
                handleEmergencyExit()
            })

            windowManager.addView(overlayView, params)
            Log.d("ShortStop", "Overlay shown successfully")
        } catch (e: Exception) {
            Log.e("ShortStop", "Failed to show overlay: ${e.message}", e)
            overlayView = null
        }
    }

    private fun hideOverlay() {
        overlayView?.let {
            try {
                windowManager.removeView(it)
                Log.d("ShortStop", "Overlay hidden")
                
                isWaitingForExit = true
                
                handler.postDelayed({
                    if (isWaitingForExit && currentApp != null) {
                        serviceScope.launch {
                            try {
                                val blockedApps = blockedAppsList.map { it.packageName }.toSet()
                                
                                if (blockedApps.contains(currentApp)) {
                                    Log.d("ShortStop", "User still in blocked app, showing overlay again")
                                    showOverlay()
                                }
                            } catch (e: Exception) {
                                Log.e("ShortStop", "Error checking blocked apps: ${e.message}", e)
                            }
                        }
                    }
                }, 1000)
                
                val audioManager = getSystemService(Context.AUDIO_SERVICE) as? android.media.AudioManager
                audioManager?.abandonAudioFocus(null)
                
            } catch (e: Exception) {
                Log.e("ShortStop", "Failed to hide overlay: ${e.message}", e)
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
                    Log.d("ShortStop", "Emergency exit: -50 points penalty")
                }
            } catch (e: Exception) {
                Log.e("ShortStop", "Failed to apply emergency exit penalty: ${e.message}")
            }
        }
        hideOverlay()
        accumulatedTime = 0L
        isWaitingForExit = false
    }
    
    private fun checkAndRewardCleanExit(pkg: String, prefs: android.content.SharedPreferences) {
        serviceScope.launch {
            try {
                val app = repository.dao.getBlockedApp(pkg) ?: return@launch
                val exitTime = app.lastExitTime
                val now = System.currentTimeMillis()
                
                if (exitTime > 0 && (now - exitTime) >= 10 * 60 * 1000L) {
                    val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
                    val userStats = repository.userStats.first() ?: return@launch
                    
                    val dailyExitCount = if (userStats.lastRewardDate == today) userStats.dailyExitCount else 0
                    
                    val reward = when (dailyExitCount) {
                        0 -> 50
                        1 -> 25
                        2 -> 10
                        3 -> 5
                        else -> 1
                    }
                    
                    val currentHour = java.text.SimpleDateFormat("yyyy-MM-dd-HH", java.util.Locale.getDefault()).format(java.util.Date())
                    val hourlyInterventions = repository.getHourlyInterventionCount(currentHour)
                    val multiplier = if (hourlyInterventions >= 5) 0.5 else 1.0
                    val finalReward = (reward * multiplier).toInt()
                    
                    repository.claimReward(finalReward, today)
                    repository.updateLastExitTime(pkg, 0L)
                    
                    Log.d("ShortStop", "Clean exit reward: +$finalReward points for $pkg (exit #${dailyExitCount + 1})")
                }
            } catch (e: Exception) {
                Log.e("ShortStop", "Failed to reward clean exit: ${e.message}")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        _isServiceRunning.value = false
        handler.removeCallbacksAndMessages(null)
        serviceScope.cancel()
        hideOverlay()
        cancelTimers()
    }
}
