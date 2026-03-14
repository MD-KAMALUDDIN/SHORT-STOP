@file:Suppress("DEPRECATION")

package com.example.shortstop

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.compose.animation.core.*
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.launch
import org.json.JSONObject

data class AppCategory(val name: String, val apps: List<ApplicationInfo>)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen() {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("shortstop_prefs", Context.MODE_PRIVATE)
    val repository = remember { com.example.shortstop.database.ShortStopRepository(context) }
    
    val userStats by repository.userStats.collectAsState(initial = null)
    val blockedAppsList by repository.blockedApps.collectAsState(initial = emptyList())
    val blockedApps = blockedAppsList.map { it.packageName }.toSet()
    
    var points by remember { mutableStateOf(userStats?.points ?: 0) }
    var installedApps by remember { mutableStateOf<List<ApplicationInfo>>(emptyList()) }
    var selectedCategory by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }
    
    var showMotivation by remember { mutableStateOf(false) }
    var showStats by remember { mutableStateOf(false) }
    var showRank by remember { mutableStateOf(false) }
    var showAchievements by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showServiceMessage by remember { mutableStateOf(false) }
    var showBatteryDialog by remember { mutableStateOf(false) }
    var showRatingPrompt by remember { mutableStateOf(false) }
    var showHelp by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        val hasShownBatteryDialog = prefs.getBoolean("has_shown_battery_dialog", false)
        if (!hasShownBatteryDialog) {
            kotlinx.coroutines.delay(2000)
            showBatteryDialog = true
        }
    }
    
    LaunchedEffect(userStats?.totalInterventions) {
        val interventions = userStats?.totalInterventions ?: 0
        val hasRated = prefs.getBoolean("has_rated_app", false)
        if (interventions >= 10 && !hasRated) {
            showRatingPrompt = true
        }
    }
    
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingToggle by remember { mutableStateOf<Pair<String, Boolean>?>(null) }
    
    LaunchedEffect(userStats) {
        points = userStats?.points ?: 0
    }
    
    val serviceRunning by ShortStopService.isServiceRunning.collectAsState()
    
    LaunchedEffect(Unit) {
        val pm = context.packageManager
        installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 || (it.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0 }
    }
    
    LaunchedEffect(Unit) {
        val hasInitialized = prefs.getBoolean("has_initialized_stats", false)
        if (!hasInitialized && userStats == null) {
            repository.updatePoints(100)
            prefs.edit().putBoolean("has_initialized_stats", true).apply()
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF5F5F5))) {
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
                        Text("ShortStop", style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
                        Text("💎 $points", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MenuButton(
                            if (serviceRunning) "Service ✓" else "Service ✗",
                            Icons.Default.CheckCircle,
                            if (serviceRunning) Color.Green else Color.Red
                        ) {
                            if (!serviceRunning) {
                                showServiceMessage = true
                            }
                        }
                        MenuButton("Motivation", Icons.Default.Star, Color.White) { showMotivation = true }
                        MenuButton("Stats", Icons.Default.Info, Color.White) { showStats = true }
                        MenuButton("Rank", Icons.Default.Face, Color.White) { showRank = true }
                        MenuButton("Achievements", Icons.Default.CheckCircle, Color.White) { showAchievements = true }
                        MenuButton("Help", Icons.Default.Info, Color.White) { showHelp = true }
                        MenuButton("Settings", Icons.Default.Settings, Color.White) { showSettings = true }
                    }
                }
            }
            
            val categories = categorizeApps(installedApps, context.packageManager)
            val filteredByCategory = when (selectedCategory) {
                "All" -> categories
                "Selected" -> listOf(AppCategory("Selected Apps 📌", installedApps.filter { blockedApps.contains(it.packageName) }))
                else -> categories.filter { it.name.contains(selectedCategory) }
            }
            
            val filteredCategories = if (searchQuery.isNotEmpty()) {
                filteredByCategory.map { category ->
                    AppCategory(
                        category.name,
                        category.apps.filter { 
                            it.loadLabel(context.packageManager).toString().contains(searchQuery, ignoreCase = true)
                        }
                    )
                }.filter { it.apps.isNotEmpty() }
            } else {
                filteredByCategory
            }
            
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    ClaimRewardsCard(prefs)
                }
                
                item {
                    Card(modifier = Modifier.fillMaxWidth().padding(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E8))) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("🔥 Current Streak", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            val streak = userStats?.currentStreak ?: 0
                            Text("$streak days", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                
                if (blockedApps.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("🎯", fontSize = 48.sp)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("No Blocked Apps Yet", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Start by blocking your most distracting apps below",
                                    fontSize = 14.sp,
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
                
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        placeholder = { Text("Search apps...") },
                        singleLine = true,
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, "Clear")
                                }
                            }
                        }
                    )
                }
                
                item {
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp, 8.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("All", "Selected", "Social", "Entertainment", "Games", "Productivity", "Other").forEach { cat ->
                            FilterChip(
                                selected = cat == selectedCategory,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat) },
                                leadingIcon = null
                            )
                        }
                    }
                }
                
                filteredCategories.forEach { category ->
                    if (category.apps.isNotEmpty()) {
                        if (selectedCategory != "All") {
                            item {
                                Text(category.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp, 8.dp))
                            }
                        }
                        items(category.apps, key = { it.packageName }) { app ->
                            val scope = rememberCoroutineScope()
                            AppItemWithStudy(app, blockedApps.contains(app.packageName), points, context, repository, blockedApps.size) { pkg, blocked ->
                                pendingToggle = Pair(pkg, blocked)
                                scope.launch {
                                    val result = snackbarHostState.showSnackbar(
                                        message = if (blocked) "App blocked" else "App unblocked",
                                        actionLabel = "Undo",
                                        duration = SnackbarDuration.Short
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        pendingToggle = null
                                    } else {
                                        pendingToggle?.let {
                                            repository.toggleBlockedApp(it.first, it.second)
                                            pendingToggle = null
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
        )
        
        if (showMotivation) {
            androidx.activity.compose.BackHandler { showMotivation = false }
            MotivationOverlay { showMotivation = false }
        }
        if (showStats) {
            androidx.activity.compose.BackHandler { showStats = false }
            StatsOverlay(userStats) { showStats = false }
        }
        if (showRank) {
            androidx.activity.compose.BackHandler { showRank = false }
            RankOverlay(userStats, prefs) { showRank = false }
        }
        if (showAchievements) {
            androidx.activity.compose.BackHandler { showAchievements = false }
            AchievementsOverlay(userStats, prefs) { showAchievements = false }
        }
        if (showHelp) {
            androidx.activity.compose.BackHandler { showHelp = false }
            HelpOverlay { showHelp = false }
        }
        if (showSettings) {
            androidx.activity.compose.BackHandler { showSettings = false }
            SettingsOverlay { showSettings = false }
        }
        
        if (showServiceMessage) {
            AlertDialog(
                onDismissRequest = { showServiceMessage = false },
                containerColor = Color.White.copy(alpha = 0.95f),
                title = { Text("⚠️ Service Not Running") },
                text = { Text("Please turn OFF and then turn ON the ShortStop accessibility service to activate it properly.") },
                confirmButton = {
                    Button(onClick = {
                        showServiceMessage = false
                        val intent = Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        context.startActivity(intent)
                    }) {
                        Text("Open Settings")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showServiceMessage = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
        
        if (showBatteryDialog) {
            AlertDialog(
                onDismissRequest = {
                    showBatteryDialog = false
                    prefs.edit().putBoolean("has_shown_battery_dialog", true).apply()
                },
                containerColor = Color.White.copy(alpha = 0.95f),
                title = { Text("⚡ Keep ShortStop Running") },
                text = {
                    Column {
                        Text("For reliable interventions 24/7, disable battery optimization for ShortStop.")
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("✅ This ensures the service won't be killed", fontSize = 14.sp)
                        Text("✅ Interventions work even after hours", fontSize = 14.sp)
                        Text("✅ Minimal battery impact", fontSize = 14.sp)
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        showBatteryDialog = false
                        prefs.edit().putBoolean("has_shown_battery_dialog", true).apply()
                        try {
                            val intent = Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                            intent.data = android.net.Uri.parse("package:${context.packageName}")
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            val intent = Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                            context.startActivity(intent)
                        }
                    }) {
                        Text("Open Settings")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showBatteryDialog = false
                        prefs.edit().putBoolean("has_shown_battery_dialog", true).apply()
                    }) {
                        Text("Skip")
                    }
                }
            )
        }
        
        if (showRatingPrompt) {
            AlertDialog(
                onDismissRequest = {
                    showRatingPrompt = false
                    prefs.edit().putBoolean("has_rated_app", true).apply()
                },
                containerColor = Color.White.copy(alpha = 0.95f),
                title = { Text("🎉 You're doing great!") },
                text = {
                    Column {
                        Text("You've completed 10 interventions! That's awesome progress.")
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Enjoying ShortStop? A 5-star review helps us reach more people breaking free from phone addiction.", fontSize = 14.sp)
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        showRatingPrompt = false
                        prefs.edit().putBoolean("has_rated_app", true).apply()
                        try {
                            val intent = Intent(Intent.ACTION_VIEW)
                            intent.data = android.net.Uri.parse("market://details?id=${context.packageName}")
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            val intent = Intent(Intent.ACTION_VIEW)
                            intent.data = android.net.Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}")
                            context.startActivity(intent)
                        }
                    }) {
                        Text("⭐ Rate 5 Stars")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showRatingPrompt = false
                        prefs.edit().putBoolean("has_rated_app", true).apply()
                    }) {
                        Text("Maybe Later")
                    }
                }
            )
        }
    }
}

@Composable
fun MenuButton(label: String, icon: ImageVector, iconColor: Color = Color.White, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = onClick).padding(4.dp)) {
        Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.2f), modifier = Modifier.size(48.dp)) {
            Icon(icon, contentDescription = label, tint = iconColor, modifier = Modifier.padding(12.dp))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, fontSize = 10.sp, color = Color.White, maxLines = 1)
    }
}

