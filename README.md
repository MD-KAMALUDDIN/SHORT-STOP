# ShortStop 🛑

**Break free from digital distractions. Reclaim your focus.**

ShortStop is a privacy-first Android app that fights phone addiction by intercepting you the moment you open a distracting app. A full-screen motivational overlay appears after 7 seconds — you cannot skip it without a penalty. Over time the interruptions build awareness and healthier habits.

**No internet. No accounts. No ads. No tracking. Everything stays on your device.**

---

## 🌟 Features

- **Smart Interventions** — 30-second pause overlay after 7 seconds on a blocked app
- **Study Mode** — 25-minute uninterrupted access for legitimate use (costs 50 pts)
- **Reward System** — Earn points for clean exits, daily streaks, and study sessions
- **Pending Rewards** — Rewards accumulate and must be manually claimed from the home screen
- **Progress Tracking** — Streak days, time saved, rank score, achievements
- **App Categories** — Auto-sorted into Social Media, Entertainment, Games, Productivity, Other
- **Emergency Exit** — Skip the overlay for −50 pts (disabled when balance < 50 pts)
- **Cooldown** — Re-opening a blocked app within 3 minutes triggers an instant overlay
- **100% Offline** — No internet permission, all data stays on device
- **Completely Free** — No ads, no subscriptions, no hidden costs

---

## 🔒 Privacy & Security

- **SQLCipher AES-256** — Database encrypted at rest (`sqlcipher-android:4.6.1`)
- **AndroidKeyStore** — AES-256-GCM key generated in hardware, never leaves the device
- **EncryptedSharedPreferences** — All preferences encrypted (AES256-SIV keys, AES256-GCM values) via `security-crypto:1.1.0-alpha06`
- **No internet permission** — Physically cannot make network calls
- **No analytics** — No crash reporting, no tracking services
- **Open source** — Transparent and auditable

---

## 📱 Requirements

- Android 8.0 (API 26) or higher
- **Usage Stats** permission — detects which app is in the foreground via `UsageStatsManager`
- **Display over other apps** permission — shows the intervention overlay

Both permissions are required. The service will not start until `setup_step >= 3` (both granted).

---

## 🚀 Installation

### From Source

```bash
git clone https://github.com/MD-KAMALUDDIN/SHORT-STOP.git
cd SHORT-STOP
```

Open in Android Studio (Hedgehog or newer), then:

```bash
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Release Build

```bash
# APK
./gradlew assembleRelease

# App Bundle (Play Store)
./gradlew bundleRelease
```

---

## 🎯 How It Works

1. Complete onboarding and grant both permissions
2. Toggle on the apps that distract you
3. Open a blocked app → after **7 seconds** a full-screen overlay appears
4. Overlay fades over **30 seconds** showing a motivational message
5. Tap **Emergency Exit** to skip (−50 pts, requires ≥ 50 pts balance) — overlay removed. You gain a 7-second window to finish your task before the overlay re-engages, bypassing the 3-minute cooldown once
6. Or wait for the overlay to fade — 7s countdown restarts
7. Leave the blocked app and stay away for **10 full minutes** → 10 pts added to pending rewards
8. Tap **Claim** on the home screen to move pending rewards to your balance
9. First intervention each day awards an additional **10 pts** streak bonus (added to pending at intervention time)
10. Points accumulate → rank score increases → climb from Sprout to Sovereign

---

## 💎 Points System

### Earning Points

| Action | Amount | When | Destination |
|---|---|---|---|
| Clean exit (stay away 10 min) | +10 pts | After 10 min away from the app | Pending rewards |
| Daily streak bonus (first intervention of the day) | +10 pts | At the moment of first intervention | Pending rewards |
| Complete a study session | +5 pts | When 25-min session ends | Direct balance |

Pending rewards must be claimed manually from the home screen.

### Spending Points

| Action | Cost | Requirement |
|---|---|---|
| Activate Study Mode (25 min) | −50 pts | Balance ≥ 50 |
| Emergency Exit (skip overlay) | −50 pts | Balance ≥ 50 |

> If your balance is below 50 pts, the Emergency Exit button is **disabled and greyed out**. You must wait the full 30 seconds.

### Claiming Rewards

`claimPendingRewards()` runs a single atomic SQL:
```sql
UPDATE user_stats
SET points = points + pendingRewards,
    totalPointsEarned = totalPointsEarned + pendingRewards,
    pendingRewards = 0
