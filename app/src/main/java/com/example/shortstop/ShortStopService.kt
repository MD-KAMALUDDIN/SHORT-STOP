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
import kotlinx.coroutines.*
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

class ShortStopService : AccessibilityService() {
    private var monitoringRunnable: Runnable? = null
    private var isMonitoring = false
    
    companion object {
        var instance: ShortStopService? = null
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

        private const val FIVE_MINUTES = 15 * 1000L
        private const val TEN_SECONDS = 10 * 1000L
        private const val ONE_MINUTE = 60 * 1000L
    }

    private lateinit var windowManager: WindowManager
    private val handler = Handler(Looper.getMainLooper())

    private var overlayView: View? = null
    private var currentApp: String? = null
    private var appStartTime = 0L
    private var accumulatedTime = 0L
    private var blurRunnable: Runnable? = null
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

                if (totalTime >= FIVE_MINUTES && overlayView == null) {
                    Log.d("ShortStop", "Showing overlay for $pkg (total time: ${totalTime}ms)")
                    
                    // Update usage statistics
                    val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                    val currentInterventions = prefs.getInt("total_interventions", 0)
                    val currentTimeSaved = prefs.getLong("total_time_saved", 0L)
                    val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
                    
                    // Update app-specific statistics
                    val appInterventionKey = KEY_APP_INTERVENTION_COUNT_PREFIX + pkg
                    val appTimeSavedKey = KEY_APP_TIME_SAVED_PREFIX + pkg
                    val currentAppInterventions = prefs.getInt(appInterventionKey, 0)
                    val currentAppTimeSaved = prefs.getLong(appTimeSavedKey, 0L)
                    val totalPointsEarned = prefs.getInt(KEY_TOTAL_POINTS_EARNED, 0)
                    
                    // Daily statistics tracking
                    val dailyInterventionKey = "daily_interventions_$today"
                    val dailyTimeSavedKey = "daily_time_saved_$today"
                    val appDailyInterventionKey = "${KEY_APP_DAILY_INTERVENTIONS_PREFIX}${pkg}_$today"
                    val appDailyTimeSavedKey = "${KEY_APP_DAILY_TIME_SAVED_PREFIX}${pkg}_$today"
                    val currentHour = java.text.SimpleDateFormat("yyyy-MM-dd-HH", java.util.Locale.getDefault()).format(java.util.Date())
                    val hourlyInterventionsKey = "hourly_interventions_$currentHour"
                    
                    val currentDailyInterventions = prefs.getInt(dailyInterventionKey, 0)
                    val currentDailyTimeSaved = prefs.getLong(dailyTimeSavedKey, 0L)
                    val currentAppDailyInterventions = prefs.getInt(appDailyInterventionKey, 0)
                    val currentAppDailyTimeSaved = prefs.getLong(appDailyTimeSavedKey, 0L)
                    val currentHourlyInterventions = prefs.getInt(hourlyInterventionsKey, 0)
                    
                    prefs.edit {
                        putInt("total_interventions", currentInterventions + 1)
                        putLong("total_time_saved", currentTimeSaved + (10 * 1000L))
                        putString(KEY_LAST_INTERVENTION_DATE, today) // Track intervention date
                        // App-specific tracking
                        putInt(appInterventionKey, currentAppInterventions + 1)
                        putLong(appTimeSavedKey, currentAppTimeSaved + (10 * 1000L))
                        // Track estimated points earned from this intervention
                        putInt(KEY_TOTAL_POINTS_EARNED, totalPointsEarned + 10) // Estimate 10 points per intervention
                        // Daily statistics
                        putInt(dailyInterventionKey, currentDailyInterventions + 1)
                        putLong(dailyTimeSavedKey, currentDailyTimeSaved + (10 * 1000L))
                        putInt(appDailyInterventionKey, currentAppDailyInterventions + 1)
                        putLong(appDailyTimeSavedKey, currentAppDailyTimeSaved + (10 * 1000L))
                        // Hourly intervention tracking for penalty system
                        putInt(hourlyInterventionsKey, currentHourlyInterventions + 1)
                    }
                    
                    // Show overlay if user is in the blocked app
                    if (currentApp == pkg) {
                        showOverlay()
                    }

                    // Auto-hide after 10 seconds and reset timer
                    blurRunnable = Runnable {
                        Log.d("ShortStop", "Blur completed, resetting timer")
                        hideOverlay()
                    }
                    handler.postDelayed(blurRunnable!!, TEN_SECONDS)
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
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        Log.d("ShortStop", "Service connected - instance set")
        
        // Force a small delay to ensure proper initialization
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

        // Only process window state changes for app switching
        if (e.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return
        }

        val now = System.currentTimeMillis()
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val blockedApps = prefs.getStringSet(KEY_BLOCKED_APPS, emptySet()) ?: emptySet()
        val studyApps = prefs.getStringSet(KEY_STUDY_APPS, emptySet()) ?: emptySet()
        
        Log.d("ShortStop", "Blocked apps: $blockedApps")
        Log.d("ShortStop", "Study apps: $studyApps")
        Log.d("ShortStop", "Current package: $packageName")
        
        // Ignore system packages but allow YouTube variants
        if (packageName.startsWith("com.android.") && 
            !packageName.contains("youtube") ||
            packageName == this.packageName ||
            packageName == "android") {
            return
        }

        // App switched
        if (packageName != currentApp) {
            Log.d("ShortStop", "App switched from '$currentApp' to '$packageName'")
            
            // Clean up previous monitoring and reset everything
            stopMonitoring()
            hideOverlay()
            cancelTimers()

            // Save exit time for previous app
            if (currentApp != null && blockedApps.contains(currentApp)) {
                prefs.edit {
                    putLong(KEY_LAST_EXIT_TIME_PREFIX + currentApp!!, now)
                }
            }

            // Reset accumulated time when switching apps
            if (packageName != currentApp) {
                if (blockedApps.contains(packageName)) {
                    // Reset timer when switching to blocked app
                    accumulatedTime = 0L
                } else {
                    // Reset when switching to non-blocked app
                    accumulatedTime = 0L
                }
            }

            // Set new current app
            currentApp = packageName
            appStartTime = now

            // Start monitoring if new app is blocked
            if (blockedApps.contains(packageName)) {
                
                // Check if this is a study app with free time remaining
                if (studyApps.contains(packageName)) {
                    val studyStartTimeKey = KEY_STUDY_START_TIME_PREFIX + packageName
                    val studyStartTime = prefs.getLong(studyStartTimeKey, 0L)
                    val currentTime = System.currentTimeMillis()
                    
                    if (studyStartTime > 0 && (currentTime - studyStartTime) < ONE_MINUTE) {
                        val remainingTime = ONE_MINUTE - (currentTime - studyStartTime)
                        Log.d("ShortStop", "📚 Study app $packageName has ${remainingTime/1000} seconds of free time remaining")
                        
                        // Schedule intervention to show immediately when free time expires
                        handler.postDelayed({
                            if (currentApp == packageName && blockedApps.contains(packageName)) {
                                Log.d("ShortStop", "📚 Study time expired for $packageName, showing intervention")
                                showOverlay()
                                // Auto-hide after 10 seconds and start normal monitoring
                                handler.postDelayed({
                                    hideOverlay()
                                    startMonitoring(packageName)
                                }, TEN_SECONDS)
                            }
                        }, remainingTime)
                        
                        return // Skip monitoring during free time
                    } else {
                        Log.d("ShortStop", "📚 Study app $packageName free time expired, now monitoring")
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
            // Pause audio/video
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
            audioManager.requestAudioFocus(
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

            overlayView = InterventionOverlay(this) {
                hideOverlay()
            }

            windowManager.addView(overlayView, params)
            Log.d("ShortStop", "Overlay shown successfully")
        } catch (e: Exception) {
            Log.e("ShortStop", "Failed to show overlay: ${e.message}")
            overlayView = null
        }
    }

    private fun hideOverlay() {
        overlayView?.let {
            try {
                windowManager.removeView(it)
                Log.d("ShortStop", "Overlay hidden and timer reset")
                
                // Reset accumulated time when blur completes
                accumulatedTime = 0L
                appStartTime = System.currentTimeMillis()
                
                // Release audio focus
                val audioManager = getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
                audioManager.abandonAudioFocus(null)
                
            } catch (e: Exception) {
                Log.e("ShortStop", "Failed to hide overlay: ${e.message}")
            }
            overlayView = null
        }
    }

    private fun cancelTimers() {
        blurRunnable?.let { handler.removeCallbacks(it) }
        blurRunnable = null
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        hideOverlay()
        cancelTimers()
    }
}