@Composable
fun ClaimRewardsCard(@Suppress("UNUSED_PARAMETER") prefs: android.content.SharedPreferences) {
    @Suppress("UNUSED_VARIABLE", "ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE", "unused")
    val context = LocalContext.current
    var pendingRewards by remember { mutableIntStateOf(0) }
    @Suppress("ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE", "unused")
    var showClaimDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        while (true) {
            pendingRewards = calculatePendingRewards(prefs)
            kotlinx.coroutines.delay(1000)
        }
    }
    
    if (pendingRewards > 0) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFD700).copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("🎁 Rewards Ready!", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("You have $pendingRewards points waiting", style = MaterialTheme.typography.bodyMedium)
                    }
                    Button(
                        onClick = { showClaimDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700))
                    ) {
                        Text("💎 Claim", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
    
    if (showClaimDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("🎉 Rewards Claimed!") },
            text = { Text("You earned $pendingRewards points for staying away from blocked apps!") },
            confirmButton = {
                Button(onClick = {
                    claimAllRewards(prefs)
                    pendingRewards = 0
                }) {
                    Text("Awesome!")
                }
            }
        )
    }
}

fun calculatePendingRewards(@Suppress("UNUSED_PARAMETER") prefs: android.content.SharedPreferences): Int {
    return 0
}