WHERE id = 1 AND pendingRewards > 0
```

---

## 🔄 Reward Flow (Detailed)

Understanding exactly when you earn points is important. Here is the precise flow:

### Clean Exit Reward (+10 pts)

```
Open blocked app
    → 7 seconds pass
    → Overlay appears                    ← intervention recorded
    → You leave the app
    → cleanExitDeadline = now + 10 min   ← stored in DB
    → Service polls every 3 seconds
    → 10 minutes pass (while NOT in the app)
    → +10 pts added to pendingRewards    ← reward fires
    → Tap Claim on home screen
    → Points move to balance
```

> **Important:** If you re-open the blocked app before 10 minutes are up, the deadline resets. You must stay away the full 10 minutes from your last exit to earn the reward. The reward also never fires while you are currently inside the blocked app.

### Streak Bonus (+10 pts)

```
First intervention of the day
    → Overlay appears
    → +10 pts added to pendingRewards immediately
    → Tap Claim on home screen
    → Points move to balance
```

> This fires **at intervention time**, not after 10 minutes. It is a separate reward from the clean exit reward. On the same day's first intervention you can earn both: +10 streak bonus immediately, and +10 clean exit reward after 10 minutes away.

### Study Session Reward (+5 pts)

```
Activate Study Mode (costs 50 pts)
    → 25 minutes of uninterrupted access
    → Session ends
    → +5 pts added directly to balance   ← no claiming needed
```

### What you see during testing

| Moment | What happens to points |
|---|---|
| Overlay appears (first intervention today) | +10 pts streak bonus → pendingRewards |
| Overlay appears (not first today) | No points yet |
| You leave the blocked app | No points yet — 10-min clock starts |
| You re-open within 10 min | Clock resets — no reward |
| 10 min pass while away | +10 pts clean exit → pendingRewards |
| You tap Claim | pendingRewards → balance |

---

## 📊 Rank System

**Rank score formula** (from `calculateRankScore()`):
```
score = (currentStreak × 25) + (successfulStudySessions × 15)
      + (totalTimeSavedMinutes / 5) - (totalEmergencyExits × 20)
