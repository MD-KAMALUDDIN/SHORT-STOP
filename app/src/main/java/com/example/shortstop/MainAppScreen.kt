package com.example.shortstop

import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.shortstop.viewmodel.DashboardViewModel
import com.example.shortstop.viewmodel.DashboardViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

@Composable
fun MainAppScreen() {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("shortstop_prefs", Context.MODE_PRIVATE)
    val settingsRepository = SettingsRepository(context)
    
    val viewModel: DashboardViewModel = viewModel(
        factory = DashboardViewModelFactory(context.applicationContext as Application, settingsRepository)
    )
    
    var showMotivation by remember { mutableStateOf(false) }
    var showStats by remember { mutableStateOf(false) }
    var showRank by remember { mutableStateOf(false) }
    var showFriends by remember { mutableStateOf(false) }
    var showAchievements by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    
    Box(modifier = Modifier.fillMaxSize()) {
        MainContent(
            viewModel = viewModel,
            onMotivationClick = { showMotivation = true },
            onStatsClick = { showStats = true },
            onRankClick = { showRank = true },
            onFriendsClick = { showFriends = true },
            onAchievementsClick = { showAchievements = true },
            onSettingsClick = { showSettings = true }
        )
        
        if (showMotivation) {
            MotivationOverlay { showMotivation = false }
        }
        if (showStats) {
            StatsOverlay(viewModel) { showStats = false }
        }
        if (showRank) {
            RankOverlay { showRank = false }
        }
        if (showFriends) {
            FriendsOverlay { showFriends = false }
        }
        if (showAchievements) {
            AchievementsOverlay { showAchievements = false }
        }
        if (showSettings) {
            SettingsOverlay { showSettings = false }
        }
    }
}

@Composable
fun MainContent(
    viewModel: DashboardViewModel,
    onMotivationClick: () -> Unit,
    onStatsClick: () -> Unit,
    onRankClick: () -> Unit,
    onFriendsClick: () -> Unit,
    onAchievementsClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val context = LocalContext.current
    val categories by viewModel.categories.collectAsState()
    val points by viewModel.points.collectAsState()
    var selectedCategory by remember { mutableStateOf("All") }
    
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF5F5F5))) {
        // Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary,
            shadowElevation = 4.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "ShortStop",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "💎 $points",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Menu Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    MenuButton("Motivation", Icons.Default.Star, onMotivationClick)
                    MenuButton("Stats", Icons.Default.Info, onStatsClick)
                    MenuButton("Rank", Icons.Default.Face, onRankClick)
                    MenuButton("Friends", Icons.Default.Person, onFriendsClick)
                    MenuButton("Achievements", Icons.Default.CheckCircle, onAchievementsClick)
                    MenuButton("Settings", Icons.Default.Settings, onSettingsClick)
                }
            }
        }
        
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            // Streak Progress Card
            item {
                StreakProgressCard(viewModel)
            }
            
            // Category Filters
            item {
                CategoryFilters(
                    categories = categories.map { it.name },
                    selectedCategory = selectedCategory,
                    onCategorySelected = { selectedCategory = it }
                )
            }
            
            // Apps List
            val filteredCategories = if (selectedCategory == "All") {
                categories
            } else {
                categories.filter { it.name == selectedCategory }
            }
            
            filteredCategories.forEach { category ->
                if (category.apps.isNotEmpty()) {
                    item {
                        Text(
                            text = category.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(16.dp, 8.dp)
                        )
                    }
                    
                    items(category.apps) { app ->
                        AppListItem(app, viewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun MenuButton(label: String, icon: ImageVector, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick).padding(4.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.2f),
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.padding(12.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            label,
            fontSize = 10.sp,
            color = Color.White
        )
    }
}

@Composable
fun StreakProgressCard(viewModel: DashboardViewModel) {
    val streak by viewModel.currentStreak.collectAsState()
    
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E8))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "🔥 Current Streak",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "$streak days",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun CategoryFilters(
    categories: List<String>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit
) {
    val allCategories = listOf("All") + categories
    
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp, 8.dp).horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        allCategories.forEach { category ->
            FilterChip(
                selected = category == selectedCategory,
                onClick = { onCategorySelected(category) },
                label = { Text(category) }
            )
        }
    }
}

