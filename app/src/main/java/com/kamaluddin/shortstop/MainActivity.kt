package com.kamaluddin.shortstop

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.isActive

// setup_step: 0=Overlay, 1=UsageStats Disclosure, 2=Enable UsageStats, 3=MainApp
// standby_reason: "overlay" | "usagestats" | ""

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = SecurePreferences.get(this)

        if (!prefs.contains("is_early_adopter")) {
            prefs.edit().putBoolean("is_early_adopter", true).apply()
        }

        if (!prefs.getBoolean("has_completed_onboarding", false)) {
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
            return
        }

        setContent {
            com.kamaluddin.shortstop.ui.theme.ShortStopTheme(darkTheme = false) {
                MaterialTheme {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        val step = remember { mutableStateOf(prefs.getInt("setup_step", 0)) }
                        val standbyReason = remember { mutableStateOf(prefs.getString("standby_reason", "") ?: "") }

                        when {
                            standbyReason.value == "overlay" -> StandbyOverlayScreen(
                                onEnableFocusMode = {
                                    standbyReason.value = ""
                                    prefs.edit().putString("standby_reason", "").apply()
                                    step.value = 0
                                    prefs.edit().putInt("setup_step", 0).apply()
                                }
                            )
                            standbyReason.value == "usagestats" -> StandbyUsageStatsScreen(
                                onEnableFocusMode = {
                                    standbyReason.value = ""
                                    prefs.edit().putString("standby_reason", "").apply()
                                    step.value = 1
                                    prefs.edit().putInt("setup_step", 1).apply()
                                }
                            )
                            step.value == 0 -> OverlayPermissionScreen(
                                onAgree = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                        startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
                                    } else {
                                        prefs.edit().putInt("setup_step", 1).apply()
                                        step.value = 1
                                    }
                                },
                                onDecline = {
                                    prefs.edit().putString("standby_reason", "overlay").apply()
                                    standbyReason.value = "overlay"
                                }
                            )
                            step.value == 1 -> UsageStatsDisclosureScreen(
                                onAgree = {
                                    prefs.edit().putInt("setup_step", 2).apply()
                                    step.value = 2
                                },
                                onDecline = {
                                    prefs.edit().putString("standby_reason", "usagestats").apply()
                                    standbyReason.value = "usagestats"
                                }
                            )
                            step.value == 2 -> EnableUsageStatsScreen(
                                onContinue = {
                                    startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                                }
                            )
                            else -> {
                                // Start service when we reach main screen
                                LaunchedEffect(Unit) {
                                    val serviceIntent = Intent(applicationContext, ShortStopService::class.java)
                                    ContextCompat.startForegroundService(applicationContext, serviceIntent)
                                }
                                MainAppScreen()
                            }
                        }

                        LaunchedEffect(Unit) {
                            while (isActive) {
                                kotlinx.coroutines.delay(1000)
                                val currentStep = prefs.getInt("setup_step", 0)
                                val currentStandby = prefs.getString("standby_reason", "") ?: ""
                                if (currentStandby.isNotEmpty()) continue
                                when (currentStep) {
                                    0 -> if (canDrawOverlays(this@MainActivity)) {
                                        prefs.edit().putInt("setup_step", 1).apply()
                                        step.value = 1
                                    }
                                    2 -> if (hasUsageStatsPermission(this@MainActivity)) {
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

    private fun canDrawOverlays(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Settings.canDrawOverlays(this) else true
    }
}

fun canDrawOverlays(context: Context): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Settings.canDrawOverlays(context) else true

// ─── Step 0: Overlay Permission ───────────────────────────────────────────────

@Composable
fun OverlayPermissionScreen(onAgree: () -> Unit, onDecline: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current

    var waitingForUser by remember { mutableStateOf(false) }

    // Auto-advance when the user returns from Settings with permission granted
    LaunchedEffect(waitingForUser) {
        while (waitingForUser) {
            kotlinx.coroutines.delay(500)
            if (canDrawOverlays(context)) {
                onAgree()
                break
            }
        }
    }

    BackHandler { onDecline() }
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        Text(if (waitingForUser) "⏳" else "📱", style = MaterialTheme.typography.displayLarge)
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            if (waitingForUser) "Waiting for Permission…" else "Display Over Other Apps",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            if (waitingForUser)
                "Enable 'Display over other apps' for ShortStop, then return here."
            else
                "ShortStop needs permission to show a 30-second pause screen when you open a distracting app.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E8))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Privacy Guarantee", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                Text("✅  Only used to display the pause screen")
                Text("✅  No content from other apps is read")
                Text("✅  Works 100% offline — nothing leaves your device")
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = {
                waitingForUser = true
                onAgree()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (waitingForUser) "Re-open Settings" else "I Agree — Grant Permission")
        }
        Spacer(modifier = Modifier.height(12.dp))
        if (!waitingForUser) {
            Button(
                onClick = onDecline,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text("No Thanks")
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

// ─── Step 1: Usage Stats Disclosure ──────────────────────────────────────────

@Composable
fun UsageStatsDisclosureScreen(onAgree: () -> Unit, onDecline: () -> Unit) {
    BackHandler { onDecline() }
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        Text("🔒", style = MaterialTheme.typography.displayLarge)
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "App Usage Monitoring",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "ShortStop uses Android's Usage Stats permission to detect which app is in the foreground, then shows a 30-second pause screen to help you make a mindful choice.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E8))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Privacy Guarantee", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                Text("✅  Only detects which app is in the foreground")
                Text("✅  No keystrokes, content, or personal data is read")
                Text("✅  No data leaves your device — works 100% offline")
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onAgree, modifier = Modifier.fillMaxWidth()) {
            Text("I Agree")
        }
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onDecline,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) {
            Text("No Thanks")
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

// ─── Step 2: Enable Usage Stats ───────────────────────────────────────────────

@Composable
fun EnableUsageStatsScreen(onContinue: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current

    //var permissionGranted by remember { mutableStateOf(false) }
    var waitingForUser by remember { mutableStateOf(false) }

    // Re-check every time the activity resumes (user returns from Settings)
    LaunchedEffect(waitingForUser) {
        if (!waitingForUser) return@LaunchedEffect

        while (isActive) {
            kotlinx.coroutines.delay(500)
            if (hasUsageStatsPermission(context)) {
                onContinue()
                break
            }
        }
    }
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        Text(if (waitingForUser) "⏳" else "⚙️", style = MaterialTheme.typography.displayLarge)
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            if (waitingForUser) "Waiting for Permission…" else "One Last Step",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            if (waitingForUser)
                "Enable 'Permit usage access' for ShortStop in the list, then return here."
            else
                "Allow ShortStop to access app usage data so it can detect when you open a blocked app.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("How to enable:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text("1. Tap 'Open Settings' below")
                Text("2. Find 'ShortStop' in the list")
                Text("3. Toggle 'Permit usage access' ON")
                Text("4. Return to this app — it will continue automatically")
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = {
                waitingForUser = true
                onContinue()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (waitingForUser) "Re-open Settings" else "Open Settings")
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

private fun hasUsageStatsPermission(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
    } else {
        @Suppress("DEPRECATION")
        appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
    }
    return mode == AppOpsManager.MODE_ALLOWED
}

// ─── Standby: Overlay declined ───────────────────────────────────────────────

@Composable
fun StandbyOverlayScreen(onEnableFocusMode: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        Text("⏸️", style = MaterialTheme.typography.displayLarge)
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "ShortStop is in Standby",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Pause screens are currently disabled.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Why this permission matters:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                Text("• Without it, ShortStop cannot show the 30-second pause screen")
                Text("• Your blocked app list is saved and ready")
                Text("• No data is collected while in Standby")
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Grant 'Display over other apps' permission to start your focus sessions.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onEnableFocusMode, modifier = Modifier.fillMaxWidth()) {
            Text("Enable Focus Mode")
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

// ─── Standby: Usage Stats declined ───────────────────────────────────────────

@Composable
fun StandbyUsageStatsScreen(onEnableFocusMode: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        Text("⏸️", style = MaterialTheme.typography.displayLarge)
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "ShortStop is in Standby",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "App monitoring is currently off.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Why this permission matters:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                Text("• Without it, ShortStop cannot detect when you open a blocked app")
                Text("• No interventions will trigger")
                Text("• Your blocked app list is saved and ready")
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Grant 'Usage access' permission to start monitoring and build your focus streak.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onEnableFocusMode, modifier = Modifier.fillMaxWidth()) {
            Text("Enable Focus Mode")
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}
