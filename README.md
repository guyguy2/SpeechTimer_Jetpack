# Simple Speech Timer

A clean, distraction-free Android speech timer designed for Toastmasters meetings, presentations, and public speaking practice. Features full-screen Green, Yellow, and Red visual cues, customizable durations, and local speaker history.

Fully free, offline-first, no ads, no accounts, and no internet permission required.

[Get it on Google Play](https://play.google.com/store/apps/details?id=com.happypuppy.toastmasterstimer)

---

## Features

- **Toastmasters presets & Last Custom** - One-tap timers for:
  - **Table Topics** (1 to 2 min)
  - **Evaluation** (2 to 3 min)
  - **Ice Breaker** (4 to 6 min)
  - **Prepared Speech** (5 to 7 min)
  - **Last Custom** quick-start card with one-tap launch and Edit shortcut
- **Custom speech durations** - Choose any start/end range from 1 to 60 minutes with quick presets, a range slider, hold-to-repeat steppers, and live Green (min), Yellow (midpoint), and Red (max) signal previews.
- **Full-screen color cues & speaker-facing mode** - Smoothly animated edge-to-edge Green, Yellow, and Red phase transitions paired with accessible phase icons/labels, a threshold progress bar with milestone markers, side-by-side landscape layout, and a hide-controls toggle for unobstructed visibility.
- **Drift-free monotonic clock** - Backed by `SystemClock.elapsedRealtime()` so pause, resume, and process-death restore preserve sub-second accuracy, with configurable Keep Screen On (default on) during active timing.
- **Vibration & sound alerts** - Configurable haptic feedback (default on, with a distinct double-pulse on Red) and alarm-usage sound alerts (default off) on each color transition, with built-in test/stop controls in Settings.
- **Saved speech history & sharing** - Save speaker name, speech type, target range, elapsed duration, phase reached, and date locally; edit names, undo deletes, sort by date, name, or speech type; share individual or full meeting timing reports via Android's share sheet.
- **Material 3 & accessibility** - Brand Light/Dark/System themes with optional Material You Dynamic Color on Android 12+, full TalkBack semantics with polite phase live regions, WCAG-verified contrast, large font scaling, and reduced-motion support.

## Privacy

All data stays on your device. No accounts, no analytics, no tracking, and no internet permission (only `VIBRATE` for optional haptic alerts).

## Requirements

- Android 12 (API 31) or higher (`targetSdk 36`)

## Tech Stack

- Kotlin + Jetpack Compose
- Material 3 / Material You
- Room (local database with exported schemas)
- DataStore Preferences
- Koin (dependency injection)
- Navigation Compose
- Kotlin Coroutines & Flow

## Building & Testing

Requires JDK 17+ locally (CI runs on Temurin JDK 21).

```bash
# Run unit tests, lint, and assemble debug + test APKs (same as PR CI)
./gradlew testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest

# Unit tests only (JVM, no device required)
./gradlew testDebugUnitTest

# Release APK (R8-minified; unsigned unless keystore.properties or ANDROID_KEYSTORE_* env vars are set)
./gradlew assembleRelease

# Release App Bundle (signed when keystore.properties or ANDROID_KEYSTORE_* env vars are set)
./gradlew lintRelease bundleRelease -PversionCode=22 -PversionName=2.2.0
```

## Architecture

Single-module MVVM + Repository pattern.

```
app/src/main/java/com/happypuppy/toastmasterstimer/
├── MainActivity.kt    # Entry point, system splash preload, edge-to-edge setup, NavGraph, MainScreen
├── MainApp.kt         # Application class initializing Koin
├── StartupTheme.kt    # Holds initial AppTheme from DataStore for splash screen release
├── data/
│   ├── database/      # Room Speaker entity, DAO, AppDatabase, SpeakerRepository
│   ├── preferences/   # DataStoreManager (theme, vibration, sound)
│   └── repository/    # Room DateConverters
├── di/                # Koin module bindings
├── navigation/        # Screen and typed TimerRoute definitions
├── ui/
│   ├── components/    # SpeechTimerButton, MinutePicker, TimerProgressBar, InputDialog, SpeechTimerBottomBar
│   ├── screens/       # TimerScreen, TimerViewModel, CustomSpeechScreen, SavedDataScreen, SettingsScreen, SpeechTimerViewModel
│   └── theme/         # Material 3 / Material You theme, colors, typography
└── util/              # TimerThresholds phase math, CustomSpeechRange clamping, formatting helpers
```

## License

[MIT](LICENSE)
