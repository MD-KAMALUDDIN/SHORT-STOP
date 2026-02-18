package com.example.shortstop.viewmodel

import com.example.shortstop.SettingsRepository


import android.app.Application
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AppCategory(
    val name: String,
    val apps: List<ApplicationInfo>
)

class DashboardViewModel(
    private val application: Application,
    private val repository: SettingsRepository
) : ViewModel() {
    
    val triggerTime = repository.triggerTimeFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, 60)
        
    val pauseDuration = repository.pauseDurationFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, 10)
        
    val targetPackages = repository.targetPackagesFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, emptySet())

    val points = repository.focusPointsFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)
    
    val focusPoints = points
    
    val totalInterventions = MutableStateFlow(0)
    val totalTimeSaved = MutableStateFlow(0L)
    val currentStreak = MutableStateFlow(0)
        
    val isStudyMode = repository.isStudyModeFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, false)
        
    val cooldownEndTime = repository.cooldownEndTimeFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, 0L)

    private val _installedApps = MutableStateFlow<List<ApplicationInfo>>(emptyList())
    
    // Categorized apps
    val categories: StateFlow<List<AppCategory>> = combine(_installedApps, targetPackages) { apps, targets ->
        categorizeApps(apps, application.packageManager)
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        loadInstalledApps()
        startRewardLoop()
        loadStatistics()
    }
    
    private fun loadStatistics() {
        viewModelScope.launch {
            val prefs = application.getSharedPreferences("shortstop_prefs", android.content.Context.MODE_PRIVATE)
            totalInterventions.value = prefs.getInt("total_interventions", 0)
            totalTimeSaved.value = prefs.getLong("total_time_saved", 0L)
            
            val lastInterventionDate = prefs.getString("last_intervention_date", "")
            val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
            currentStreak.value = if (lastInterventionDate == today) 1 else 0
        }
    }

    private fun loadInstalledApps() {
        viewModelScope.launch {
            val pm = application.packageManager
            val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 || (it.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0 }
            _installedApps.value = apps
        }
    }
    
    // Ported from MainActivity
    private fun startRewardLoop() {
        viewModelScope.launch {
            while (true) {
                val now = System.currentTimeMillis()
                val blocked = targetPackages.value
                val todayDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
                val currentHour = java.text.SimpleDateFormat("yyyy-MM-dd-HH", java.util.Locale.getDefault()).format(java.util.Date())

                blocked.forEach { pkg ->
                    val lastExit = repository.getLastExitTime(pkg)
                    val lastRewardDate = repository.getLastRewardDate(pkg)
                    
                    if (lastExit > 0 && now - lastExit >= 30 * 1000L) { // Using 10 mins (30s debug) constant from repo if avail or hardcode
                        // Reset daily count if it's a new day
                        val dailyCount = if (lastRewardDate == todayDate) {
                            repository.getDailyExitCount(pkg)
                        } else {
                            0 
                        }
                        
                        // Calculate base reward
                        val baseReward = when (dailyCount) {
                            0 -> 50
                            1 -> 25
                            2 -> 10
                            3 -> 5
                            else -> 1
                        }
                        
                        // Check hourly interventions for penalty
                        val hourlyInterventions = repository.getHourlyInterventions(currentHour)
                        
                        val multiplier = if (hourlyInterventions >= 5) 0.5 else 1.0
                        val reward = (baseReward * multiplier).toInt()
                        
                        repository.addFocusPoints(reward)
                        repository.incrementDailyExitCount(pkg)
                        repository.setLastRewardDate(pkg, todayDate)
                        repository.clearLastExitTime(pkg)
                        repository.addTotalPoints(reward)
                    }
                }
                kotlinx.coroutines.delay(30_000)
            }
        }
    }
    
    fun markAppAsStudy(packageName: String) {
        val currentPoints = points.value
        if (currentPoints >= 50) {
            viewModelScope.launch {
                repository.addFocusPoints(-50)
                repository.setAppStudyMode(packageName)
                repository.incrementSuccessfulStudySessions()
                repository.incrementAppStudySessions(packageName)
                repository.addTotalPoints(50) 
            }
        }
    }
    
    fun isAppStudyActive(packageName: String): Boolean {
        // This requires synchronous check or exposed state. 
        // For now, simplistically check repo? 
        // Better to expose a Map<String, Long> of study start times if we want UI to update instantly.
        // We'll leave this for now and rely on UI to refresh or simple check.
        return false 
    }
    
    fun isAppBlocked(packageName: String): Boolean {
        return targetPackages.value.contains(packageName)
    }

    fun toggleAppBlock(packageName: String, isBlocked: Boolean) {
        viewModelScope.launch { repository.toggleTargetPackage(packageName, isBlocked) }
    }

    // Compatibility for old method signature
    fun toggleApp(packageName: String, isTarget: Boolean) {
        toggleAppBlock(packageName, isTarget)
    }

    fun updateTriggerTime(seconds: Int) {
        viewModelScope.launch { repository.setTriggerTime(seconds) }
    }

    fun updatePauseDuration(seconds: Int) {
        viewModelScope.launch { repository.setPauseDuration(seconds) }
    }
    
    fun buyStudyMode() {
        val currentPoints = points.value
        if (currentPoints >= 50) {
            viewModelScope.launch {
                repository.addFocusPoints(-50)
                repository.setStudyMode(true)
            }
        }
    }
}