@Composable
fun AppListItem(app: ApplicationInfo, viewModel: DashboardViewModel) {
    val context = LocalContext.current
    var isBlocked by remember { mutableStateOf(viewModel.isAppBlocked(app.packageName)) }
    
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val icon = app.loadIcon(context.packageManager).toBitmap().asImageBitmap()
            Image(
                bitmap = icon,
                contentDescription = null,
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp))
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Text(
                text = app.loadLabel(context.packageManager).toString(),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            
            Switch(
                checked = isBlocked,
                onCheckedChange = {
                    isBlocked = it
                    viewModel.toggleAppBlock(app.packageName, it)
                }
            )
        }
    }
}

@Composable
fun MotivationOverlay(onDismiss: () -> Unit) {
    var quote by remember { mutableStateOf("Loading...") }
    var refreshTrigger by remember { mutableStateOf(0) }
    
    LaunchedEffect(refreshTrigger) {
        quote = fetchDynamicQuote() ?: getDailyMotivation()
    }
    
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)).clickable { onDismiss() }
    ) {
        Card(
            modifier = Modifier.align(Alignment.Center).padding(32.dp).clickable { refreshTrigger++ },
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "💪 Motivation",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    quote,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(16.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { refreshTrigger++ }) {
                    Text("Get New Quote")
                }
            }
        }
    }
}

@Composable
fun StatsOverlay(viewModel: DashboardViewModel, onDismiss: () -> Unit) {
    val interventions by viewModel.totalInterventions.collectAsState()
    val timeSaved by viewModel.totalTimeSaved.collectAsState()
    
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)).clickable { onDismiss() }
    ) {
        Card(
            modifier = Modifier.align(Alignment.Center).padding(32.dp).fillMaxWidth(0.9f),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    "📊 Statistics",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(24.dp))
                StatItem("Total Interventions", interventions.toString())
                StatItem("Time Saved", "${timeSaved / 60000} minutes")
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
fun RankOverlay(onDismiss: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)).clickable { onDismiss() }
    ) {
        Card(
            modifier = Modifier.align(Alignment.Center).padding(32.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "🏆 Your Rank",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Coming Soon!", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
fun FriendsOverlay(onDismiss: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)).clickable { onDismiss() }
    ) {
        Card(
            modifier = Modifier.align(Alignment.Center).padding(32.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "👥 Friends",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Coming Soon!", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
fun AchievementsOverlay(onDismiss: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)).clickable { onDismiss() }
    ) {
        Card(
            modifier = Modifier.align(Alignment.Center).padding(32.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "🎖️ Achievements",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Coming Soon!", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
fun SettingsOverlay(onDismiss: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)).clickable { onDismiss() }
    ) {
        Card(
            modifier = Modifier.align(Alignment.Center).padding(32.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "⚙️ Settings",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Coming Soon!", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
    }
}

suspend fun fetchDynamicQuote(): String? {
    return withContext(Dispatchers.IO) {
        try {
            val url = URL("https://api.quotable.io/random?tags=wisdom|inspirational")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            
            val response = connection.inputStream.bufferedReader().readText()
            val json = JSONObject(response)
            val content = json.getString("content")
            val author = json.getString("author")
            
            "\"$content\"\n\n— $author"
        } catch (e: Exception) {
            null
        }
    }
}

fun getDailyMotivation(): String {
    val quotes = listOf(
        "Every moment you resist is a victory.",
        "Your future self will thank you.",
        "Small steps lead to big changes.",
        "You're stronger than your urges.",
        "Focus on what truly matters.",
        "Progress, not perfection.",
        "You've got this!",
        "Stay focused on your goals.",
        "Your time is valuable.",
        "Make today count."
    )
    return quotes.random()
}
