package com.kamaluddin.shortstop

import android.content.Context

object SessionGuard {

    fun isAuthorized(context: Context): Boolean {
        val prefs = SecurePreferences.get(context)
        return prefs.getBoolean("has_completed_onboarding", false)
            && prefs.getInt("setup_step", 0) >= 3
    }
}