fun claimAllRewards(@Suppress("UNUSED_PARAMETER") prefs: android.content.SharedPreferences) {
}

@Composable
fun AppItemWithStudy(app: ApplicationInfo, isBlocked: Boolean, currentPoints: Int, @Suppress("UNUSED_PARAMETER") context: Context, repository: com.example.shortstop.database.ShortStopRepository, @Suppress("UNUSED_PARAMETER") blockedCount: Int, onToggle: (String, Boolean) -> Unit) {
    val localContext = LocalContext.current
    var checked by remember { mutableStateOf(isBlocked) }
    LaunchedEffect(isBlocked) { checked = isBlocked }
    var showStudyDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    val studyApps by repository.studyApps.collectAsState(initial = emptyList())
    val studyApp = studyApps.find { it.packageName == app.packageName }
    @Suppress("UNUSED_VARIABLE", "ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE", "unused")
    val isStudyActive = studyApp?.isStudyMode == true && 
                        studyApp.studyStartTime > 0 && 
                        (System.currentTimeMillis() - studyApp.studyStartTime) < 5 * 60 * 1000L
    
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                val icon = app.loadIcon(localContext.packageManager).toBitmap().asImageBitmap()
                Image(bitmap = icon, contentDescription = null, modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)))
                Spacer(modifier = Modifier.width(12.dp))
                Text(app.loadLabel(localContext.packageManager).toString(), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Switch(
                    checked = checked, 
                    onCheckedChange = { newValue ->
                        checked = newValue
                        onToggle(app.packageName, newValue)
                    }
                )
            }
            
            if (checked) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { showStudyDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = currentPoints >= 50,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    )
                ) {
                    Text("Study Mode 25min (50 pts)")
                }
            }
        }
    }
    
    if (showStudyDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("📚 Study Mode") },
            text = { Text("Activate 25-minute Pomodoro session for ${app.loadLabel(localContext.packageManager)}? This will cost 50 points.") },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        repository.updatePoints(currentPoints - 50)
                        repository.setStudyMode(app.packageName, System.currentTimeMillis())
                    }
                }) {
                    Text("Activate")
                }
            },
            dismissButton = {
                TextButton(onClick = { }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun MotivationOverlay(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("shortstop_prefs", Context.MODE_PRIVATE)
    val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
    
    val lastQuoteDate = prefs.getString("last_quote_date", "") ?: ""
    val savedQuote = prefs.getString("daily_quote", "") ?: ""
    
    val quote = if (lastQuoteDate == today && savedQuote.isNotEmpty()) {
        savedQuote
    } else {
        val newQuote = getLocalQuote(context)
        prefs.edit().putString("last_quote_date", today).putString("daily_quote", newQuote).apply()
        newQuote
    }
    
    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)).clickable { onDismiss() }) {
        Card(modifier = Modifier.align(Alignment.Center).padding(32.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("💪 Daily Motivation", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                Text(quote, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(16.dp), textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
fun StatsOverlay(userStats: com.example.shortstop.database.UserStatsEntity?, onDismiss: () -> Unit) {
    val interventions = userStats?.totalInterventions ?: 0
    val timeSaved = userStats?.totalTimeSaved ?: 0L
    val studySessions = userStats?.successfulStudySessions ?: 0
    val streak = userStats?.currentStreak ?: 0
    val points = userStats?.points ?: 0
    
    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)).clickable { onDismiss() }) {
        Card(modifier = Modifier.align(Alignment.Center).padding(24.dp).fillMaxWidth(0.95f).verticalScroll(rememberScrollState()), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("📊 Statistics", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(24.dp))
                
                StatCard(
                    title = "Total Interventions",
                    value = interventions.toString(),
                    icon = "🛡️",
                    color = Color(0xFF2196F3)
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                StatCard(
                    title = "Time Saved",
                    value = "${timeSaved / 60000} min",
                    icon = "⏱️",
                    color = Color(0xFF4CAF50)
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                StatCard(
                    title = "Study Sessions",
                    value = studySessions.toString(),
                    icon = "📚",
                    color = Color(0xFFFF9800)
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                StatCard(
                    title = "Current Streak",
                    value = "$streak days",
                    icon = "🔥",
                    color = Color(0xFFE91E63)
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                StatCard(
                    title = "Total Points",
                    value = points.toString(),
                    icon = "💎",
                    color = Color(0xFF9C27B0)
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Close") }
            }
        }
    }
}

@Composable
fun StatCard(title: String, value: String, icon: String, color: Color) {
    val scale = remember { Animatable(0.8f) }
    
    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }
    
    Card(
        modifier = Modifier.fillMaxWidth().graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
        },
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(icon, fontSize = 32.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
fun RankOverlay(userStats: com.example.shortstop.database.UserStatsEntity?, prefs: android.content.SharedPreferences, onDismiss: () -> Unit) {
    val score = calculateRankScore(userStats)
    val rank = getRankFromScore(score)
    val rankColor = getRankColor(score)
    val (nextRank, nextThreshold) = getNextRankThreshold(score)
    val progress = if (nextThreshold > 0) (score.toFloat() / nextThreshold).coerceIn(0f, 1f) else 1f
    @Suppress("UNUSED_VARIABLE", "ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE", "unused")
    val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
    @Suppress("UNUSED_VARIABLE", "ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE", "unused")
    val dailyExitCount = prefs.getInt("daily_exit_count_$today", 0)
    
    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)).clickable { onDismiss() }) {
        Card(modifier = Modifier.align(Alignment.Center).padding(24.dp).fillMaxWidth(0.95f).verticalScroll(rememberScrollState()), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🏆 Your Rank", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(24.dp))
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = rankColor.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(rank, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = rankColor)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Score: $score", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                if (nextRank.isNotEmpty()) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("Next Rank", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(nextRank, fontWeight = FontWeight.Bold, color = rankColor)
                                    Text("$score / $nextThreshold", color = Color.Gray)
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                LinearProgressIndicator(
                                    progress = progress,
                                    modifier = Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp)),
                                    color = rankColor,
                                    trackColor = Color(0xFFE0E0E0)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    "${nextThreshold - score} points to go",
                                    fontSize = 14.sp,
                                    color = rankColor,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text("How to Earn Points", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E8))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                PointEarnRow("✅ Clean exit (10 min away)", "50\u00A0pts")
                                PointEarnRow("🔥 Daily streak bonus", "10\u00A0pts/day")
                                PointEarnRow("📚 Complete study session", "5\u00A0pts")
                            }
                        }
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFD700).copy(alpha = 0.2f))
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("🎉", fontSize = 48.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Maximum Rank Achieved!",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = rankColor
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PointEarnRow(action: String, points: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(action, fontSize = 14.sp)
        Text(points, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
    }
}

@Suppress("unused")
@Composable
fun FriendsOverlay(onDismiss: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)).clickable { onDismiss() }) {
        Card(modifier = Modifier.align(Alignment.Center).padding(32.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("👥 Friends", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Coming Soon!", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
fun HelpOverlay(onDismiss: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)).clickable { onDismiss() }) {
        Card(
            modifier = Modifier.align(Alignment.Center).padding(32.dp).fillMaxWidth(0.9f).fillMaxHeight(0.8f),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState())) {
                Text("❓ Help & FAQ", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(24.dp))
                
                HelpSection(
                    title = "How to Block Apps",
                    content = "1. Scroll through the app list\n2. Toggle ON the apps you want to block\n3. After 7 seconds in a blocked app, you'll see an intervention"
                )
                
                HelpSection(
                    title = "What is Study Mode?",
                    content = "Study Mode gives you 25 minutes of uninterrupted access to a blocked app. Perfect for when you need to use social media for work or study. Costs 50 points."
                )
                
                HelpSection(
                    title = "How do Points work?",
                    content = "Earn points by:\n• Exiting blocked apps (50 pts)\n• Maintaining streaks (10 pts/day)\n• Completing study sessions (5 pts)\n\nSpend points on Study Mode (50 pts)"
                )
                
                HelpSection(
                    title = "What are Streaks?",
                    content = "A streak counts consecutive days where you had at least one intervention. Keep your streak alive by using the app daily!"
                )
                
                HelpSection(
                    title = "App Not Working?",
                    content = "1. Enable Accessibility Service in Settings\n2. Disable Battery Optimization for ShortStop\n3. Restart your device\n4. Re-enable the accessibility service"
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Got it!")
                }
            }
        }
    }
}