```
Score is floored at 0 — emergency exits can never push you below zero.

| Score | Rank |
|---|---|
| 0 – 99 | 🌱 Sprout |
| 100 – 299 | 🔨 Apprentice |
| 300 – 749 | 🎯 Focused |
| 750 – 1499 | 🧘 Monk |
| 1500 – 2999 | ⚔️ Sentinel |
| 3000+ | 👑 Sovereign |

---

## 📚 Study Mode

- Costs **50 points** to activate
- Grants **25 minutes** of uninterrupted access — no overlay fires
- When 25 minutes expire, overlay fires immediately
- Completing the session awards **5 points** directly to balance (no claiming needed)
- `studyStartTime` stored in DB — survives service restarts

---

## 🛠️ Technical Details

### Architecture

- **Language**: Kotlin
- **UI**: Jetpack Compose + Material Design 3
- **Pattern**: ViewModel (`AndroidViewModel`) + Repository + Room
- **Database**: Room + SQLCipher 4.6.1 (AES-256 encrypted)
- **Service**: Foreground service (`specialUse`), `UsageStatsManager`, `START_STICKY`
- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 35 (Android 15)
- **DB schema version**: 9

### Key Files

| File | Role |
|---|---|
| `ShortStopViewModel.kt` | Owns repository; exposes Flows and write actions to UI |
| `MainAppScreen.kt` | Entire Compose UI — app list, stats, rank, achievements, settings |
| `ShortStopService.kt` | Foreground service — polls every 3s, triggers overlays, checks reward deadlines on every tick |
| `InterventionOverlay.kt` | System overlay drawn over other apps via `WindowManager`; `canAffordExit` controls button state |
| `ShortStopRepository.kt` | All DB operations; `getOrCreateUserStats()` guarantees single row; `recordEmergencyExit()` atomically deducts points and increments counter |
| `ShortStopDatabase.kt` | Room + SQLCipher setup, `System.loadLibrary("sqlcipher")` explicit load, migrations 1→9, singleton with `clearInstance()` |
| `ShortStopDao.kt` | DAO — atomic SQL for rewards, targeted updates, `updateCleanExitDeadline` |
| `BootReceiver.kt` | Restarts service after reboot (`exported=false`, checks `setup_step >= 3`) |
| `SecurePreferences.kt` | Cached `EncryptedSharedPreferences` singleton (double-checked locking) |
| `SessionGuard.kt` | Guards export — checks `has_completed_onboarding` + `setup_step >= 3` |
| `AppLogger.kt` | `d()` debug-only; `w()` and `e()` always logged; sanitizes `\n`/`\r` to prevent log injection |

### Permissions

| Permission | Purpose |
|---|---|
| `SYSTEM_ALERT_WINDOW` | Draw overlay over other apps |
| `PACKAGE_USAGE_STATS` | Detect foreground app via `UsageStatsManager` |
| `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_SPECIAL_USE` | Keep service alive (specialUse type with Play Store justification property) |
| `POST_NOTIFICATIONS` | Study mode completion notification |
| `VIBRATE` | 500ms haptic feedback when overlay appears |
| `RECEIVE_BOOT_COMPLETED` | Auto-start service after reboot |

---

## ⏱️ Timing Constants

Located in `ShortStopService.kt`:

```kotlin
TRIGGER_THRESHOLD_MS   = 7L * 1000        // 7 seconds  — time on app before overlay
OVERLAY_DURATION_MS    = 30L * 1000       // 30 seconds — overlay fade duration
STUDY_MODE_DURATION_MS = 25L * 60 * 1000 // 25 minutes — study session length
COOLDOWN_PERIOD_MS     = 3L * 60 * 1000  // 3 minutes  — re-open triggers instant overlay
POLL_INTERVAL_MS       = 3L * 1000       // 3 seconds  — foreground app check frequency
```

Clean exit reward deadline: `exitTime + 10 * 60 * 1000` (10 minutes) — stored as `cleanExitDeadline` in `blocked_apps`, checked on every poll tick via `checkPendingRewardDeadlines()`. Reward only fires if the user is **not currently inside the blocked app** at the time the deadline expires.

### Timer Survival

| Timer | Mechanism | Survives service kill |
|---|---|---|
| Intervention threshold | In-memory counter (`accumulatedTime`) | ❌ |
| Overlay auto-hide | `overlayAutoHideRunnable` via Handler | ❌ |
| Study session end | `studyStartTime` timestamp in DB | ✅ |
| Clean exit reward | `cleanExitDeadline` timestamp in DB | ✅ |
| Cooldown | `lastExitTime` timestamp in DB | ✅ |

---

## 🗄️ Database Schema (version 9)

### `user_stats` — always exactly 1 row (id = 1)

| Field | Type | Default | Purpose |
|---|---|---|---|
| `id` | Int | 1 | Fixed primary key |
| `points` | Int | 0 | Spendable balance |
| `currentStreak` | Int | 0 | Consecutive days with interventions |
| `lastInterventionDate` | String | "" | `yyyy-MM-dd` for streak logic |
| `totalInterventions` | Int | 0 | Lifetime count |
| `totalTimeSaved` | Long | 0 | Milliseconds accumulated |
| `successfulStudySessions` | Int | 0 | Lifetime count |
| `totalPointsEarned` | Int | 0 | Historical total |
| `pendingRewards` | Int | 0 | Unclaimed points |
| `totalEmergencyExits` | Int | 0 | Lifetime emergency exits (affects rank score) |

### `blocked_apps`

| Field | Type | Default | Purpose |
|---|---|---|---|
| `packageName` | String | — | Primary key + unique index |
| `isBlocked` | Boolean | — | Active block flag |
| `lastExitTime` | Long | — | Epoch ms of last exit |
| `isStudyMode` | Boolean | false | Study session active |
| `studyStartTime` | Long | 0 | Epoch ms when study started |
| `cleanExitDeadline` | Long | 0 | Epoch ms when reward fires; 0 = none pending |

### `app_usage`

| Field | Type | Purpose |
|---|---|---|
| `id` | Int (auto) | Primary key |
| `packageName` | String | App identifier (indexed) |
| `date` | String | `yyyy-MM-dd` (indexed) |
| `interventions` | Int | Count that day |
| `timeSaved` | Long | Milliseconds saved |
| `studySessions` | Int | Sessions that day |

### `hourly_interventions`

| Field | Type | Purpose |
|---|---|---|
| `hourKey` | String | Primary key — `yyyy-MM-dd-HH` |
| `interventionCount` | Int | Interventions that hour |

### Database Migrations

| Version | Change |
|---|---|
| 1 → 2 | Added `isStudyMode`, `studyStartTime` to `blocked_apps` |
| 2 → 3 | Created `hourly_interventions` table |
| 3 → 4 | Added `dailyExitCount`, `lastRewardDate` to `user_stats` |
| 4 → 5 | Added `pendingRewards` to `user_stats` |
| 5 → 6 | Removed `totalInterventions`, `totalTimeSaved` from `blocked_apps` |
| 6 → 7 | Removed `dailyExitCount`, `lastRewardDate` from `user_stats` |
| 7 → 8 | Added `cleanExitDeadline` to `blocked_apps` |
| 8 → 9 | Added `totalEmergencyExits` to `user_stats` |

---

## 🧪 Testing

### Unit Tests (JVM)
```bash
./gradlew test
```
Covers: `calculateRankScore` (all weights + floor), `getRankFromScore` (all 6 tiers), `deductPoints` (floor at zero), `claimPendingRewards` (add + noop).

### Instrumented Tests (device/emulator)
```bash
./gradlew connectedAndroidTest
```
Covers: DAO operations — `UserStats` (insert, pending rewards, claim), `BlockedApps` (insert, delete, study mode), `AppUsage`, `HourlyInterventions`.

### Manual Test Checklist
- [ ] Onboarding completes and both permissions granted
- [ ] Service starts and notification appears
- [ ] Overlay appears after 7 seconds on a blocked app
- [ ] Emergency Exit button disabled and greyed out when balance < 50 pts
- [ ] Emergency Exit deducts 50 pts, increments `totalEmergencyExits`, restarts 7s countdown
- [ ] Overlay fades naturally after 30s and restarts 7s countdown
- [ ] Streak bonus (+10 pts) appears in pending immediately on first intervention of the day
- [ ] No clean exit reward fires immediately on leaving the app
- [ ] Clean exit reward (+10 pts) appears in pending only after 10 full minutes away
- [ ] Re-opening the blocked app before 10 min resets the deadline — no reward
- [ ] Pending rewards claimed from home screen — balance updates
- [ ] Study mode activates, blocks overlay for 25 min, awards 5 pts directly to balance on completion
- [ ] Cooldown: re-opening blocked app within 3 min triggers instant overlay
- [ ] Streaks persist across app restarts and device reboots
- [ ] Boot receiver restarts service after reboot
- [ ] Reset wipes all data and restarts app to onboarding
- [ ] Unblocking an app cancels its pending reward deadline
- [ ] SQLCipher loads correctly on Samsung/OEM devices (no `UnsatisfiedLinkError`)

---

## 📦 Build

Expected sizes:
- **Debug APK**: ~18 MB
- **Release APK**: ~15 MB
- **Play Store download**: ~8–12 MB (ABI + density splits)

Optimizations:
- ProGuard minification + obfuscation
- Resource shrinking
- App Bundle splits: `arm64-v8a`, `x86_64`, `armeabi-v7a`
- `AppLogger.d()` calls stripped in release builds
- `useLegacyPackaging = true` — SQLCipher `.so` files extracted at install time (required for Samsung/OEM linker compatibility)

---

## 🔧 Known Issues & Notes

### SQLCipher on Samsung / OEM devices
Samsung's native linker cannot load `.so` files directly from compressed APK entries. The fix is two-part and both parts must be present:

1. `useLegacyPackaging = true` in `build.gradle.kts` — extracts `.so` to filesystem at install
2. `System.loadLibrary("sqlcipher")` in `ShortStopDatabase.getDatabase()` — explicit load before Room initializes

If you see `UnsatisfiedLinkError: nativeOpen`, run a clean build:
```bash
./gradlew clean assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
# Verify extraction:
adb shell ls /data/app/com.kamaluddin.shortstop*/lib/arm64/
# Expected: libsqlcipher.so
```

---

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit changes (`git commit -m 'Add amazing feature'`)
4. Push to branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

---

## 📄 License

MIT License — Free to use, modify, and distribute.

---

## 📧 Contact

- **Issues**: [GitHub Issues](https://github.com/MD-KAMALUDDIN/SHORT-STOP/issues)
- **Email**: mdkamaluddin7339@gmail.com

---

**Built with ❤️ for digital wellbeing**

