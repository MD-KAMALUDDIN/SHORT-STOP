# Changelog

All notable changes to ShortStop will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

---

## [1.0.0] - 2025

### 🎉 Initial Release

#### Core Features
- Smart intervention system — 7-second trigger, 30-second motivational overlay
- Emergency Exit button disabled when balance < 50 pts — must wait full 30 seconds
- Study Mode — 25-minute uninterrupted access (costs 50 pts, awards 5 pts on completion)
- Clean exit reward — stay away 10 minutes from a blocked app → +10 pts pending
- Daily streak bonus — first intervention of the day → +10 pts pending
- Pending rewards system — rewards accumulate, manually claimed from home screen
- Cooldown — re-opening a blocked app within 3 minutes triggers instant overlay

#### Rank System
- Formula: `(streak × 25) + (studySessions × 15) + (timeSavedMinutes / 5) - (emergencyExits × 20)`
- 6 ranks: 🌱 Sprout → 🔨 Apprentice → 🎯 Focused → 🧘 Monk → ⚔️ Sentinel → 👑 Sovereign
- Emergency exits penalise rank score (−20 per exit)

#### Architecture
- ViewModel (`AndroidViewModel`) + Repository + Room pattern
- Single `ShortStopRepository` instance owned by ViewModel
- All Flows observed via `collectAsState` — no local repository creation in Composables
- `getOrCreateUserStats()` guarantees single `user_stats` row always exists
- `pointsMutex` serialises all read-modify-write DB operations
- Atomic SQL for `addToPendingRewards` and `claimPendingRewards`
- `recordEmergencyExit()` atomically deducts points and increments counter

#### Security
- SQLCipher AES-256 encrypted database (`sqlcipher-android:4.6.1`)
- AndroidKeyStore-backed encryption key — never leaves hardware
- DB passphrase encrypted with Keystore key, stored in `EncryptedSharedPreferences`
- `System.loadLibrary("sqlcipher")` + `useLegacyPackaging = true` for Samsung compatibility
- No internet permission — physically cannot make network calls

#### Service
- Foreground service type: `specialUse` — no 6-hour timeout
- `START_STICKY` — Android restarts if killed
- `BootReceiver` (`exported=false`) — restarts after device reboot
- All reward timers converted to DB timestamps — survive service kills and reboots
- `cleanExitDeadline` checked on every 3-second poll tick
- No public mutable service instance — state exposed only via `StateFlow`

#### Overlay
- `LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS` — covers notch/punch-hole (API 28+)
- `FLAG_LAYOUT_IN_SCREEN` + `FLAG_LAYOUT_NO_LIMITS` — true full-screen
- Dark mode aware — adapts background and text colors
- `canAffordExit` parameter — disables/greys Emergency Exit when balance < 50

#### Database
- Schema version 9
- 4 tables: `user_stats`, `blocked_apps`, `app_usage`, `hourly_interventions`
- 8 migrations (1→9)
- `cleanExitDeadline` in `blocked_apps` — timestamp-based reward timer
- `totalEmergencyExits` in `user_stats` — feeds rank penalty formula

#### Privacy
- Zero data collection — nothing leaves the device
- No analytics, no crash reporting, no tracking
- All motivational quotes stored locally in `quotes.json`
- Export data as JSON (user-initiated, requires authorization)
- Reset wipes all tables, closes DB singleton, clears prefs, restarts to onboarding

---

## Planned (Future Versions)

- Custom trigger threshold settings
- Schedule-based blocking (block during work hours)
- Weekly/monthly statistics reports
- Dark theme
- Widget support
