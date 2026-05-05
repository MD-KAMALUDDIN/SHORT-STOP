package com.kamaluddin.shortstop

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kamaluddin.shortstop.database.ShortStopRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.OutputStream

class ShortStopViewModel(app: Application) : AndroidViewModel(app) {

    val repository = ShortStopRepository(app)

    val userStats = repository.userStats
    val blockedApps = repository.blockedApps
    val studyApps = repository.studyApps

    fun toggleBlockedApp(packageName: String, isBlocked: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.toggleBlockedApp(packageName, isBlocked)
        }
    }

    fun activateStudyMode(packageName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deductPoints(50)
            repository.setStudyMode(packageName, System.currentTimeMillis())
        }
    }

    fun claimAllRewards() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.claimAllRewards()
        }
    }

    fun exportData(outputStream: OutputStream) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.exportData(outputStream)
        }
    }

    fun resetAllData(prefs: android.content.SharedPreferences, onDone: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (!com.kamaluddin.shortstop.SessionGuard.isAuthorized(getApplication())) return@launch
                // 1. Stop the service so it doesn't write stale state after reset
                getApplication<Application>().stopService(
                    android.content.Intent(getApplication(), ShortStopService::class.java)
                )
                // 2. Clear all DB tables then close the singleton so next open is fresh
                val db = com.kamaluddin.shortstop.database.ShortStopDatabase
                    .getDatabase(getApplication())
                db.clearAllTables()
                com.kamaluddin.shortstop.database.ShortStopDatabase.clearInstance()
                // 3. Clear prefs last — SessionGuard reads prefs, so clear after DB
                prefs.edit().clear().apply()
                // 4. Notify caller on main thread to restart the activity
                kotlinx.coroutines.withContext(Dispatchers.Main) { onDone() }
            } catch (e: Exception) {
                AppLogger.e("ShortStop", "Reset failed")
            }
        }
    }
}
