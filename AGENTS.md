# AGENTS.md

This file provides guidance to AI coding agents when working with code in this repository.

## Project

Toastmasters speech timer (Android, single `app` module). Kotlin, Jetpack Compose + Material3, Room, DataStore, Koin. `minSdk 31`, `compileSdk`/`targetSdk 36`, JVM 17. Source root: `app/src/main/java/com/happypuppy/toastmasterstimer`.

## Commands

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest   # what CI runs on PRs
./gradlew testDebugUnitTest                                                    # unit tests only
./gradlew testDebugUnitTest --tests "com.happypuppy.toastmasterstimer.ui.screens.TimerViewModelTest"
./gradlew testDebugUnitTest --tests "*TimerViewModelTest.methodName"           # single test
./gradlew lintRelease bundleRelease -PversionCode=N -PversionName=X.Y.Z        # release AAB (needs signing config)
```

CI uses JDK 21 (Temurin). Tests are JVM unit tests only (`app/src/test`, with `isReturnDefaultValues = true`, `MainDispatcherRule`, and `FakeSpeakerDao`); `androidTest` is compiled in CI (`assembleDebugAndroidTest`) but has no tests yet and is not run.

Use the `android` CLI as needed for Android workflows (`android --help` / `android <cmd> --help`) — e.g. `android run` / `install`, `emulator` / `device`, `preview` (render Compose `@Preview`s without a device), `screen` / `layout` / `input`, `analyze` (APK/AAB inspection), `sdk` / `info`, and `docs`.

## Architecture

- **DI**: Koin, started in `MainApp`. All bindings live in `di/AppModule.kt` (Room DB, DAO, `SpeakerRepository`, `DataStoreManager`, both ViewModels). `app/proguard-rules.pro` explicitly keeps `di.**`, `SpeechTimerViewModel`, `TimerViewModel`, `data.database.**`, and `DateConverters` for R8 — update it if you add new ViewModels or Room `TypeConverter`s.
- **Navigation**: `NavGraph` is defined in `MainActivity.kt`; routes are the `Screen` sealed class in `navigation/Navigation.kt`.
  - `SpeechTimerViewModel` is Activity-scoped (created in `MainActivity` and passed down); `TimerViewModel` is scoped to the timer destination's `NavBackStackEntry`.
  - The timer route carries its config in the path: `timer_screen/{title}/{minMinutes}/{maxMinutes}`. `SpeechTimerButton` parses min/max from `(\d+) TO (\d+) MIN` in its label, and `TimerScreen` strips the trailing `(...)` from `title` to store `speechType`.
- **Timer logic** is split into two layers:
  - `util/TimerThresholds.kt`: pure threshold math. Green starts at min, red at max, yellow at the midpoint, and `maxDurationSeconds` is `redSeconds + 2` for the progress bar. `TimerPhase` is `BEFORE_GREEN/GREEN/YELLOW/RED`.
  - `ui/screens/TimerViewModel.kt`: drives the clock. Takes an injectable `elapsedRealtimeMs` lambda (defaults to `SystemClock.elapsedRealtime`) so tests can fake time. Elapsed time is computed from a monotonic clock (`accumulatedMs` + delta since `startedAtMs`), not by counting ticks, so it must not drift or lose sub-second progress on pause/resume. The tick loop only triggers `syncElapsed()`. Phase changes are emitted once each through `phaseAlerts` (a `SharedFlow`), which `TimerScreen` collects to fire vibration/sound.
  - `TimerScreen` also manages `LocalView.current.keepScreenOn` while running, draws the animated phase color before `innerPadding` for edge-to-edge coverage, and stops any active `Ringtone` on reset/dispose.
- **Custom speech durations**: `util/CustomSpeechRange.kt` clamps start/end so `1 <= start <= 59` and `start < end <= 60`, keeping thresholds distinct. Use its helpers rather than clamping in UI code.
- **Persistence**:
  - Room: a single `speakers` table (saved speech results). Entity, DAO, `AppDatabase` and `SpeakerRepository` all live in `data/database/Database.kt`, but the **package is `data.database.repository`**, not `data.database`. `DateConverters` lives separately in `data/repository/DateConverters.kt` (`data.repository` package).
  - DataStore (`data/preferences/DataStoreManager.kt`): theme, vibrate (default on), sound (default off). Injected directly into composables (`SimpleSpeechTimerTheme`, `SettingsScreen`, `TimerScreen`) via `koinInject()`.
- **Screen ViewModels & utilities**:
  - `SpeechTimerViewModel` wraps the repository, exposes `uiState` plus one-shot `events` (`SpeakerEvent`), and routes DB writes through `runMutation` so failures become `uiState.error` instead of crashes.
  - `AppVersionInfo` (`util/Utils.kt`) reads version info via `PackageManager` instead of `BuildConfig` (`buildConfig` is not enabled).

## Database migrations

`AppDatabase` is at version 2 with `exportSchema = true`; schemas are written to `app/schemas/` and should be committed. The builder only allows destructive fallback **from v1**. Any schema change to v3+ needs an explicit `Migration`, or users will crash on upgrade.

## Build, versioning and release

- `versionCode`/`versionName` default to `21` / `2.1.1` in `app/build.gradle.kts` but are overridden by `-PversionCode/-PversionName` or `VERSION_CODE/VERSION_NAME` env vars. Play's highest uploaded code is 21, so any release must be `>= 22`.
- `release-internal.yml` runs on pushes to `main`, `v*` tags, and manual `workflow_dispatch` (which supports a `publish=false` dry run): `versionCode = 10000 + run_number`, `versionName` = tag (without leading `v`), else `2.2.<run_number>`. It runs unit tests, `lintRelease`, `bundleRelease`, an offline `gplay preflight` check (`--max-size 100M --fail-on error`), and publishes to the Play **internal** track via `com.github.triplet.play`, authenticated keylessly through Workload Identity Federation.
- `promote.yml` (manual dispatch) handles `promote`, `update-rollout`, `complete`, and `halt` via `promoteArtifact`, using `PLAY_PROD_SA_EMAIL` when targeting `production`.
- Release builds enable R8, resource shrinking, and `ndk.debugSymbolLevel = "SYMBOL_TABLE"`, and fail the build if `targetSdk < 36` (Play policy gate in `app/build.gradle.kts`).
- Local signing reads `keystore.properties` (git-ignored) or `ANDROID_KEYSTORE_*` env vars; without them the release build is unsigned. Never commit keystores or `keystore.properties`.
- `app/src/main/play/release-notes/` is generated by CI from commit subjects (`wip`, `ci`, `chore`, `docs`, `build`, and `merge` commits are dropped; conventional prefixes and trailing `(Wave N)` are stripped; 500 char cap) and is git-ignored. Write commit subjects accordingly.
- Dependabot is configured in `.github/dependabot.yml`; GitHub Actions are pinned by commit SHA.

## Known gaps

Screens scroll vertically and `TimerScreen` adjusts spacing below `460.dp` height, but there is no dedicated side-by-side landscape layout.
