package com.example.shortstop.ui

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.shortstop.SettingsRepository
import com.example.shortstop.ui.screens.*

@Composable
fun ShortStopApp(
    context: Context,
    settingsRepository: SettingsRepository,
    prefs: SharedPreferences,
    pm: PackageManager,
    startDestination: String = "home",
    onFinish: () -> Unit
) {
    val navController = rememberNavController()
    
    NavHost(navController = navController, startDestination = startDestination) {
        composable("enable_overlay") {
            EnableOverlayScreen()
        }
        
        composable("accessibility_disclosure") {
            AccessibilityDisclosureScreen(
                onAcknowledge = {
                    navController.navigate("enable_service")
                }
            )
        }

        composable("enable_service") {
            EnableServiceScreen()
            // In a real app we'd observe lifecycle to auto-advance
        }
        
        composable("battery_optimization") {
             DisableBatteryOptimizationScreen()
        }
        
        composable("home") {
            HomeScreen(
                context = context,
                settingsRepository = settingsRepository,
                prefs = prefs,
                onNavigateToStats = { navController.navigate("stats") },
                onNavigateToMotivation = { navController.navigate("motivation") },
                onNavigateToRank = { navController.navigate("rank") },
                onNavigateToFriends = { navController.navigate("friends") },
                onNavigateToAchievements = { navController.navigate("achievements") },
                onNavigateToSettings = { navController.navigate("settings") }
            )
        }
        
        composable("stats") {
            StatisticsScreen(
                prefs = prefs,
                pm = pm,
                onBack = { navController.popBackStack() }
            )
        }
        
        composable("motivation") {
            MotivationScreen(
                onBack = { navController.popBackStack() }
            )
        }
        
        composable("rank") {
            RankScreen(
                prefs = prefs,
                onBack = { navController.popBackStack() }
            )
        }
        
        composable("friends") {
            FriendsScreen(
                prefs = prefs,
                onBack = { navController.popBackStack() }
            )
        }
        
        composable("achievements") {
            AchievementsScreen(
                prefs = prefs,
                onBack = { navController.popBackStack() }
            )
        }
        
        composable("settings") {
            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
