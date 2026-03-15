package com.kamaluddin.shortstop

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val prefs = context.getSharedPreferences("shortstop_prefs", Context.MODE_PRIVATE)
            val setupComplete = prefs.getInt("setup_step", 0) >= 3
            if (setupComplete) {
                ContextCompat.startForegroundService(
                    context,
                    Intent(context, ShortStopService::class.java)
                )
            }
        }
    }
}
