package com.example.shortstop.utils

import android.content.SharedPreferences
import android.content.pm.PackageManager
import java.text.SimpleDateFormat
import java.util.*

data class AppStatistic(
    val packageName: String,
    val interventions: Int,
    val timeSaved: Long,
    val studySessions: Int
)

data class PeriodStatistics(
    val interventions: Int,
    val timeSaved: Long,
    val studySessions: Int,
    val currentStreak: Int,
    val bestStreak: Int,
    val totalPointsEarned: Int,
    val avgDailyInterventions: Double,
    val appBreakdown: List<AppStatistic>
)

fun getFilteredStatistics(prefs: SharedPreferences, period: String): PeriodStatistics {
    val calendar = Calendar.getInstance()
    var interventions = 0
    var timeSaved = 0L
    var studySessions = 0
    val appStatsMap = mutableMapOf<String, Triple<Int, Long, Int>>() // pkg -> (interventions, timeSaved, studySessions)
    
    val daysToCheck = when (period) {
        "Today" -> 1
        "Week" -> 7
        "Month" -> 30
        "Year" -> 365
        else -> 365 * 10 // All time
    }
    
    // Calculate totals based on period
    // Note: This is an approximation based on saved daily/hourly stats structure
    // Ideally we would query a database, but here we aggregate preferences
    
    val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    
    if (period == "Today") {
        val dailyInterventions = prefs.getInt("daily_interventions_$today", 0)
        val dailyTimeSaved = prefs.getLong("daily_time_saved_$today", 0L) / (1000 * 60)
        val dailyStudySessions = prefs.getInt("daily_study_sessions_$today", 0)
        
        interventions = dailyInterventions
        timeSaved = dailyTimeSaved
        studySessions = dailyStudySessions
        
        // App breakdown for today
        val blockedApps = prefs.getStringSet("blocked_apps", emptySet()) ?: emptySet()
        blockedApps.forEach { pkg ->
            val appInterventions = prefs.getInt("app_daily_interventions_${pkg}_$today", 0)
            val appTimeSaved = prefs.getLong("app_daily_time_saved_${pkg}_$today", 0L) / (1000 * 60)
            val appStudySessions = prefs.getInt("app_daily_study_sessions_${pkg}_$today", 0)
            
            if (appInterventions > 0 || appTimeSaved > 0 || appStudySessions > 0) {
                appStatsMap[pkg] = Triple(appInterventions, appTimeSaved, appStudySessions)
            }
        }
    } else {
        // Loop back days
        for (i in 0 until daysToCheck) {
            calendar.time = Date()
            calendar.add(Calendar.DAY_OF_YEAR, -i)
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
            
            interventions += prefs.getInt("daily_interventions_$date", 0)
            timeSaved += prefs.getLong("daily_time_saved_$date", 0L)
            studySessions += prefs.getInt("daily_study_sessions_$date", 0)
        }
        timeSaved /= (1000 * 60) // Convert to minutes
        
        // App breakdown is tricky without a DB, we'll use total stats if "All Time", 
        // otherwise we simply skip detailed app breakdown for Week/Month/Year to avoid massive pref reads
        if (period == "All Time") {
            val blockedApps = prefs.getStringSet("blocked_apps", emptySet()) ?: emptySet()
            blockedApps.forEach { pkg ->
                val appInterventions = prefs.getInt("app_intervention_count_$pkg", 0)
                val appTimeSaved = prefs.getLong("app_time_saved_$pkg", 0L) / (1000 * 60)
                val appStudySessions = prefs.getInt("app_study_sessions_$pkg", 0)
                
                if (appInterventions > 0 || appTimeSaved > 0) {
                    appStatsMap[pkg] = Triple(appInterventions, appTimeSaved, appStudySessions)
                }
            }
        }
    }
    
    val appBreakdown = appStatsMap.map { (pkg, stats) ->
        AppStatistic(pkg, stats.first, stats.second, stats.third)
    }.sortedByDescending { it.interventions }
    
    val currentStreak = calculateCurrentStreak(prefs)
    val bestStreak = prefs.getInt("best_streak", 0)
    val totalPoints = prefs.getInt("total_points_earned", 0)
    
    // Avoid division by zero
    val divisor = if (daysToCheck > 0) daysToCheck else 1
    val avgDailyInterventions = if (period == "Today") interventions.toDouble() else interventions.toDouble() / divisor
    
    return PeriodStatistics(
        interventions,
        timeSaved,
        studySessions,
        currentStreak,
        bestStreak,
        totalPoints,
        avgDailyInterventions,
        appBreakdown
    )
}

