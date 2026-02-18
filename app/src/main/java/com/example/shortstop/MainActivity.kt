package com.example.shortstop

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val prefs = getSharedPreferences("shortstop_prefs", Context.MODE_PRIVATE)
        
        // Mark permissions as acknowledged on first launch
        if (!prefs.contains("permissions_checked")) {
            prefs.edit().putBoolean("permissions_checked", true).apply()
        }

        // Schedule periodic service monitoring
        val workRequest = PeriodicWorkRequestBuilder<ServiceMonitorWorker>(
            15, TimeUnit.MINUTES 
        ).build()
        WorkManager.getInstance(this).enqueue(workRequest)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainAppScreen()
                }
            }
        }
    }
}