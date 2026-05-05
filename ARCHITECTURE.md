# ShortStop — Architecture

Technical architecture documentation for developers.

---

## System Overview

```
┌─────────────────────────────────────────┐
│           UI Layer (Compose)            │
│  MainAppScreen, OnboardingActivity      │
│  ShortStopViewModel (AndroidViewModel)  │
└─────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────┐
│         Repository Layer                │
│  ShortStopRepository                    │
│  getOrCreateUserStats() — row guarantee │
└─────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────┐
│         Data Layer (Room + SQLCipher)   │
│  ShortStopDatabase (v9, AES-256)        │
│  ShortStopDao (atomic SQL)              │
│  4 entities                             │
└─────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────┐
│         System Services Layer           │
│  ShortStopService (ForegroundService)   │
│  UsageStatsManager polling every 3s     │
│  WindowManager overlay                  │
└─────────────────────────────────────────┘
```

---

## Core Components

### UI Layer

**`ShortStopViewModel.kt`** (`AndroidViewModel`)
- Single ViewModel owned by `MainActivity`
- Holds the single `ShortStopRepository` instance
- Exposes `userStats`, `blockedApps`, `studyApps` as Flows
- All write actions: `toggleBlockedApp`, `activateStudyMode`, `claimAllRewards`, `exportData`, `resetAllData`
- `resetAllData` stops service → clears DB → clears prefs → restarts activity

**`MainAppScreen.kt`** (Jetpack Compose)
- Receives `vm: ShortStopViewModel = viewModel()`
- App list with category filtering and search
- Claim rewards card, streak display, rank overlay, achievements
- All child composables receive `vm` — no local repository creation

**`MainActivity.kt`**
- Onboarding flow: overlay permission → usage stats disclosure → enable usage stats → main screen
- Permission polling loop (breaks once `setup_step >= 3`)
- Starts `ShortStopService` when reaching main screen

**`OnboardingActivity.kt`**
- 4-page `HorizontalPager` explaining the app
- Sets `has_completed_onboarding = true` on completion

### Service Layer

**`ShortStopService.kt`** (`ForegroundService`, type: `specialUse`)
- Polls `UsageStatsManager.queryUsageStats()` every **3 seconds**
- Detects foreground app changes via `maxByOrNull { lastTimeUsed }`
- Triggers overlay after **7 continuous seconds** on a blocked app
- Checks `cleanExitDeadline` timestamps on every poll tick
- `START_STICKY` — Android restarts if killed
- All state variables are `private` — no public mutable instance

**Key state:**
```kotlin
private var currentApp: String?          // current foreground app
private var accumulatedTime: Long        // ms spent on current app
private var interventionTriggered: Boolean // prevents duplicate triggers
private var monitoredPkg: String         // snapshot — never changes mid-session
```

**`InterventionOverlay.kt`** (`FrameLayout`)
- System window via `WindowManager.TYPE_APPLICATION_OVERLAY`
- `LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS` — covers notch/punch-hole
- `canAffordExit: Boolean` — disables Emergency Exit button when points < 50
- `ValueAnimator.ofFloat(1f, 0f)` over 30s — linear fade
- 500ms haptic feedback on appearance

### Data Layer

**`ShortStopDatabase.kt`** (Room + SQLCipher 4.6.1)
- `System.loadLibrary("sqlcipher")` before `SupportOpenHelperFactory`
- `useLegacyPackaging = true` — `.so` stored uncompressed
- Key: 32 random bytes, encrypted with AndroidKeyStore AES-256-GCM key
- Encrypted key stored in `EncryptedSharedPreferences`
- `@Volatile INSTANCE` singleton with double-checked locking
- `clearInstance()` — closes and nulls for reset flow
- `RoomDatabase.Callback.onCreate()` seeds `user_stats` row

**`ShortStopRepository.kt`**
- `getOrCreateUserStats()` — always returns a valid row, inserts default if missing
- `pointsMutex: Mutex` — all read-modify-write operations are serialised
- `addPendingRewards` / `claimAllRewards` — delegate to atomic SQL (no read needed)
- `recordEmergencyExit()` — atomically deducts 50 pts AND increments `totalEmergencyExits`

**`ShortStopDao.kt`**
- `getUserStats()` → `Flow<UserStatsEntity?>` — for UI observation
- `getUserStatsOnce()` → `suspend UserStatsEntity?` — for one-shot reads
- `getBlockedAppsOnce()` → `suspend List<BlockedAppEntity>` — for one-shot reads
- `addToPendingRewards(amount)` → `pendingRewards = pendingRewards + :amount`
- `claimPendingRewards()` → single atomic SQL moving pending → points

---

## Data Flow

### Intervention Flow

```
Poll tick (every 3s)
    → getForegroundApp() via UsageStatsManager
    → onForegroundAppChanged(pkg)
    → if pkg != currentApp → stopMonitoring(), record exit, reset accumulatedTime
    → if pkg is blocked → startMonitoring(pkg)
    → monitoringRunnable polls every 3s
    → accumulatedTime + elapsed >= 7s
    → interventionTriggered = true (prevents duplicates)
    → stopMonitoring() + resetSessionTime()
    → recordIntervention() on IO thread
    → triggerOverlay(pkg)
    → overlayAutoHideRunnable fires after 30s
    → removeOverlayView() + startMonitoring(pkg)
```

