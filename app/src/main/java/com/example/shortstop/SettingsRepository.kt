package com.example.shortstop

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onStart

class SettingsRepository(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("shortstop_prefs", Context.MODE_PRIVATE)

    companion object {
        const val TRIGGER_TIME_SECONDS = "trigger_time_seconds" // key used in service? service uses hardcoded 15s? No, service uses PREFS constants.
        // Service constants:
        // private const val FIVE_MINUTES = 15 * 1000L // Hardcoded in service, NOT from prefs.
        // Wait, does service read settings?
        // Let's look at ShortStopService again.
        
        // ShortStopService.kt:
        // val triggerTime = prefs.getLong("trigger_time", FIVE_MINUTES) 
        // val pauseDuration = prefs.getLong("pause_duration", TEN_SECONDS)
        
        // So keys are "trigger_time" and "pause_duration".
        const val KEY_TRIGGER_TIME = "trigger_time"
        const val KEY_PAUSE_DURATION = "pause_duration"
        const val KEY_TARGET_PACKAGES = "target_packages" // Service: checks if packageName in set?
        // Service: val blockedApps = prefs.getStringSet("blocked_apps", emptySet())
        const val KEY_BLOCKED_APPS = "blocked_apps"
        
        const val KEY_POINTS = "points"
        const val KEY_STUDY_MODE = "is_study_mode"
        const val KEY_COOLDOWN_END = "cooldown_end_time"
        
        // Keys from MainActivity for stats
        private const val KEY_LAST_EXIT_TIME_PREFIX = "last_exit_time_"
        private const val KEY_DAILY_EXIT_COUNT_PREFIX = "daily_exit_count_"
        private const val KEY_LAST_REWARD_DATE_PREFIX = "last_reward_date_"
        private const val KEY_TOTAL_POINTS_EARNED = "total_points_earned"
        private const val KEY_SUCCESSFUL_STUDY_SESSIONS = "successful_study_sessions"
        private const val KEY_APP_STUDY_SESSIONS_PREFIX = "app_study_sessions_"
    }

    // Helper to genericize flow creation
    private fun <T> getPreferenceFlow(key: String, default: T, getter: (String, T) -> T): Flow<T> {
        return callbackFlow {
            val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, changedKey ->
                if (key == changedKey || changedKey == null) {
                    trySend(getter(key, default))
                }
            }
            prefs.registerOnSharedPreferenceChangeListener(listener)
            trySend(getter(key, default)) // emit initial value
            awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
        }.distinctUntilChanged()
    }

    val triggerTimeFlow: Flow<Int> = getPreferenceFlow(KEY_TRIGGER_TIME, 60) { k, d -> 
        // Service uses Long in millis, UI uses Int in seconds.
        // If Service uses millis, we need to convert.
        // Service: prefs.getLong("trigger_time", ...)
        // Let's stick to storing Long Millis to be compatible with Service, but expose Int Seconds to UI.
        (prefs.getLong(k, d * 1000L) / 1000).toInt()
    }

    val pauseDurationFlow: Flow<Int> = getPreferenceFlow(KEY_PAUSE_DURATION, 10) { k, d ->
        (prefs.getLong(k, d * 1000L) / 1000).toInt()
    }

    val targetPackagesFlow: Flow<Set<String>> = getPreferenceFlow(KEY_BLOCKED_APPS, emptySet()) { k, d ->
        prefs.getStringSet(k, d) ?: d
    }

    val focusPointsFlow: Flow<Int> = getPreferenceFlow(KEY_POINTS, 0) { k, d ->
        prefs.getInt(k, d)
    }

    val isStudyModeFlow: Flow<Boolean> = getPreferenceFlow(KEY_STUDY_MODE, false) { k, d ->
        prefs.getBoolean(k, d)
    }
    
    val cooldownEndTimeFlow: Flow<Long> = getPreferenceFlow(KEY_COOLDOWN_END, 0L) { k, d ->
        prefs.getLong(k, d)
    }

    suspend fun setTriggerTime(seconds: Int) {
        prefs.edit().putLong(KEY_TRIGGER_TIME, seconds * 1000L).apply()
    }

    suspend fun setPauseDuration(seconds: Int) {
         prefs.edit().putLong(KEY_PAUSE_DURATION, seconds * 1000L).apply()
    }

    suspend fun toggleTargetPackage(packageName: String, isTarget: Boolean) {
        val current = prefs.getStringSet(KEY_BLOCKED_APPS, emptySet())?.toMutableSet() ?: mutableSetOf()
        if (isTarget) {
            current.add(packageName)
        } else {
            current.remove(packageName)
        }
        prefs.edit().putStringSet(KEY_BLOCKED_APPS, current).apply()
    }

    suspend fun addFocusPoints(points: Int) {
        val current = prefs.getInt(KEY_POINTS, 0)
        prefs.edit().putInt(KEY_POINTS, current + points).apply()
    }
    
    suspend fun addTotalPoints(points: Int) {
         val current = prefs.getInt(KEY_TOTAL_POINTS_EARNED, 0)
         prefs.edit().putInt(KEY_TOTAL_POINTS_EARNED, current + points).apply()
    }

    suspend fun setStudyMode(isActive: Boolean) {
        prefs.edit().putBoolean(KEY_STUDY_MODE, isActive).apply()
    }
    
    // Explicit API methods for ViewModel compatibility
    
    fun getLastExitTime(packageName: String): Long {
        return prefs.getLong(KEY_LAST_EXIT_TIME_PREFIX + packageName, -1)
    }
    
    fun clearLastExitTime(packageName: String) {
        prefs.edit().remove(KEY_LAST_EXIT_TIME_PREFIX + packageName).apply()
    }
    
    fun getLastRewardDate(packageName: String): String {
        return prefs.getString(KEY_LAST_REWARD_DATE_PREFIX + packageName, "") ?: ""
    }
    
    fun setLastRewardDate(packageName: String, date: String) {
        prefs.edit().putString(KEY_LAST_REWARD_DATE_PREFIX + packageName, date).apply()
    }
    
    fun getDailyExitCount(packageName: String): Int {
        return prefs.getInt(KEY_DAILY_EXIT_COUNT_PREFIX + packageName, 0)
    }
    
    fun incrementDailyExitCount(packageName: String) {
        val count = getDailyExitCount(packageName)
        prefs.edit().putInt(KEY_DAILY_EXIT_COUNT_PREFIX + packageName, count + 1).apply()
    }
    
    fun getHourlyInterventions(hourKey: String): Int {
        return prefs.getInt("hourly_interventions_$hourKey", 0)
    }
    
    fun setAppStudyMode(packageName: String) {
         // Logic from MainActivity:
         // putStringSet(KEY_STUDY_APPS, ...)
         // putLong(KEY_STUDY_START_TIME_PREFIX + pkg, ...)
         val studyApps = prefs.getStringSet("study_apps", emptySet())?.toMutableSet() ?: mutableSetOf()
         studyApps.add(packageName)
         
         prefs.edit()
            .putStringSet("study_apps", studyApps)
            .putLong("study_start_time_$packageName", System.currentTimeMillis())
            .apply()
    }
    
    fun incrementSuccessfulStudySessions() {
        val current = prefs.getInt(KEY_SUCCESSFUL_STUDY_SESSIONS, 0)
        prefs.edit().putInt(KEY_SUCCESSFUL_STUDY_SESSIONS, current + 1).apply()
    }
    
    fun incrementAppStudySessions(packageName: String) {
        val key = KEY_APP_STUDY_SESSIONS_PREFIX + packageName
        val current = prefs.getInt(key, 0)
        prefs.edit().putInt(key, current + 1).apply()
    }
}
