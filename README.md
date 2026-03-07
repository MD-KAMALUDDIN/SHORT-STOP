# ShortStop 🛑

**Break free from digital distractions. Reclaim your focus.**

ShortStop is a privacy-first Android app that helps you build healthier phone habits by gently interrupting addictive app usage with motivational interventions.

## 🌟 Features

- **Smart Interventions**: 30-second pause overlays when you spend 7+ seconds on distracting apps
- **Study Mode**: 25-minute focused sessions with app blocking
- **Progress Tracking**: Monitor streaks, time saved, and rank progression
- **Category Management**: Organize apps into Social Media, Entertainment, and Other
- **100% Offline**: No internet required, all data stays on your device
- **Completely Free**: No ads, no subscriptions, no hidden costs

## 🔒 Privacy First

- **Zero data collection** - Nothing leaves your device
- **No internet permission** - Works completely offline
- **No third-party services** - No analytics, no tracking
- **Open source** - Transparent and auditable

## 📱 Requirements

- Android 8.0 (API 26) or higher
- Accessibility Service permission (for app detection)
- Overlay permission (for intervention screens)

## 🚀 Installation

### From Source

1. Clone the repository:
```bash
git clone https://github.com/MD-KAMALUDDIN/SHORT-STOP.git
cd SHORT-STOP
```

2. Open in Android Studio (Hedgehog or newer)

3. Build and run:
```bash
./gradlew assembleDebug
```

### Release Build

```bash
# APK
./gradlew assembleRelease

# App Bundle (for Play Store)
./gradlew bundleRelease
```

## 🎯 How It Works

1. **Select Apps**: Choose which apps distract you (TikTok, Instagram, YouTube, etc.)
2. **Enable Service**: Grant accessibility permission for app detection
3. **Get Interrupted**: After 7 seconds on a blocked app, see a 30-second motivational overlay
4. **Build Streaks**: Track your progress and climb the ranks from Novice to Legend

## 📊 Rank System

Your rank is calculated based on:
- **Streak days** × 10 points
- **Time saved** (in minutes)
- **Study sessions** × 5 points
- **Interventions** × 2 points

**Ranks**: 😔 Struggling Beginner → 🌱 Getting Started → 🔨 Building Habits → 🎯 Focused Learner → 🧘 Self-Control Master → ⚔️ Digital Warrior → 🏆 Wellness Champion → 👑 Ultimate Controller

## 🛠️ Technical Details

### Architecture

- **Language**: Kotlin
- **UI**: Jetpack Compose
- **Database**: Room (SQLite)
- **Service**: AccessibilityService for app detection
- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 34 (Android 14)

### Key Components

- `MainAppScreen.kt` - Main UI with Compose
- `ShortStopService.kt` - Accessibility service for app monitoring
- `AppDatabase.kt` - Room database for local storage
- `quotes.json` - 100 motivational quotes

### Permissions Used

- `SYSTEM_ALERT_WINDOW` - Display intervention overlays
- `BIND_ACCESSIBILITY_SERVICE` - Detect app usage
- `FOREGROUND_SERVICE` - Keep monitoring active
- `POST_NOTIFICATIONS` - Study mode notifications
- `VIBRATE` - Haptic feedback
- `RECEIVE_BOOT_COMPLETED` - Auto-start after reboot

## 📝 Configuration

### Timing Constants

Located in `MainAppScreen.kt`:

```kotlin
TRIGGER_THRESHOLD_MS = 7000L        // 7 seconds before intervention
OVERLAY_DURATION_MS = 30000L        // 30 second intervention
STUDY_MODE_DURATION_MS = 1500000L   // 25 minute study sessions
COOLDOWN_PERIOD_MS = 180000L        // 3 minute cooldown
```

### ProGuard Optimization

Release builds use ProGuard with:
- 5 optimization passes
- Logging removal (Log.d, Log.v, Log.i)
- Resource shrinking
- Code obfuscation

## 🧪 Testing

### Debug Build
```bash
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Test Checklist
- [ ] App selection works
- [ ] Accessibility service enables
- [ ] Overlay appears after 7 seconds
- [ ] Study mode blocks apps for 25 minutes
- [ ] Statistics update correctly
- [ ] Streaks persist across app restarts
- [ ] Boot receiver starts service

## 📦 Build Optimization

Expected sizes:
- **APK**: 15-18 MB
- **Play Store download**: 8-12 MB (with App Bundle splits)

Optimizations applied:
- ProGuard minification
- Resource shrinking
- App Bundle splits (language, density, ABI)
- Logging removal in release

## 🤝 Contributing

Contributions welcome! Please:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit changes (`git commit -m 'Add amazing feature'`)
4. Push to branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 License

MIT License - Free to use, modify, and distribute.

## 📧 Contact

- **Issues**: [GitHub Issues](https://github.com/MD-KAMALUDDIN/SHORT-STOP/issues)
- **Email**: mdkamaluddin7339@gmail.com

## 🙏 Acknowledgments

- Motivational quotes from various sources
- Material Design 3 components
- Android Jetpack libraries

---

**Built with ❤️ for digital wellbeing**
