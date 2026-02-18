package com.example.shortstop.ui.screens

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.shortstop.utils.getRankInfo

@Composable
fun FriendsScreen(
    prefs: SharedPreferences,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val totalInterventions = prefs.getInt("total_interventions", 0)
    val totalTimeSaved = prefs.getLong("total_time_saved", 0L) / (1000 * 60)
    val points = prefs.getInt("points", 0)

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
                    text = "👥 Friends",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Button(onClick = onBack) {
                    Text("Back")
                }
            }
            
            LazyColumn {
                // Your Progress Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFE3F2FD)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "🏆 Your Progress",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1976D2)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "$totalInterventions interventions completed",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "${totalTimeSaved} minutes saved",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Current points: $points",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF4CAF50)
                            )
                        }
                    }
                }
                
                // Share Progress
                item {
                    Text(
                        text = "📤 Share Your Journey",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val shareText = "I've completed $totalInterventions interventions with ShortStop! 💪 Breaking the dopamine loop one step at a time. #DigitalWellbeing #Focus"
                                val shareIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Progress"))
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("📤 Share")
                        }
                        
                        Button(
                            onClick = {
                                val challengeText = "Challenge: Can you beat my $totalInterventions interventions on ShortStop? Let's break our phone addiction together! 🚀 #ShortStopChallenge"
                                val shareIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, challengeText)
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Challenge Friends"))
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFF9800)
                            )
                        ) {
                            Text("🏆 Challenge")
                        }
                    }
                }
                
                // Leaderboard Section
                item {
                    Text(
                        text = "🏅 Community Leaderboard",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
                
                // Mock leaderboard data
                val leaderboardData = listOf(
                    Triple("🥇 Alex", 127, "Digital Warrior"),
                    Triple("🥈 Sarah", 98, "Focus Master"),
                    Triple("🥉 Mike", 76, "Habit Builder"),
                    Triple("👤 You", totalInterventions, getRankInfo(prefs).first),
                    Triple("👤 Emma", 45, "Getting Started"),
                    Triple("👤 John", 32, "Building Habits")
                ).sortedByDescending { it.second }
                
                items(leaderboardData.take(6)) { (name, interventions, rank) ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (name.contains("You")) {
                                Color(0xFFFFD700).copy(alpha = 0.2f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (name.contains("You")) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.weight(1f)
                            )
                            Column(
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(
                                    text = "$interventions",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF4CAF50)
                                )
                                Text(
                                    text = rank,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                
                // Tips Section
                item {
                    Text(
                        text = "💬 Community Tips",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
                
                val communityTips = listOf(
                    "📱 Put your phone in another room while studying",
                    "⏰ Use the 25-minute focus technique",
                    "🌱 Replace phone time with a healthy habit",
                    "👥 Tell friends about your digital wellness goals",
                    "📚 Read a book instead of scrolling social media"
                )
                
                items(communityTips) { tip ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFE8F5E8)
                        )
                    ) {
                        Text(
                            text = tip,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }
    }
}