class DashboardViewModelFactory(
    private val application: Application,
    private val repository: SettingsRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DashboardViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

// Logic moved from MainActivity
fun categorizeApps(apps: List<ApplicationInfo>, pm: PackageManager): List<AppCategory> {
    val social = mutableListOf<ApplicationInfo>()
    val entertainment = mutableListOf<ApplicationInfo>()
    val games = mutableListOf<ApplicationInfo>()
    val productivity = mutableListOf<ApplicationInfo>()
    val other = mutableListOf<ApplicationInfo>()

    apps.forEach { app ->
        val pkg = app.packageName.lowercase()
        when {
            pkg.contains("facebook") || pkg.contains("instagram") || pkg.contains("twitter") ||
            pkg.contains("snapchat") || pkg.contains("tiktok") || pkg.contains("whatsapp") ||
            pkg.contains("telegram") || pkg.contains("discord") || pkg.contains("reddit") -> social.add(app)
            
            pkg.contains("youtube") || pkg.contains("netflix") || pkg.contains("disney") ||
            pkg.contains("hulu") || pkg.contains("primevideo") || pkg.contains("spotify") ||
            pkg.contains("twitch") -> entertainment.add(app)
            
            (app.flags and ApplicationInfo.FLAG_IS_GAME) != 0 || pkg.contains("game") -> games.add(app)
            
            pkg.contains("gmail") || pkg.contains("outlook") || pkg.contains("slack") ||
            pkg.contains("zoom") || pkg.contains("teams") || pkg.contains("docs") ||
            pkg.contains("drive") || pkg.contains("keep") -> productivity.add(app)
            
            else -> other.add(app)
        }
    }
    
    val categories = mutableListOf<AppCategory>()
    if (social.isNotEmpty()) categories.add(AppCategory("Social Media \uD83D\uDCAC", social.sortedBy { it.loadLabel(pm).toString() }))
    if (entertainment.isNotEmpty()) categories.add(AppCategory("Entertainment \uD83C\uDFAC", entertainment.sortedBy { it.loadLabel(pm).toString() }))
    if (games.isNotEmpty()) categories.add(AppCategory("Games \uD83C\uDFAE", games.sortedBy { it.loadLabel(pm).toString() }))
    if (productivity.isNotEmpty()) categories.add(AppCategory("Productivity \uD83D\uDCCA", productivity.sortedBy { it.loadLabel(pm).toString() }))
    if (other.isNotEmpty()) categories.add(AppCategory("Other Apps \uD83D\uDCC1", other.sortedBy { it.loadLabel(pm).toString() }))
    
    return categories
}
