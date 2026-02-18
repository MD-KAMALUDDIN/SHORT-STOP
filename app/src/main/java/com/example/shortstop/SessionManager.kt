package com.example.shortstop

object SessionManager {

    private var currentPackage: String? = null
    private var startTime: Long = 0

    fun startSession(packageName: String) {
        if (currentPackage == packageName) return

        if (currentPackage != null) {
            endSession()
        }

        currentPackage = packageName
        startTime = System.currentTimeMillis()
    }

    fun endSession() {
        if (currentPackage == null) return
        // Logging removed for production
        currentPackage = null
        startTime = 0
    }

    fun isSessionActive(): Boolean {
        return currentPackage != null
    }

    fun getCurrentPackage(): String? {
        return currentPackage
    }

    fun getStartTime(): Long {
        return startTime
    }
}