@Composable
fun HelpSection(title: String, content: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(content, fontSize = 14.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
fun AchievementsOverlay(userStats: com.example.shortstop.database.UserStatsEntity?, prefs: android.content.SharedPreferences, onDismiss: () -> Unit) {
    val achievements = getAchievements(userStats, prefs)
    
    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)).clickable { onDismiss() }) {
        Card(modifier = Modifier.align(Alignment.Center).padding(32.dp).fillMaxWidth(0.9f).fillMaxHeight(0.8f), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState())) {
                Text("🎖️ Achievements", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                achievements.forEach { achievement ->
                    AchievementItem(achievement)
                }
            }
        }
    }
}

@Composable
fun AchievementItem(achievement: Achievement) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = if (achievement.unlocked) Color(0xFFE8F5E8) else Color(0xFFF5F5F5))) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(achievement.icon, fontSize = 32.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(achievement.name, fontWeight = FontWeight.Bold)
                Text(achievement.description, fontSize = 12.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
fun SettingsOverlay(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("shortstop_prefs", Context.MODE_PRIVATE)
    @Suppress("UNUSED_VARIABLE", "ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE", "unused")
    val repository = remember { com.example.shortstop.database.ShortStopRepository(context) }
    val scope = rememberCoroutineScope()
    @Suppress("ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE", "unused")
    var showResetDialog by remember { mutableStateOf(false) }
    @Suppress("ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE", "unused")
    var showExportDialog by remember { mutableStateOf(false) }
    
    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)).clickable { onDismiss() }) {
        Card(modifier = Modifier.align(Alignment.Center).padding(32.dp).fillMaxWidth(0.9f).verticalScroll(rememberScrollState()), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("⚙️ Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = { showExportDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("💾 Export Data")
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Text("ShortStop v1.0", style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Break free from digital distractions", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { showResetDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("🗑️ Reset All Data")
                }
            }
        }
    }
    
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("⚠️ Reset All Data?") },
            text = { Text("This will permanently delete all your progress, points, statistics, and blocked apps. This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            try {
                                prefs.edit().clear().apply()
                                val db = com.example.shortstop.database.ShortStopDatabase.getDatabase(context)
                                db.clearAllTables()
                                onDismiss()
                            } catch (e: Exception) {
                                android.util.Log.e("ShortStop", "Reset failed: ${e.message}")
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Delete Everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("💾 Export Data") },
            text = { Text("Export your data as JSON. You can save it to Google Drive or share it.") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            try {
                                val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                                    addCategory(Intent.CATEGORY_OPENABLE)
                                    type = "application/json"
                                    putExtra(Intent.EXTRA_TITLE, "shortstop_backup_${System.currentTimeMillis()}.json")
                                }
                                (context as? android.app.Activity)?.startActivityForResult(intent, 1001)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                ) {
                    Text("Export")
                }
            },
            dismissButton = {
                TextButton(onClick = { }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Suppress("unused")
@Composable
fun StatItem(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
    }
}

data class Achievement(val icon: String, val name: String, val description: String, val unlocked: Boolean)

fun getAchievements(userStats: com.example.shortstop.database.UserStatsEntity?, prefs: android.content.SharedPreferences): List<Achievement> {
    val interventions = userStats?.totalInterventions ?: 0
    val studySessions = userStats?.successfulStudySessions ?: 0
    val streak = userStats?.currentStreak ?: 0
    val timeSaved = (userStats?.totalTimeSaved ?: 0L) / 60000
    val totalPoints = userStats?.totalPointsEarned ?: 0
    val blockedApps = prefs.getStringSet("blocked_apps", emptySet())?.size ?: 0
    
    return listOf(
        Achievement("🎯", "First Steps", "Complete your first intervention", interventions >= 1),
        Achievement("🔟", "Ten Strong", "Complete 10 interventions", interventions >= 10),
        Achievement("🔥", "On Fire", "Complete 25 interventions", interventions >= 25),
        Achievement("⭐", "Half Century", "Complete 50 interventions", interventions >= 50),
        Achievement("💯", "Century Club", "Complete 100 interventions", interventions >= 100),
        Achievement("🏆", "Intervention Master", "Complete 500 interventions", interventions >= 500),
        Achievement("📚", "Study Starter", "Complete your first study session", studySessions >= 1),
        Achievement("🎓", "Dedicated Student", "Complete 10 study sessions", studySessions >= 10),
        Achievement("🧑‍🎓", "Scholar", "Complete 50 study sessions", studySessions >= 50),
        Achievement("⏱️", "Time Saver", "Save 60 minutes total", timeSaved >= 60),
        Achievement("🔥", "Clean Streak", "Maintain a 7-day streak", streak >= 7),
        Achievement("📅", "Week Warrior", "Maintain a 30-day streak", streak >= 30),
        Achievement("💪", "Unstoppable", "Maintain a 100-day streak", streak >= 100),
        Achievement("💎", "Point Collector", "Earn 100 total points", totalPoints >= 100),
        Achievement("💰", "Wealthy Warrior", "Earn 500 total points", totalPoints >= 500),
        Achievement("🛡️", "App Blocker", "Block 5 or more apps", blockedApps >= 5),
        Achievement("⚔️", "Digital Warrior", "Reach 500 rank score", calculateRankScore(userStats) >= 500)
    )
}


@Suppress("unused")
fun calculateStreak(prefs: android.content.SharedPreferences): Int {
    val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
    val lastDate = prefs.getString("last_intervention_date", "") ?: ""
    val currentStreak = prefs.getInt("current_streak", 0)
    
    if (lastDate.isEmpty()) return 0
    if (lastDate == today) return currentStreak
    
    val yesterday = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        .format(java.util.Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000L))
    
    return if (lastDate == yesterday) currentStreak else 0
}

