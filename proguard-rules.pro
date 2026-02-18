# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\Users\mdkam\AppData\Local\Android\Sdk/tools/proguard/proguard-android.txt
# or C:\Users\mdkam\AppData\Local\Android\Sdk/tools/proguard/proguard-android-optimize.txt

# Keep data classes and specific models that might be used by DataStore or Reflection
-keep class com.example.shortstop.DashboardViewModel$AppInfo
-keep class com.example.shortstop.SettingsRepository** { *; }

# Keep service and receiver entry points
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
