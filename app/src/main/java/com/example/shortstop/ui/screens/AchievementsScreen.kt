package com.example.shortstop.ui.screens

import android.content.SharedPreferences
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.shortstop.ui.components.AchievementCard
import com.example.shortstop.utils.calculateCurrentStreak
import com.example.shortstop.utils.getRankInfo
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AchievementsScreen(
    prefs: SharedPreferences,
    onBack: () -> Unit
) {
    val totalInterventions = prefs.getInt("total_interventions", 0)
    val totalTimeSaved = prefs.getLong("total_time_saved", 0L) / (1000 * 60)
    val points = prefs.getInt("points", 0)
    val studySessions = prefs.getInt("successful_study_sessions", 0)
    val bestStreak = prefs.getInt("best_streak", 0)
    val totalPointsEarned = prefs.getInt("total_points_earned", 0)

    Card(
        modifier = Modifier.fillMaxSize(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "🏅 Achievements",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Button(onClick = onBack) {
                    Text("Back")
                }
            }
            
            LazyColumn {
                // Intervention Achievements
                item {
                    Text(
                        text = "🎯 Intervention Milestones",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                
                item {
                    AchievementCard(
                        title = "First Steps",
                        description = "Complete your first intervention",
                        emoji = "👶",
                        isUnlocked = totalInterventions >= 1
                    )
                }
                
                item {
                    AchievementCard(
                        title = "Getting Started",
                        description = "Complete 5 interventions",
                        emoji = "🚀",
                        isUnlocked = totalInterventions >= 5
                    )
                }
                
                item {
                    AchievementCard(
                        title = "Habit Builder",
                        description = "Complete 10 interventions",
                        emoji = "🔨",
                        isUnlocked = totalInterventions >= 10
                    )
                }
                
                item {
                    AchievementCard(
                        title = "Focus Master",
                        description = "Complete 25 interventions",
                        emoji = "🎯",
                        isUnlocked = totalInterventions >= 25
                    )
                }
                
                item {
                    AchievementCard(
                        title = "Ultimate Controller",
                        description = "Complete 50 interventions",
                        emoji = "👑",
                        isUnlocked = totalInterventions >= 50
                    )
                }
                
                item {
                    AchievementCard(
                        title = "Century Club",
                        description = "Complete 100 interventions",
                        emoji = "💯",
                        isUnlocked = totalInterventions >= 100
                    )
                }
                
                // Study Achievements
                item {
                    Text(
                        text = "📚 Study Achievements",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                
                item {
                    AchievementCard(
                        title = "Study Starter",
                        description = "Use study mode for the first time",
                        emoji = "📖",
                        isUnlocked = studySessions >= 1
                    )
                }
                
                item {
                    AchievementCard(
                        title = "Dedicated Learner",
                        description = "Use study mode 10 times",
                        emoji = "🎓",
                        isUnlocked = studySessions >= 10
                    )
                }
                
                item {
                    AchievementCard(
                        title = "Scholar",
                        description = "Use study mode 25 times",
                        emoji = "🏛️",
                        isUnlocked = studySessions >= 25
                    )
                }
                
                // Time & Streak Achievements
                item {
                    Text(
                        text = "⏰ Time & Streak Achievements",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                
                item {
                    AchievementCard(
                        title = "Time Saver",
                        description = "Save 1 hour of time",
                        emoji = "⏱️",
                        isUnlocked = totalTimeSaved >= 60
                    )
                }
                
                item {
                    AchievementCard(
                        title = "Productivity Booster",
                        description = "Save 5 hours of time",
                        emoji = "⚡",
                        isUnlocked = totalTimeSaved >= 300
                    )
                }
                
                item {
                    AchievementCard(
                        title = "Clean Streak",
                        description = "Go 3 days without interventions",
                        emoji = "🔥",
                        isUnlocked = bestStreak >= 3
                    )
                }
                
                item {
                    AchievementCard(
                        title = "Week Warrior",
                        description = "Go 7 days without interventions",
                        emoji = "🛡️",
                        isUnlocked = bestStreak >= 7
                    )
                }
                
                // Points & Progress Achievements
                item {
                    Text(
                        text = "💰 Points & Progress",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                
                item {
                    AchievementCard(
                        title = "Point Collector",
                        description = "Earn 500 total points",
                        emoji = "💎",
                        isUnlocked = totalPointsEarned >= 500
                    )
                }
                
                item {
                    AchievementCard(
                        title = "Wealthy Warrior",
                        description = "Have 200 points at once",
                        emoji = "💰",
                        isUnlocked = points >= 200
                    )
                }
                
                item {
                    AchievementCard(
                        title = "App Blocker",
                        description = "Block 5 different apps",
                        emoji = "🚫",
                        isUnlocked = (prefs.getStringSet("blocked_apps", emptySet())?.size ?: 0) >= 5
                    )
                }
                
                // Special Achievements
                item {
                    Text(
                        text = "🌟 Special Achievements",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                
                val (rankName, _) = getRankInfo(prefs)
                
                item {
                    AchievementCard(
                        title = "Digital Warrior",
                        description = "Reach Digital Warrior rank",
                        emoji = "⚔️",
                        isUnlocked = rankName == "Digital Warrior" || rankName == "Wellness Champion" || rankName == "Ultimate Controller"
                    )
                }
                
                item {
                    AchievementCard(
                        title = "Early Bird",
                        description = "Use the app for 7 days",
                        emoji = "🐦",
                        isUnlocked = run {
                            val firstUse = prefs.getString("first_use_date", "")
                            if (firstUse.isNullOrEmpty()) false
                            else {
                                try {
                                    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                    val firstDate = formatter.parse(firstUse)
                                    val today = formatter.parse(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()))
                                    val daysDiff = (today!!.time - firstDate!!.time) / (1000 * 60 * 60 * 24)
                                    daysDiff >= 7
                                } catch (e: Exception) {
                                    false
                                }
                            }
                        }
                    )
                }
                
                item {
                    AchievementCard(
                        title = "Perfect Balance",
                        description = "Have exactly 100 points",
                        emoji = "⚖️",
                        isUnlocked = points == 100
                    )
                }
            }
        }
    }
}