fun calculateRankScore(userStats: com.example.shortstop.database.UserStatsEntity?): Int {
    val streak = userStats?.currentStreak ?: 0
    val timeSaved = (userStats?.totalTimeSaved ?: 0L) / 60000
    val studySessions = userStats?.successfulStudySessions ?: 0
    val interventions = userStats?.totalInterventions ?: 0
    return (streak * 10) + timeSaved.toInt() + (studySessions * 5) - (interventions * 3)
}

fun getRankFromScore(score: Int): String {
    return when {
        score < 0 -> "😔 Struggling Beginner"
        score < 50 -> "🌱 Getting Started"
        score < 150 -> "🔨 Building Habits"
        score < 300 -> "🎯 Focused Learner"
        score < 500 -> "🧘 Self-Control Master"
        score < 750 -> "⚔️ Digital Warrior"
        score < 1000 -> "🏆 Wellness Champion"
        else -> "👑 Ultimate Controller"
    }
}

fun getNextRankThreshold(score: Int): Pair<String, Int> {
    return when {
        score < 0 -> Pair("🌱 Getting Started", 0)
        score < 50 -> Pair("🔨 Building Habits", 50)
        score < 150 -> Pair("🎯 Focused Learner", 150)
        score < 300 -> Pair("🧘 Self-Control Master", 300)
        score < 500 -> Pair("⚔️ Digital Warrior", 500)
        score < 750 -> Pair("🏆 Wellness Champion", 750)
        score < 1000 -> Pair("👑 Ultimate Controller", 1000)
        else -> Pair("", 0)
    }
}

