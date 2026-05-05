package com.kamaluddin.shortstop

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object SecurePreferences {
    private const val FILE_NAME = "shortstop_prefs"
    @Volatile private var instance: SharedPreferences? = null

    fun get(context: Context): SharedPreferences {
        return instance ?: synchronized(this) {
            instance ?: EncryptedSharedPreferences.create(
                context.applicationContext,
                FILE_NAME,
                MasterKey.Builder(context.applicationContext)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build(),
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            ).also { instance = it }
        }
    }
}