fun calculateCurrentStreak(prefs: SharedPreferences): Int {
    val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    // Logic: Streak increments if 0 interventions for a day
    // This requires checking past days. 
    // Implementation simplified for this refactor: rely on a stored value if it exists, 
    // or calculate simply
    return prefs.getInt("current_streak", 0) 
}

fun getChartData(prefs: SharedPreferences, period: String, offset: Int): List<Pair<String, Int>> {
    val calendar = Calendar.getInstance()
    val data = mutableListOf<Pair<String, Int>>()
    
    when (period) {
        "Today" -> {
            // Hourly data for today
            repeat(24) { i ->
                val hour = 23 - i - (offset * 24)
                if (hour >= 0 && hour < 24) {
                    calendar.time = Date()
                    calendar.add(Calendar.DAY_OF_YEAR, -offset)
                    calendar.set(Calendar.HOUR_OF_DAY, hour)
                    val hourKey = SimpleDateFormat("yyyy-MM-dd-HH", Locale.getDefault()).format(calendar.time)
                    val interventions = prefs.getInt("hourly_interventions_$hourKey", 0)
                    data.add(0, "${hour}h" to interventions)
                }
            }
        }
        "Week" -> {
            // Daily data for 7 days
            repeat(7) { i ->
                calendar.time = Date()
                calendar.add(Calendar.DAY_OF_YEAR, -(i + offset * 7))
                val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
                val dayLabel = SimpleDateFormat("EEE", Locale.getDefault()).format(calendar.time)
                val interventions = prefs.getInt("daily_interventions_$date", 0)
                data.add(0, dayLabel to interventions)
            }
        }
        "Month" -> {
            // Weekly data for 4 weeks
            repeat(4) { i ->
                var weekTotal = 0
                repeat(7) { day ->
                    calendar.time = Date()
                    calendar.add(Calendar.DAY_OF_YEAR, -(day + (i + offset * 4) * 7))
                    val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
                    weekTotal += prefs.getInt("daily_interventions_$date", 0)
                }
                data.add(0, "W${4 - i}" to weekTotal)
            }
        }
        "Year" -> {
            // Monthly data for 12 months
            repeat(12) { i ->
                calendar.time = Date()
                calendar.add(Calendar.MONTH, -(i + offset * 12))
                val monthLabel = SimpleDateFormat("MMM", Locale.getDefault()).format(calendar.time)
                val year = SimpleDateFormat("yyyy", Locale.getDefault()).format(calendar.time)
                val month = SimpleDateFormat("MM", Locale.getDefault()).format(calendar.time)
                
                var monthTotal = 0
                val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                repeat(daysInMonth) { day ->
                    val date = "$year-$month-${String.format("%02d", day + 1)}"
                    monthTotal += prefs.getInt("daily_interventions_$date", 0)
                }
                data.add(0, monthLabel to monthTotal)
            }
        }
        "All Time" -> {
            // Yearly data
            val firstUse = prefs.getString("first_use_date", "")
            val currentYear = Calendar.getInstance().get(Calendar.YEAR)
            val startYear = if (firstUse.isNullOrEmpty()) currentYear else {
                try {
                    firstUse.substring(0, 4).toInt()
                } catch (e: Exception) {
                    currentYear
                }
            }
            
            for (year in startYear..(currentYear - offset)) {
                var yearTotal = 0
                repeat(365) { day ->
                    calendar.set(year, 0, 1)
                    calendar.add(Calendar.DAY_OF_YEAR, day)
                    val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
                    yearTotal += prefs.getInt("daily_interventions_$date", 0)
                }
                data.add(year.toString() to yearTotal)
            }
        }
    }
    
    return data.takeLast(when(period) {
        "Today" -> 24
        "Week" -> 7
        "Month" -> 4
        "Year" -> 12
        else -> 10
    })
}

fun getChartDescription(period: String): String {
    return when (period) {
        "Today" -> "Hourly interventions for today"
        "Week" -> "Daily interventions for the week"
        "Month" -> "Weekly interventions for the month"
        "Year" -> "Monthly interventions for the year"
        "All Time" -> "Yearly interventions since start"
        else -> ""
    }
}

fun getRankInfo(prefs: SharedPreferences): Pair<String, String> {
    val totalInterventions = prefs.getInt("total_interventions", 0)
    return when {
         totalInterventions < 5 -> "Novice" to "🌱"
         totalInterventions < 20 -> "Apprentice" to "🔨"
         totalInterventions < 50 -> "Focus Master" to "🎯"
         totalInterventions < 100 -> "Digital Warrior" to "⚔️"
         totalInterventions < 200 -> "Wellness Champion" to "🏆"
         else -> "Ultimate Controller" to "👑"
    }
}