fun getRankColor(score: Int): Color {
    return when {
        score < 0 -> Color(0xFF9E9E9E)
        score < 50 -> Color(0xFF8BC34A)
        score < 150 -> Color(0xFF2196F3)
        score < 300 -> Color(0xFF9C27B0)
        score < 500 -> Color(0xFF009688)
        score < 750 -> Color(0xFFFF9800)
        score < 1000 -> Color(0xFFFFD700)
        else -> Color(0xFF00FF00)
    }
}

fun categorizeApps(apps: List<ApplicationInfo>, pm: PackageManager): List<AppCategory> {
    val social = mutableListOf<ApplicationInfo>()
    val entertainment = mutableListOf<ApplicationInfo>()
    val games = mutableListOf<ApplicationInfo>()
    val productivity = mutableListOf<ApplicationInfo>()
    val other = mutableListOf<ApplicationInfo>()
    
    apps.forEach { app ->
        val pkg = app.packageName.lowercase()
        when {
            pkg.contains("facebook") || pkg.contains("instagram") || pkg.contains("twitter") || pkg.contains("snapchat") || pkg.contains("tiktok") -> social.add(app)
            pkg.contains("youtube") || pkg.contains("netflix") || pkg.contains("spotify") -> entertainment.add(app)
            (app.flags and ApplicationInfo.FLAG_IS_GAME) != 0 -> games.add(app)
            pkg.contains("gmail") || pkg.contains("outlook") || pkg.contains("slack") -> productivity.add(app)
            else -> other.add(app)
        }
    }
    
    val categories = mutableListOf<AppCategory>()
    if (social.isNotEmpty()) categories.add(AppCategory("Social Media", social.sortedBy { it.loadLabel(pm).toString() }))
    if (entertainment.isNotEmpty()) categories.add(AppCategory("Entertainment", entertainment.sortedBy { it.loadLabel(pm).toString() }))
    if (games.isNotEmpty()) categories.add(AppCategory("Games", games.sortedBy { it.loadLabel(pm).toString() }))
    if (productivity.isNotEmpty()) categories.add(AppCategory("Productivity", productivity.sortedBy { it.loadLabel(pm).toString() }))
    if (other.isNotEmpty()) categories.add(AppCategory("Other", other.sortedBy { it.loadLabel(pm).toString() }))
    
    return categories
}

