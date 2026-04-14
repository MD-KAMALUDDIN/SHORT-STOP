package com.kamaluddin.shortstop

import android.content.Context

/**
 * Guards sensitive operations behind two preconditions:
 *  1. Onboarding has been completed.
 *  2. Setup step >= 3 (both permissions granted).
 *
 * Called at the repository layer so every write path is covered
 * regardless of which UI surface triggers it.
 */
object SessionGuard {

    fun isAuthorized(context: Context): Boolean {
        val prefs = SecurePreferences.get(context)
        val onboardingDone = prefs.getBoolean("has_completed_onboarding", false)
        val setupDone = prefs.getInt("setup_step", 0) >= 3
        return onboardingDone && setupDone
    }
}
