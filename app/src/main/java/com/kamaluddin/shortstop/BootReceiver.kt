package com.kamaluddin.shortstop

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

import com.kamaluddin.shortstop.SecurePreferences

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val prefs = SecurePreferences.get(context)
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
