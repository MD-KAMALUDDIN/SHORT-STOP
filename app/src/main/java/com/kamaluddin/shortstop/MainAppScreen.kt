package com.kamaluddin.shortstop

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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import org.json.JSONObject

data class AppCategory(val name: String, val apps: List<ApplicationInfo>)

private val defaultUserStats = com.kamaluddin.shortstop.database.UserStatsEntity()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(vm: ShortStopViewModel = viewModel()) {
    val context = LocalContext.current
    val prefs = remember { SecurePreferences.get(context) }
    val userStats by vm.userStats.collectAsState(initial = null)
    val blockedAppsList by vm.blockedApps.collectAsState(initial = emptyList())
    val blockedApps = blockedAppsList.map { it.packageName }.toSet()

    var installedApps by remember { mutableStateOf<List<ApplicationInfo>>(emptyList()) }
    var appCategories by remember { mutableStateOf<List<AppCategory>>(emptyList()) }
    var selectedCategory by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }

    var showMotivation by remember { mutableStateOf(false) }
    var showStats by remember { mutableStateOf(false) }
    var showRank by remember { mutableStateOf(false) }
    var showAchievements by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
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



    val serviceRunning by ShortStopService.isServiceRunning.collectAsState()

    LaunchedEffect(Unit) {
        val pm = context.packageManager
        val apps = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            pm.getInstalledApplications(PackageManager.GET_META_DATA)
                .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 || (it.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0 }
        }
        installedApps = apps
        appCategories = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            categorizeApps(apps, pm)
        }
    }

    LaunchedEffect(userStats) {
        if (userStats == null) return@LaunchedEffect  // wait for real DB row
        val hasInitialized = prefs.getBoolean("has_initialized_stats", false)
        if (!hasInitialized) {
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
                        Text("💎 ${userStats?.points ?: 0}", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MenuButton(
                            if (serviceRunning) "Service ✓" else "Service ✗",
                            Icons.Default.CheckCircle,
                            if (serviceRunning) Color.Green else Color.Red
                        ) {
                            if (!serviceRunning) {
                                val serviceIntent = Intent(context, ShortStopService::class.java)
                                androidx.core.content.ContextCompat.startForegroundService(context, serviceIntent)
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

            val filteredByCategory = when (selectedCategory) {
                "All" -> appCategories
                "Selected" -> listOf(AppCategory("Selected Apps 📌", installedApps.filter { blockedApps.contains(it.packageName) }))
                else -> appCategories.filter { it.name.contains(selectedCategory) }
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
                    ClaimRewardsCard(vm, userStats ?: defaultUserStats)
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
                            AppItemWithStudy(app, blockedApps.contains(app.packageName), userStats?.points ?: 0, vm) { pkg, blocked ->
                                // Write immediately so DB is always consistent
                                vm.toggleBlockedApp(pkg, blocked)
                                scope.launch {
                                    val result = snackbarHostState.showSnackbar(
                                        message = if (blocked) "App blocked" else "App unblocked",
                                        actionLabel = "Undo",
                                        duration = SnackbarDuration.Short
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        vm.toggleBlockedApp(pkg, !blocked)
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
            StatsOverlay(userStats ?: defaultUserStats) { showStats = false }
        }
        if (showRank) {
            androidx.activity.compose.BackHandler { showRank = false }
            RankOverlay(userStats ?: defaultUserStats) { showRank = false }
        }
        if (showAchievements) {
            androidx.activity.compose.BackHandler { showAchievements = false }
            AchievementsOverlay(userStats ?: defaultUserStats, vm) { showAchievements = false }
        }
        if (showHelp) {
            androidx.activity.compose.BackHandler { showHelp = false }
            HelpOverlay { showHelp = false }
        }
        if (showSettings) {
            androidx.activity.compose.BackHandler { showSettings = false }
            SettingsOverlay(vm) { showSettings = false }
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
                        val intent = Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                        context.startActivity(intent)
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
fun ClaimRewardsCard(vm: ShortStopViewModel, userStats: com.kamaluddin.shortstop.database.UserStatsEntity) {
    val pendingRewards = userStats.pendingRewards
    var showClaimDialog by remember { mutableStateOf(false) }

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
            onDismissRequest = { showClaimDialog = false },
            title = { Text("🎉 Rewards Claimed!") },
            text = { Text("You earned $pendingRewards points for staying away from blocked apps!") },
            confirmButton = {
                Button(onClick = {
                    vm.claimAllRewards()
                    showClaimDialog = false
                }) {
                    Text("Awesome!")
                }
            }
        )
    }
}

@Composable
fun AppItemWithStudy(app: ApplicationInfo, isBlocked: Boolean, currentPoints: Int, vm: ShortStopViewModel, onToggle: (String, Boolean) -> Unit) {
    val localContext = LocalContext.current
    var showStudyDialog by remember { mutableStateOf(false) }
    val studyApps by vm.studyApps.collectAsState(initial = emptyList())
    val appLabel by produceState(initialValue = app.packageName.substringAfterLast("."), app.packageName) {
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            app.loadLabel(localContext.packageManager).toString()
        }
    }
    val appIcon by produceState<androidx.compose.ui.graphics.ImageBitmap?>(initialValue = null, app.packageName) {
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            app.loadIcon(localContext.packageManager).toBitmap().asImageBitmap()
        }
    }
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp))) {
                    if (appIcon != null) Image(bitmap = appIcon!!, contentDescription = null, modifier = Modifier.fillMaxSize())
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(appLabel, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Switch(checked = isBlocked, onCheckedChange = { onToggle(app.packageName, it) })
            }
            if (isBlocked) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { showStudyDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = currentPoints >= 50,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Text("Study Mode 25min (50 pts)")
                }
            }
        }
    }
    if (showStudyDialog) {
        AlertDialog(
            onDismissRequest = { showStudyDialog = false },
            title = { Text("ðŸ“š Study Mode") },
            text = { Text("Activate 25-minute Pomodoro session for $appLabel? This will cost 50 points.") },
            confirmButton = {
                Button(onClick = {
                    vm.activateStudyMode(app.packageName)
                    showStudyDialog = false
                }) { Text("Activate") }
            },
            dismissButton = {
                TextButton(onClick = { showStudyDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun MotivationOverlay(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { SecurePreferences.get(context) }
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
fun StatsOverlay(userStats: com.kamaluddin.shortstop.database.UserStatsEntity, onDismiss: () -> Unit) {
    val interventions = userStats.totalInterventions
    val timeSaved = userStats.totalTimeSaved
    val studySessions = userStats.successfulStudySessions
    val streak = userStats.currentStreak
    val points = userStats.points

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
fun RankOverlay(userStats: com.kamaluddin.shortstop.database.UserStatsEntity, onDismiss: () -> Unit) {
    val score = calculateRankScore(userStats)
    val rank = getRankFromScore(score)
    val rankColor = getRankColor(score)
    val (nextRank, nextThreshold) = getNextRankThreshold(score)
    val progress = if (nextThreshold > 0) (score.toFloat() / nextThreshold).coerceIn(0f, 1f) else 1f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Close", tint = Color.White)
                    }
                    Text(
                        "🏆 Your Rank",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // Scrollable content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // Current rank card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = rankColor.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(rank, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = rankColor)
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

                        Spacer(modifier = Modifier.height(24.dp))

                        Text("How to Earn Score", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E8))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                PointEarnRow("🔥 Streak day", "+25\u00A0score")
                                PointEarnRow("📚 Study session", "+15\u00A0score")
                                PointEarnRow("⏱️ 5 min saved", "+1\u00A0score")
                                PointEarnRow("🚨 Emergency exit", "−20\u00A0score")
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Text("All Ranks", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))

                        listOf(
                            Triple("🌱 Sprout",     "0 – 99",       Color(0xFF8BC34A)),
                            Triple("🔨 Apprentice", "100 – 299",    Color(0xFF2196F3)),
                            Triple("🎯 Focused",    "300 – 749",    Color(0xFF9C27B0)),
                            Triple("🧘 Monk",       "750 – 1499",   Color(0xFF009688)),
                            Triple("⚔️ Sentinel",  "1500 – 2999",  Color(0xFFFF9800)),
                            Triple("👑 Sovereign",  "3000+",        Color(0xFFFFD700))
                        ).forEach { (rankName, range, color) ->
                            val isCurrent = getRankFromScore(score) == rankName
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isCurrent) color.copy(alpha = 0.15f) else Color(0xFFF5F5F5)
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        rankName,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isCurrent) color else Color.DarkGray,
                                        fontSize = 15.sp
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(range, fontSize = 13.sp, color = Color.Gray)
                                        if (isCurrent) Text("← you", fontSize = 12.sp, color = color, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFD700).copy(alpha = 0.2f))
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("🎉", fontSize = 56.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "Maximum Rank Achieved!",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = rankColor,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Close")
                }
                Spacer(modifier = Modifier.height(16.dp))
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
                    content = "Earn points by:\n• Clean exit (stay away 10 min): 10 pts\n• Maintaining streaks: 10 pts/day\n• Completing study sessions: 5 pts\n\nSpend points on Study Mode (50 pts)"
                )

                HelpSection(
                    title = "What are Streaks?",
                    content = "A streak counts consecutive days where you had at least one intervention. Keep your streak alive by using the app daily!"
                )

                HelpSection(
                    title = "App Not Working?",
                    content = "1. Go to Settings → Apps → Special app access → Usage access\n2. Enable 'Permit usage access' for ShortStop\n3. Disable Battery Optimization for ShortStop\n4. Restart your device"
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
fun AchievementsOverlay(userStats: com.kamaluddin.shortstop.database.UserStatsEntity, vm: ShortStopViewModel, onDismiss: () -> Unit) {
    val blockedAppsList by vm.blockedApps.collectAsState(initial = emptyList())
    val achievements = getAchievements(userStats, blockedAppsList.size)

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
fun SettingsOverlay(vm: ShortStopViewModel, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { SecurePreferences.get(context) }
    val scope = rememberCoroutineScope()
    var showResetDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            try {
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    vm.exportData(stream)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

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
            onDismissRequest = { showResetDialog = false },
            title = { Text("⚠️ Reset All Data?") },
            text = { Text("This will permanently delete all your progress, points, statistics, and blocked apps. This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        vm.resetAllData(prefs) {
                            // Restart the activity so all in-memory state (ViewModel, Flows, Service) is cleared
                            val ctx = context
                            val intent = android.content.Intent(ctx, MainActivity::class.java)
                            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
                            ctx.startActivity(intent)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Delete Everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("💾 Export Data") },
            text = { Text("Export your data as JSON. You can save it to Google Drive or share it.") },
            confirmButton = {
                Button(
                    onClick = {
                        showExportDialog = false
                        exportLauncher.launch("shortstop_backup_${System.currentTimeMillis()}.json")
                    }
                ) {
                    Text("Export")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}


data class Achievement(val icon: String, val name: String, val description: String, val unlocked: Boolean)

fun getAchievements(userStats: com.kamaluddin.shortstop.database.UserStatsEntity, blockedAppsCount: Int): List<Achievement> {
    val interventions = userStats.totalInterventions
    val studySessions = userStats.successfulStudySessions
    val streak = userStats.currentStreak
    val timeSaved = userStats.totalTimeSaved / 60000
    val totalPoints = userStats.totalPointsEarned

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
        Achievement("🛡️", "App Blocker", "Block 5 or more apps", blockedAppsCount >= 5),
        Achievement("⚔️", "Sentinel", "Reach 1500 rank score", calculateRankScore(userStats) >= 1500)
    )
}


fun calculateRankScore(userStats: com.kamaluddin.shortstop.database.UserStatsEntity?): Int {
    userStats ?: return 0
    val streak = userStats.currentStreak
    val studySessions = userStats.successfulStudySessions
    val timeSavedMinutes = (userStats.totalTimeSaved / 60000).toInt()
    val emergencyExits = userStats.totalEmergencyExits
    return ((streak * 25) + (studySessions * 15) + (timeSavedMinutes / 5) - (emergencyExits * 20))
        .coerceAtLeast(0)
}

fun getRankFromScore(score: Int): String {
    return when {
        score < 100  -> "🌱 Sprout"
        score < 300  -> "🔨 Apprentice"
        score < 750  -> "🎯 Focused"
        score < 1500 -> "🧘 Monk"
        score < 3000 -> "⚔️ Sentinel"
        else         -> "👑 Sovereign"
    }
}

fun getNextRankThreshold(score: Int): Pair<String, Int> {
    return when {
        score < 100  -> Pair("🔨 Apprentice", 100)
        score < 300  -> Pair("🎯 Focused", 300)
        score < 750  -> Pair("🧘 Monk", 750)
        score < 1500 -> Pair("⚔️ Sentinel", 1500)
        score < 3000 -> Pair("👑 Sovereign", 3000)
        else         -> Pair("", 0)
    }
}

fun getRankColor(score: Int): Color {
    return when {
        score < 100  -> Color(0xFF8BC34A)  // green   — Sprout
        score < 300  -> Color(0xFF2196F3)  // blue    — Apprentice
        score < 750  -> Color(0xFF9C27B0)  // purple  — Focused
        score < 1500 -> Color(0xFF009688)  // teal    — Monk
        score < 3000 -> Color(0xFFFF9800)  // orange  — Sentinel
        else         -> Color(0xFFFFD700)  // gold    — Sovereign
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