### Clean Exit Reward Flow

```
User exits blocked app
    → updateLastExitTime(pkg, now)
    → cleanExitDeadline = now + 10min stored in DB
    → Every 3s poll tick calls checkPendingRewardDeadlines()
    → if now >= cleanExitDeadline && isBlocked
    → addPendingRewards(10)
    → updateLastExitTime(pkg, 0L)  ← clears deadline
```

### Reset Flow

```
User taps "Delete Everything"
    → SessionGuard.isAuthorized() check
    → stopService()
    → db.clearAllTables()
    → ShortStopDatabase.clearInstance()
    → prefs.edit().clear()
    → startActivity(MainActivity, FLAG_ACTIVITY_CLEAR_TASK)
```

---

## Rank Score Formula

From `calculateRankScore()`:

```kotlin
score = (currentStreak × 25)
      + (successfulStudySessions × 15)
      + (totalTimeSaved / 60000 / 5)   // minutes saved ÷ 5
      - (totalEmergencyExits × 20)
score = max(score, 0)
```

| Score | Rank |
|---|---|
| < 100 | 🌱 Sprout |
| 100–299 | 🔨 Apprentice |
| 300–749 | 🎯 Focused |
| 750–1499 | 🧘 Monk |
| 1500–2999 | ⚔️ Sentinel |
| 3000+ | 👑 Sovereign |

---

## Timer Architecture

| Timer | Type | Duration | Survives kill |
|---|---|---|---|
| Polling | Handler loop | 3s | ❌ |
| Monitoring | Handler loop | 3s | ❌ |
| Overlay auto-hide | Handler one-shot | 30s | ❌ |
| Study end | Handler one-shot | remaining | ❌ (studyStartTime in DB ✅) |
| Clean exit reward | DB timestamp poll | 10 min | ✅ |
| Cooldown | DB timestamp check | 3 min | ✅ |

---

## Security Architecture

```
AndroidKeyStore
    → AES-256-GCM key (alias: shortstop_db_key)
    → Encrypts 32-byte random passphrase
    → Encrypted passphrase stored in EncryptedSharedPreferences
    → Passphrase passed to SupportOpenHelperFactory
    → SQLCipher encrypts entire DB file with AES-256
```

- No plaintext key material ever stored on disk
- `EncryptedSharedPreferences`: AES256-SIV key encryption, AES256-GCM value encryption
- `SessionGuard.isAuthorized()`: checks `has_completed_onboarding && setup_step >= 3`
- `AppLogger`: sanitizes `\n`/`\r` to prevent log injection; `d()` stripped in release

---

## Database Schema (version 9)

### Migrations

| Version | Change |
|---|---|
| 1 → 2 | Added `isStudyMode`, `studyStartTime` to `blocked_apps` |
| 2 → 3 | Created `hourly_interventions` |
| 3 → 4 | Added `dailyExitCount`, `lastRewardDate` to `user_stats` |
| 4 → 5 | Added `pendingRewards` to `user_stats` |
| 5 → 6 | Removed `totalInterventions`, `totalTimeSaved` from `blocked_apps` |
| 6 → 7 | Removed `dailyExitCount`, `lastRewardDate` from `user_stats` |
| 7 → 8 | Added `cleanExitDeadline` to `blocked_apps` |
| 8 → 9 | Added `totalEmergencyExits` to `user_stats` |

---

## Build Configuration

```kotlin
minSdk     = 26   // Android 8.0
targetSdk  = 35   // Android 15
compileSdk = 35
jvmTarget  = "17"

// ABI splits
abiFilters = ["arm64-v8a", "x86_64", "armeabi-v7a"]

// SQLCipher .so must be uncompressed
jniLibs.useLegacyPackaging = true
```

---

## File Structure

```
app/src/main/java/com/kamaluddin/shortstop/
├── MainActivity.kt              — onboarding + permission flow
├── MainAppScreen.kt             — entire Compose UI
├── OnboardingActivity.kt        — first-time setup
├── ShortStopViewModel.kt        — AndroidViewModel, owns repository
├── ShortStopService.kt          — foreground service, overlay logic
├── InterventionOverlay.kt       — system window overlay view
├── AppLogger.kt                 — logging wrapper
├── SecurePreferences.kt         — EncryptedSharedPreferences singleton
├── SessionGuard.kt              — auth guard for sensitive ops
├── BootReceiver.kt              — auto-start after reboot
└── database/
    ├── ShortStopDatabase.kt     — Room + SQLCipher, migrations 1→9
    ├── ShortStopDao.kt          — DAO with atomic SQL
    ├── ShortStopRepository.kt   — data access, mutex-protected writes
    ├── UserStatsEntity.kt       — user_stats table (v9)
    ├── BlockedAppEntity.kt      — blocked_apps table
    ├── AppUsageEntity.kt        — app_usage table
    └── HourlyInterventionEntity.kt — hourly_interventions table
```

---

**Last Updated**: Based on current codebase (DB version 9)
