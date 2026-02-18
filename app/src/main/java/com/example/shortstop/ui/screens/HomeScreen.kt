package com.example.shortstop.ui.screens

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.Divider
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.shortstop.viewmodel.DashboardViewModel
import com.example.shortstop.viewmodel.DashboardViewModelFactory
import com.example.shortstop.SettingsRepository
import com.example.shortstop.ui.components.AppIcon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    context: Context,
    settingsRepository: SettingsRepository,
    prefs: SharedPreferences,
    onNavigateToStats: () -> Unit,
    onNavigateToMotivation: () -> Unit,
    onNavigateToRank: () -> Unit,
    onNavigateToFriends: () -> Unit,
    onNavigateToAchievements: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val viewModel: DashboardViewModel = viewModel(
        factory = DashboardViewModelFactory(context.applicationContext as Application, settingsRepository)
    )
    
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    // UI State from ViewModel
    val categories by viewModel.categories.collectAsState()
    val points by viewModel.points.collectAsState()
    val isStudy by viewModel.isStudyMode.collectAsState()
    val cooldownEnd by viewModel.cooldownEndTime.collectAsState()
    val triggerTime by viewModel.triggerTime.collectAsState()
    val pauseDuration by viewModel.pauseDuration.collectAsState()
    
    var timeRemainingStr by remember { mutableStateOf("") }
    
    LaunchedEffect(cooldownEnd) {
        while (cooldownEnd > System.currentTimeMillis()) {
            val diff = cooldownEnd - System.currentTimeMillis()
            val minutes = diff / 1000 / 60
            val seconds = (diff / 1000) % 60
            timeRemainingStr = String.format("%02d:%02d", minutes, seconds)
            kotlinx.coroutines.delay(1000)
        }
        timeRemainingStr = ""
    }
    
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "ShortStop Menu",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.headlineSmall
                )
                Divider()
                NavigationDrawerItem(
                    label = { Text("Home") },
                    selected = true,
                    onClick = { /* Already here */ },
                    icon = { Icon(Icons.Default.Home, null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { Text("Statistics") },
                    selected = false,
                    onClick = onNavigateToStats,
                    icon = { Icon(Icons.Default.Info, null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { Text("Motivation") },
                    selected = false,
                    onClick = onNavigateToMotivation,
                    icon = { Icon(Icons.Default.Star, null) }, // Using Star as placeholder
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { Text("Your Rank") },
                    selected = false,
                    onClick = onNavigateToRank,
                    icon = { Icon(Icons.Default.Face, null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { Text("Friends") },
                    selected = false,
                    onClick = onNavigateToFriends,
                    icon = { Icon(Icons.Default.Person, null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { Text("Achievements") },
                    selected = false,
                    onClick = onNavigateToAchievements,
                    icon = { Icon(Icons.Default.CheckCircle, null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                Divider()
                NavigationDrawerItem(
                    label = { Text("Settings") },
                    selected = false,
                    onClick = onNavigateToSettings,
                    icon = { Icon(Icons.Default.Settings, null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("ShortStop") },
                    navigationIcon = {
                        IconButton(onClick = { 
                            // In a real app we'd use scope.launch { drawerState.open() }
                            // But here we rely on the parent logic or just use the button 
                            // Since we are decoupling, let's just expose the drawer state logic in the lambda?
                            // For simplicity in this refactor step, we'll keep it simple.
                            // However, coroutine scope is needed.
                            kotlinx.coroutines.GlobalScope.launch { drawerState.open() } 
                        }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        Text(
                            text = "💎 $points",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 16.dp)
                        )
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier.padding(padding).fillMaxSize()
            ) {
                // Study Mode Card
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isStudy) Color(0xFFE8F5E8) else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isStudy) "📚 Study Mode Active" else "🎯 Focus Mode",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isStudy) "Intervention delayed to 20 mins" else "5 minute scrolling limit active",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.buyStudyMode() },
                            enabled = !isStudy && points >= 50,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isStudy) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (isStudy) "Study Session in Progress" else "Buy Study Mode (50 pts)")
                        }
                    }
                }
                
                // Cooldown Info
                if (timeRemainingStr.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "⏳ Cooldown: $timeRemainingStr",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = "+50 pts",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                // Settings Accordion (Simplified as Card for now)
                var showSettings by remember { mutableStateOf(false) }
                Card(
                     modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).clickable { showSettings = !showSettings },
                     colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Settings, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Friction Configuration", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(if (showSettings) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, null)
                        }
                        
                        if (showSettings) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Trigger Time: ${triggerTime / 60}m ${triggerTime % 60}s")
                            Slider(
                                value = triggerTime.toFloat(),
                                onValueChange = { viewModel.updateTriggerTime(it.toInt()) },
                                valueRange = 10f..3600f
                            )
                            
                            Text("Pause Duration: ${pauseDuration}s")
                            Slider(
                                value = pauseDuration.toFloat(),
                                onValueChange = { viewModel.updatePauseDuration(it.toInt()) },
                                valueRange = 5f..30f
                            )
                        }
                    }
                }
                
                // App List
                Text(
                    text = "Blocked Apps",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                
                LazyColumn(
                    modifier = Modifier.weight(1f)
                ) {
                    // Flatten categories for simple display or use StickyHeader if available
                    // For now, simple category header + items
                    
                    categories.forEach { category ->
                        if (category.apps.isNotEmpty()) {
                            item {
                                Text(
                                    text = category.name,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                            
                            items(category.apps) { app ->
                                var isBlocked by remember { mutableStateOf(viewModel.isAppBlocked(app.packageName)) }
                                
                                ListItem(
                                    headlineContent = { Text(app.loadLabel(context.packageManager).toString()) },
                                    leadingContent = {
                                        AppIcon(app.loadIcon(context.packageManager), Modifier.size(40.dp))
                                    },
                                    trailingContent = {
                                        Switch(
                                            checked = isBlocked,
                                            onCheckedChange = { 
                                                isBlocked = it
                                                viewModel.toggleAppBlock(app.packageName, it)
                                            }
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}