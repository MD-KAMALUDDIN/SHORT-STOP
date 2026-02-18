package com.example.shortstop.ui.screens

import android.content.SharedPreferences
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.shortstop.ui.components.AppIcon
import com.example.shortstop.ui.components.SimpleBarChart
import com.example.shortstop.utils.getChartData
import com.example.shortstop.utils.getChartDescription
import com.example.shortstop.utils.getFilteredStatistics

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    prefs: SharedPreferences,
    pm: PackageManager,
    onBack: () -> Unit
) {
    var selectedTimePeriod by remember { mutableStateOf("Today") }

    Card(
        modifier = Modifier.fillMaxSize(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        // Get filtered statistics
        val stats = remember(selectedTimePeriod) {
            getFilteredStatistics(prefs, selectedTimePeriod)
        }
        
        LazyColumn(
            modifier = Modifier.padding(16.dp)
        ) {
            item {
                Row(
                   modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                   horizontalArrangement = Arrangement.SpaceBetween,
                   verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📊 Statistics",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Button(onClick = onBack) {
                        Text("Back")
                    }
                }
            }

            // Streak Progress Section (first item)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFF3E0)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "🔥 Streak Progress",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF6F00)
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "🔥",
                                        style = MaterialTheme.typography.displayMedium
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${stats.currentStreak}",
                                        style = MaterialTheme.typography.displaySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFF6F00)
                                    )
                                }
                                Text(
                                    text = "Days Clean",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color(0xFFFF6F00)
                                )
                            }
                            
                            Column(
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(
                                    text = "🏆 ${stats.bestStreak}",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFFB300)
                                )
                                Text(
                                    text = "Best Streak",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            StreakMilestone("3", stats.currentStreak >= 3)
                            StreakMilestone("7", stats.currentStreak >= 7)
                            StreakMilestone("14", stats.currentStreak >= 14)
                            StreakMilestone("30", stats.currentStreak >= 30)
                            StreakMilestone("100", stats.currentStreak >= 100)
                        }
                    }
                }
            }
            
            item { Spacer(modifier = Modifier.height(16.dp)) }
            
            // Time period selector
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 16.dp)
                ) {
                    val periods = listOf("Today", "Week", "Month", "Year", "All Time")
                    items(periods) { period ->
                        FilterChip(
                            onClick = { selectedTimePeriod = period },
                            label = { Text(period) },
                            selected = selectedTimePeriod == period
                        )
                    }
                }
            }
            
            // Overview Cards Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Interventions Card
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFE3F2FD)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "${stats.interventions}",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1976D2)
                            )
                            Text(
                                text = "Interventions",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF1976D2)
                            )
                        }
                    }
                    
                    // Time Saved Card
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFFF3E0)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "${stats.timeSaved}m",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF8F00)
                            )
                            Text(
                                text = "Time Saved",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFFF8F00)
                            )
                        }
                    }
                }
            }
            
            item { Spacer(modifier = Modifier.height(8.dp)) }
            
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Study Sessions Card
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFE8F5E8)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "${stats.studySessions}",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF388E3C)
                            )
                            Text(
                                text = "Study Sessions",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF388E3C)
                            )
                        }
                    }
                    
                    // Current Streak Card
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFFF3E0)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "🔥",
                                    style = MaterialTheme.typography.headlineMedium
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${stats.currentStreak}",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFF6F00)
                                )
                            }
                            Text(
                                text = "Day Streak",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFFF6F00)
                            )
                        }
                    }
                }
            }
            
            item { Spacer(modifier = Modifier.height(16.dp)) }
            
            // Bar Chart Section
            item {
                Text(
                    text = "📊 Trend Analysis",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            item {
                var chartOffset by remember { mutableIntStateOf(0) }
                
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        val chartData = remember(selectedTimePeriod, chartOffset) {
                            getChartData(prefs, selectedTimePeriod, chartOffset)
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { chartOffset++ }
                            ) {
                                Text(
                                    text = "<",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            SimpleBarChart(
                                data = chartData,
                                modifier = Modifier.weight(1f).height(120.dp)
                            )
                            
                            IconButton(
                                onClick = { if (chartOffset > 0) chartOffset-- },
                                enabled = chartOffset > 0
                            ) {
                                Text(
                                    text = ">",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (chartOffset > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = getChartDescription(selectedTimePeriod),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            item { Spacer(modifier = Modifier.height(16.dp)) }
            
            // App Breakdown Section
            item {
                Text(
                    text = "📱 App Breakdown",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            items(stats.appBreakdown) { appStat ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val appIcon = remember(appStat.packageName) {
                             try {
                                 pm.getApplicationIcon(appStat.packageName)
                             } catch (e: Exception) {
                                 null
                             }
                        }
                        
                        if (appIcon != null) {
                            AppIcon(
                                drawable = appIcon,
                                modifier = Modifier.size(32.dp)
                            )
                        } else {
                            Text(
                                text = "📱",
                                style = MaterialTheme.typography.headlineSmall,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Column(modifier = Modifier.weight(1f)) {
                            val appName = remember(appStat.packageName) {
                                try {
                                    pm.getApplicationLabel(pm.getApplicationInfo(appStat.packageName, 0)).toString()
                                } catch (e: Exception) {
                                    appStat.packageName.split(".").lastOrNull() ?: "Unknown"
                                }
                            }
                            
                            Text(
                                text = appName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${appStat.interventions} interventions • ${appStat.timeSaved}m saved",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        Text(
                            text = "${appStat.studySessions}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50)
                        )
                        Text(
                            text = " study",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            if (stats.appBreakdown.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No data for this period",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Start using blocked apps to see statistics",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            
            item { Spacer(modifier = Modifier.height(16.dp)) }
            
            // Progress Section
            item {
                Text(
                    text = "📈 Progress",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF3E5F5)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Total Points Earned: ${stats.totalPointsEarned}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Average Daily Interventions: ${String.format("%.1f", stats.avgDailyInterventions)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StreakMilestone(
    days: String,
    achieved: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.alpha(if (achieved) 1f else 0.3f)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .background(
                    color = if (achieved) Color(0xFFFF6F00) else Color.Gray,
                    shape = androidx.compose.foundation.shape.CircleShape
                )
        ) {
            Text(
                text = days,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            text = "Days",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// Extension to allow modifier manipulation
fun Modifier.alpha(alpha: Float) = this.then(Modifier.graphicsLayer(alpha = alpha))
