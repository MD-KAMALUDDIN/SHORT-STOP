# ── Room ─────────────────────────────────────────────────────────────────────
# Keep all database entities, DAOs, and repository so Room's generated code
# can reference them by name at runtime.
-keep class com.kamaluddin.shortstop.database.** { *; }

# Room uses reflection to instantiate entities and call DAO methods.
-keepclassmembers class * extends androidx.room.RoomDatabase {
    public static <methods>;
}
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }

# ── SQLCipher ─────────────────────────────────────────────────────────────────
-keep class net.sqlcipher.** { *; }
-keep class net.sqlcipher.database.** { *; }
-dontwarn net.sqlcipher.**

# ── Kotlin Coroutines ─────────────────────────────────────────────────────────
# Coroutines use reflection for debugging and cancellation; keep internal names.
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# ── Kotlin Serialization / Metadata ──────────────────────────────────────────
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations

# ── Gson ──────────────────────────────────────────────────────────────────────
# Gson uses reflection to serialize/deserialize; keep field names on data classes
# used with Gson (exportData in ShortStopRepository).
-keepclassmembers class com.kamaluddin.shortstop.database.UserStatsEntity { *; }
-keepclassmembers class com.kamaluddin.shortstop.database.BlockedAppEntity { *; }
-dontwarn com.google.gson.**

# ── Jetpack Compose ───────────────────────────────────────────────────────────
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# ── AndroidX Security (EncryptedSharedPreferences) ───────────────────────────
-keep class androidx.security.crypto.** { *; }
-dontwarn androidx.security.crypto.**

# ── Android Keystore / Crypto ─────────────────────────────────────────────────
-keep class android.security.keystore.** { *; }

# ── Service and receiver entry points ────────────────────────────────────────
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends androidx.work.Worker

# ── Remove debug logging in release ──────────────────────────────────────────
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# ── Optimisation ─────────────────────────────────────────────────────────────
-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-verbose