fun getLocalQuote(context: Context): String {
    return try {
        val inputStream = context.resources.openRawResource(R.raw.quotes)
        val json = inputStream.bufferedReader().use { it.readText() }
        val jsonObject = JSONObject(json)
        val quotesArray = jsonObject.getJSONArray("quotes")
        val randomIndex = (0 until quotesArray.length()).random()
        quotesArray.getString(randomIndex)
    } catch (_: Exception) {
        getDailyMotivation()
    }
}

@Suppress("unused")
fun isServiceEnabled(context: Context): Boolean {
    val enabledServices = android.provider.Settings.Secure.getString(
        context.contentResolver,
        android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false
    return enabledServices.contains(context.packageName) && enabledServices.contains("ShortStopService")
}

fun getDailyMotivation(): String {
    val quotes = listOf(
        "Every moment you resist is a victory.",
        "Your future self will thank you.",
        "Small steps lead to big changes.",
        "You're stronger than your urges.",
        "Focus on progress, not perfection.",
        "Discipline is choosing what you want most over what you want now.",
        "The pain of discipline is far less than the pain of regret.",
        "You don't have to be great to start, but you have to start to be great.",
        "Success is the sum of small efforts repeated day in and day out.",
        "Your only limit is you.",
        "Don't watch the clock; do what it does. Keep going.",
        "The secret of getting ahead is getting started.",
        "It's not about having time. It's about making time.",
        "You are in control of your attention.",
        "Break the scroll, build your soul.",
        "Real life happens offline.",
        "Your phone is a tool, not a master.",
        "Digital detox, mental clarity.",
        "Less screen time, more life time.",
        "Be present in the moment.",
        "Mindful scrolling leads to mindful living.",
        "Choose intention over distraction.",
        "Your goals are waiting for you offline.",
        "Reclaim your time, reclaim your life.",
        "The best version of you is not on your phone.",
        "Disconnect to reconnect with yourself.",
        "Every 'no' to distraction is a 'yes' to your dreams.",
        "You're building habits, not just passing time.",
        "Self-control is self-care.",
        "Your attention is your most valuable asset."
    )
    return quotes.random()
}
