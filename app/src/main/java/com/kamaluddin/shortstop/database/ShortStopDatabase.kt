package com.kamaluddin.shortstop.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import net.zetetic.database.sqlcipher.SQLiteDatabase
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
@Database(
    entities = [UserStatsEntity::class, BlockedAppEntity::class, AppUsageEntity::class, HourlyInterventionEntity::class],
    version = 5,
    exportSchema = false
)
abstract class ShortStopDatabase : RoomDatabase() {
    
    abstract fun dao(): ShortStopDao
    
    companion object {
        @Volatile
        private var INSTANCE: ShortStopDatabase? = null
        
        fun getDatabase(context: Context): ShortStopDatabase {
            return INSTANCE ?: synchronized(this) {
                val passphrase = getOrCreateDbKey(context)
                val factory = SupportOpenHelperFactory(passphrase)
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ShortStopDatabase::class.java,
                    "shortstop_database"
                )
                //.openHelperFactory(factory)
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                .build()
                INSTANCE = instance
                instance
            }
        }

        private fun getOrCreateDbKey(context: Context): ByteArray {
            val keyAlias = "shortstop_db_key"
            val keyStore = java.security.KeyStore.getInstance("AndroidKeyStore").also { it.load(null) }
            if (!keyStore.containsAlias(keyAlias)) {
                val keyGen = javax.crypto.KeyGenerator.getInstance(
                    android.security.keystore.KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore"
                )
                keyGen.init(
                    android.security.keystore.KeyGenParameterSpec.Builder(
                        keyAlias,
                        android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or
                        android.security.keystore.KeyProperties.PURPOSE_DECRYPT
                    )
                    .setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
                )
                keyGen.generateKey()
            }
            // Derive a stable 32-byte passphrase by encrypting a fixed nonce with the Keystore key
            val secretKey = (keyStore.getEntry(keyAlias, null) as java.security.KeyStore.SecretKeyEntry).secretKey
            val prefs = context.getSharedPreferences("shortstop_db_meta", Context.MODE_PRIVATE)
            val ivB64 = prefs.getString("db_iv", null)
            val encB64 = prefs.getString("db_enc", null)
            if (ivB64 != null && encB64 != null) {
                val cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding")
                val iv = android.util.Base64.decode(ivB64, android.util.Base64.NO_WRAP)
                cipher.init(
                    javax.crypto.Cipher.DECRYPT_MODE, secretKey,
                    javax.crypto.spec.GCMParameterSpec(128, iv)
                )
                return cipher.doFinal(android.util.Base64.decode(encB64, android.util.Base64.NO_WRAP))
            }
            // First run: generate random 32-byte key material, encrypt and store it
            val keyMaterial = ByteArray(32).also { java.security.SecureRandom().nextBytes(it) }
            val cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, secretKey)
            val encrypted = cipher.doFinal(keyMaterial)
            prefs.edit()
                .putString("db_iv", android.util.Base64.encodeToString(cipher.iv, android.util.Base64.NO_WRAP))
                .putString("db_enc", android.util.Base64.encodeToString(encrypted, android.util.Base64.NO_WRAP))
                .apply()
            return keyMaterial
        }
        // v1→2: added isStudyMode and studyStartTime to blocked_apps
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE blocked_apps ADD COLUMN isStudyMode INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE blocked_apps ADD COLUMN studyStartTime INTEGER NOT NULL DEFAULT 0")
            }
        }
        // v2→3: created hourly_interventions table
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS hourly_interventions (
                        hourKey TEXT NOT NULL PRIMARY KEY,
                        interventionCount INTEGER NOT NULL
                    )"""
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_hourly_interventions_hourKey ON hourly_interventions(hourKey)")
            }
        }
        // v3→4: added dailyExitCount and lastRewardDate to user_stats
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_stats ADD COLUMN dailyExitCount INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE user_stats ADD COLUMN lastRewardDate TEXT NOT NULL DEFAULT ''")
            }
        }
        // v4→5: added pendingRewards to user_stats
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_stats ADD COLUMN pendingRewards INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}
