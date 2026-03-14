package com.example.shortstop

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val prefs = getSharedPreferences("shortstop_prefs", Context.MODE_PRIVATE)
        
        // Set early adopter flag on first run
        if (!prefs.contains("is_early_adopter")) {
            prefs.edit().putBoolean("is_early_adopter", true).apply()
        }
        
        if (!prefs.getBoolean("has_completed_onboarding", false)) {
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
            return
        }

        val workRequest = PeriodicWorkRequestBuilder<ServiceMonitorWorker>(
            15, TimeUnit.MINUTES 
        ).build()
        WorkManager.getInstance(this).enqueue(workRequest)

        setContent {
            com.example.shortstop.ui.theme.ShortStopTheme(darkTheme = false) {
                MaterialTheme {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        val step = remember { mutableStateOf(prefs.getInt("setup_step", 0)) }
                        val declinedPermission = remember { mutableStateOf(prefs.getBoolean("declined_permission", false)) }
                        
                        when {
                            declinedPermission.value -> StandbyModeScreen(
                                onEnableFocusMode = {
                                    declinedPermission.value = false
                                    prefs.edit().putBoolean("declined_permission", false).apply()
                                    step.value = 1
                                    prefs.edit().putInt("setup_step", 1).apply()
                                }
                            )
                            step.value == 0 -> OverlayPermissionScreen(
                                onContinue = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                                        startActivity(intent)
                                    }
                                }
                            )
                            step.value == 1 -> AccessibilityDisclosureScreen(
                                onAgree = {
                                    prefs.edit().putInt("setup_step", 2).apply()
                                    step.value = 2
                                },
                                onDecline = {
                                    prefs.edit().putBoolean("declined_permission", true).apply()
                                    declinedPermission.value = true
                                }
                            )
                            step.value == 2 -> EnableServiceScreen(
                                onContinue = {
                                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                    startActivity(intent)
                                }
                            )
                            else -> MainAppScreen()
                        }
                        
                        LaunchedEffect(Unit) {
                            while (true) {
                                kotlinx.coroutines.delay(1000)
                                val currentStep = prefs.getInt("setup_step", 0)
                                
                                when (currentStep) {
                                    0 -> {
                                        if (canDrawOverlays()) {
                                            prefs.edit().putInt("setup_step", 1).apply()
                                            step.value = 1
                                        }
                                    }
                                    2 -> {
                                        if (isAccessibilityServiceEnabled()) {
                                            prefs.edit().putInt("setup_step", 3).apply()
                                            step.value = 3
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    private fun canDrawOverlays(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }
    }
    
    private fun isAccessibilityServiceEnabled(): Boolean {
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabledServices.contains(packageName) && enabledServices.contains("ShortStopService")
    }
}

@Composable
fun OverlayPermissionScreen(onContinue: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "📱",
            style = MaterialTheme.typography.displayLarge
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "Overlay Permission Required",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "ShortStop needs permission to display overlays on top of other apps.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "This allows us to show intervention screens when you spend too much time on distracting apps.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Enable Overlay Permission")
        }
    }
}

@Composable
fun StandbyModeScreen(onEnableFocusMode: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("⏸️", style = MaterialTheme.typography.displayLarge)
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "Standby Mode",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Text(
                "ShortStop is currently in 'Standby Mode.' To start your focus sessions and block distracting apps, we need your permission to monitor app switches.",
                modifier = Modifier.padding(20.dp),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
        }
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onEnableFocusMode, modifier = Modifier.fillMaxWidth()) {
            Text("Enable Focus Mode")
        }
    }
}

@Composable
fun AccessibilityDisclosureScreen(onAgree: () -> Unit, onDecline: () -> Unit) {
    BackHandler { onDecline() }
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🔒", style = MaterialTheme.typography.displayLarge)
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "Mindful Usage Monitoring",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "ShortStop needs the Accessibility Service permission to detect when you open distracting apps. This allows us to show you a 30-second focus timer to help you stay productive.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color(0xFFE8F5E8))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Privacy Guarantee", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                Text("✅  No data is collected or stored.")
                Text("✅  No data leaves your device.")
                Text("✅  ShortStop works 100% offline.")
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onAgree, modifier = Modifier.fillMaxWidth()) {
            Text("I Agree")
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(onClick = onDecline, modifier = Modifier.fillMaxWidth()) {
            Text("No Thanks")
        }
    }
}

@Composable
fun EnableServiceScreen(onContinue: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "⚙️",
            style = MaterialTheme.typography.displayLarge
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "Enable ShortStop Service",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Final step! Enable the ShortStop accessibility service to start monitoring your app usage.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Instructions:",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "1. Tap the button below\n2. Find 'ShortStop' in the list\n3. Toggle it ON\n4. Confirm the permission\n5. Return to this app",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Open Accessibility Settings")
        }
    }
}