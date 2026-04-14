# Changelog

All notable changes to ShortStop will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2025-07-14

### 🎉 Initial Release

#### Added
- Smart intervention system with 7-second trigger threshold
- 30-second motivational overlay with 100 local quotes
- Study Mode with 25-minute focused sessions
- App blocking for Social Media, Entertainment, and Other categories
- Progress tracking: streaks, time saved, study sessions, interventions
- Rank progression system (😔 Struggling Beginner → 👑 Ultimate Controller)
- Category filter with "All" option
- Statistics dashboard with vertical StatCard layout
- Onboarding flow with permission setup
- Settings panel with customization options
- 100% offline functionality with local quote system
- Privacy-first architecture with zero data collection
- Custom app icon with breaking chain design
- Light theme UI with Material Design 3

#### Technical
- Kotlin with Jetpack Compose UI
- Room database (SQLCipher encrypted) for local storage
- ForegroundService + UsageStatsManager for app detection
- ProGuard optimization for release builds
- App Bundle splits for size optimization
- Min SDK 26 (Android 8.0), Target SDK 35 (Android 15)

#### Privacy & Security
- No internet permission
- No third-party services
- No analytics or tracking
- All data stored locally
- GDPR and CCPA compliant

### 🐛 Bug Fixes (Pre-release)
- Fixed rank calculation formula (interventions now add points instead of subtract)
- Fixed StatCard number wrapping on narrow screens
- Fixed category header visibility with "All" filter
- Restored missing `isServiceEnabled()` function
- Removed unused network imports

### 🔧 Optimizations
- ProGuard with 5 optimization passes
- Logging removal in release builds
- Resource shrinking enabled
- App Bundle splits for language, density, and ABI
- Expected APK size: 15-18 MB
- Expected Play Store download: 8-12 MB

---

## Version History

### Versioning Scheme

- **Major (X.0.0)**: Breaking changes, major features
- **Minor (1.X.0)**: New features, backwards compatible
- **Patch (1.0.X)**: Bug fixes, minor improvements

### Planned Features (Future Versions)

#### v1.1.0 (Potential)
- Custom intervention duration settings
- Custom trigger threshold settings
- Export/import statistics
- Weekly/monthly reports
- Custom quote additions
- Widget support

#### v1.2.0 (Potential)
- Schedule-based blocking (e.g., block during work hours)
- App usage time limits
- Daily goals and challenges
- Achievement system
- Dark theme option (if requested)

#### v2.0.0 (Potential)
- Complete UI redesign
- Advanced analytics
- Habit tracking integration
- Focus session templates
- Parental controls

---

## Release Notes Template

```markdown
## [X.Y.Z] - YYYY-MM-DD

### Added
- New features

### Changed
- Changes to existing functionality

### Deprecated
- Soon-to-be removed features

### Removed
- Removed features

### Fixed
- Bug fixes

### Security
- Security improvements
```

---

**Note**: This changelog will be updated with each release. Check back for the latest changes!
