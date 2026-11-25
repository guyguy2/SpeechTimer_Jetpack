# AGENTS.md

This file provides guidance to AI coding agents when working with code in this repository.

## Project

Toastmasters speech timer (Android, single `app` module). Kotlin, Jetpack Compose + Material3, Room, DataStore, Koin. `minSdk 31`, `compileSdk 37.1` (`release(37) { minorApiLevel = 1 }`, needed by Compose BOM 2026.11.01), `targetSdk 36`, JVM 17. Source root: `app/src/main/java/com/happypuppy/toastmasterstimer`.

## Commands

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest   # what CI runs on PRs
./gradlew testDebugUnitTest                                                    # unit tests only
./gradlew testDebugUnitTest --tests "com.happypuppy.toastmasterstimer.ui.screens.TimerViewModelTest"
./gradlew testDebugUnitTest --tests "*TimerViewModelTest.methodName"           # single test
./gradlew lintRelease bundleRelease -PversionCode=N -PversionName=X.Y.Z        # release AAB (needs signing config)
```

CI uses JDK 21 (Temurin). CI runs the JVM unit tests and compiles `androidTest` (`assembleDebugAndroidTest`) but does not run the instrumented tests; they are run locally (see Testing).

JDK for local builds: this repo has no daemon JVM pin, so Gradle uses `JAVA_HOME`. The gates for the Wave 0/1 work all ran on JDK 17 (`JAVA_HOME=<jdk17> ./gradlew ...`). Observed on 2026-11-23: on Gradle 9.9.2, `testDebugUnitTest` also succeeded under the Android Studio JBR 25.0.3 and OpenJDK 27.0.1.1 (only that task was tried; lint, assemble and release were run on JDK 17 only).

Use the `android` CLI as needed for Android workflows (`android --help` / `android <cmd> --help`) — e.g. `android run` / `install`, `emulator` / `device`, `preview` (render Compose `@Preview`s without a device), `screen` / `layout` / `input`, `analyze` (APK/AAB inspection), `sdk` / `info`, and `docs`.

## Architecture

- **DI**: Koin, started in `MainApp`. All bindings live in `di/AppModule.kt` (Room DB, DAO, `SpeakerRepository`, `DataStoreManager`, both ViewModels). `app/proguard-rules.pro` explicitly keeps `di.**`, `SpeechTimerViewModel`, `TimerViewModel`, `data.database.**`, and `DateConverters` for R8 — update it if you add new ViewModels or Room `TypeConverter`s.
- **Navigation & startup**: `NavGraph` is defined in `MainActivity.kt` (`startDestination = Screen.Main.route`, no Compose splash screen); routes are the `Screen` sealed class in `navigation/Navigation.kt` plus the `@Serializable` `TimerRoute(speechTypeId, minMinutes, maxMinutes, customLabel)` in `navigation/TimerRoute.kt`.
  - `SpeechTimerViewModel` is Activity-scoped (created in `MainActivity` and passed down); `TimerViewModel` is scoped to the timer destination's `NavBackStackEntry`.
  - `SpeechType` (`model/SpeechType.kt`) defines the four presets and `CUSTOM` with stable `id`s. Note: `SpeechType.label` is currently both the stored Room `speechType` string and the display text (Wave 5 must keep the stored label stable while Wave 4 maps it to `SpeechType.id`).
  - `TimerRoute` carries typed config (`speechTypeId`, `minMinutes`, `maxMinutes`, optional `customLabel`) via `SavedStateHandle` under its property names. `toRoute()` cannot be unit-tested on the JVM because the unit-test `android.jar` returns default values from `Bundle`/`SavedStateHandle`; `TimerRouteNavigationTest` verifies the real round trip on an emulator.
  - **System splash (`core-splashscreen 1.2.0`)**: `Theme.App.Starting` (`values/themes.xml`, `values/colors.xml`, `values-night/colors.xml`, `ic_splash.xml`) stays on screen until `StartupTheme` (`StartupTheme.kt`) resolves the first `DataStoreManager.themeFlow` emission (or `FIRST_READ_TIMEOUT_MS = 2_000L` expires, or an `IOException` falls back to `AppTheme.SYSTEM_DEFAULT`), so the first Compose frame renders with the stored theme without flicker.
- **Timer logic** is split into two layers:
  - `util/TimerThresholds.kt`: pure threshold math. Green starts at min, red at max, yellow at the midpoint, and `maxDurationSeconds` is `redSeconds + 2` for the progress bar. `TimerPhase` is `BEFORE_GREEN/GREEN/YELLOW/RED`.
  - `ui/screens/TimerViewModel.kt`: constructor-driven (`savedStateHandle`, injectable `elapsedRealtimeMs` lambda defaulting to `SystemClock.elapsedRealtime`, optional `initialThresholds`, and injectable `TimerTicker`). Precedence at `init`: restored clock state > `TimerRoute` args in `SavedStateHandle` > `initialThresholds` > default `1..2` minutes. Elapsed time is computed from a monotonic clock (`accumulatedMs` + delta since `startedAtMs`), not by counting ticks, so it must not drift or lose sub-second progress on pause/resume. `TimerTicker` only triggers `syncElapsed()` and its `Job` is cancelled in `pause()`, `reset()`, and `onCleared()`. Phase changes are emitted once each through `phaseAlerts` (a `SharedFlow`), which `TimerScreen` collects to fire vibration/sound.
  - `TimerViewModel` mirrors `accumulatedMs`, `startedAtMs`, the phase and the thresholds into a `SavedStateHandle` (a required constructor parameter, injected by Koin) and rebuilds from it, so a timer survives process death (not a reboot or swipe-away). A restored state sets `isRestored`, which `TimerScreen` shows as "Timer restored" until the user acts. Restart takes a `snapshot()` and offers Undo (`undoReset`) through a snackbar.
  - `alert/AlertPlayer.kt` is the interface plus `dispatchPhaseAlert` (which cue for which phase and setting); `AndroidAlertPlayer` plays vibration with `VibrationAttributes` USAGE_ALARM (`AudioAttributes` below API 33) and sound with alarm `AudioAttributes` plus transient audio focus. JVM tests use `FakeAlertPlayer`.
  - `TimerScreen` also manages `LocalView.current.keepScreenOn` while running, draws the animated phase color before `innerPadding` for edge-to-edge coverage, hosts its snackbar in `Scaffold(snackbarHost = ...)` (a host placed under the content overlay cannot be tapped), asks before leaving with time on the clock, and stops the active sound on reset/dispose. Phase and hint colors live in `ui/screens/PhaseColors.kt` and are covered by a WCAG contrast test.
  - `ui/screens/TimerFace.kt` is the stateless phase-coloured content of the timer (plain values in, callbacks out, no ViewModel, Koin or navigation), so tests can show it at any phase. `ui/screens/PhaseCue.kt` holds the pure mapping: `phaseCue(phase)` gives the label and spoken-phase string resources plus a `PhaseIcon` key (hourglass, check, warning, stop: distinct shapes), and `spokenDuration` turns seconds into "1 minute 4 seconds" through injected plural formatters.
  - **Phase semantics contract** (pinned by `TimerFaceSemanticsTest`): the phase is never color only. A visible chip (icon plus label, for example "Green: minimum reached") is its own semantics node and is one of exactly two polite live regions, so a screen reader hears phase changes and nothing that ticks. The other is the semantics-only node that replaces the chip in pure-color mode (the hide-controls eye toggle): nothing is drawn, but a full-size node with the same description and live region stays in the tree. The time digits keep their text (`01:04`) but expose one spoken description ("1 minute 4 seconds, green phase") and never have a live region. A paused timer shows a steady "Paused" label and dimmed digits (`PausedDigitsAlpha`), no blinking; the Paused row is always laid out (invisible and without semantics unless paused) so the buttons do not move. The progress bar is one node with `progressBarRangeInfo` and the description "Time progress".
- **Custom speech durations**: `util/CustomSpeechRange.kt` clamps start/end so `1 <= start <= 59` and `start < end <= 60`, keeping thresholds distinct. Use its helpers rather than clamping in UI code.
- **Persistence**:
  - Room: a single `speakers` table (saved speech results). Entity, DAO, `AppDatabase` and `SpeakerRepository` all live in `data/database/Database.kt`, but the **package is `data.database.repository`**, not `data.database`. `DateConverters` lives separately in `data/repository/DateConverters.kt` (`data.repository` package).
  - DataStore (`data/preferences/DataStoreManager.kt`): theme, vibrate (default on), sound (default off). Injected directly into composables (`SimpleSpeechTimerTheme`, `SettingsScreen`, `TimerScreen`) via `koinInject()`.
- **Screen ViewModels & utilities**:
  - `SpeechTimerViewModel` wraps the repository, exposes `uiState` plus one-shot `events` (`SpeakerEvent`), and routes DB writes through `runMutation` so failures become `uiState.error` instead of crashes.
  - `AppVersionInfo` (`util/Utils.kt`) reads version info via `PackageManager` instead of `BuildConfig` (`buildConfig` is not enabled).

## Alerts are foreground-only

Owner decision: no foreground service, no exact alarms, no notification permission. Cues are only promised while the timer screen is open; Settings and the timer screen say so. On return after a phase was crossed while away, `TimerViewModel.onAppForegrounded` fires one catch-up cue (skipped when a live alert for that phase fired within the last 2 s, for example when a frozen process wakes up).

Observed on an API 37 emulator, 2026-11-23: about 60 to 90 s after leaving the foreground the process becomes `cached=true isFrozen=true` (`adb shell dumpsys activity processes`). A GREEN cue fired live with usage ALARM, later cues only arrived when the app was reopened. USAGE_ALARM alone does not make background cues reliable; the catch-up cue covers it.

Manual check of vibration attributes: start a 1-2 min timer, press Home, wait past 60 s, then run `adb -s emulator-NNNN shell dumpsys vibrator_manager` and look at the `com.happypuppy.toastmasterstimer` entries under "Recent vibrations". Expect `usage: ALARM` and a status other than `ignored_background` (before the AlertPlayer change it was `ignored_background`, `usage: TOUCH`). Check restore with `adb -s emulator-NNNN shell am kill com.happypuppy.toastmasterstimer` while the app is backgrounded, then relaunch: the time is restored with "Timer restored".

## Testing

JVM unit tests (`app/src/test`, `./gradlew testDebugUnitTest`, 141 tests): `TimerContractTest` 10, `TimerThresholdsTest` 5, `CustomSpeechRangeTest` 5, `FormatTimeTest` 7, `SpeechTypeTest` 10, `TimerRouteTest` 4, `TimerViewModelTest` 11, `TimerViewModelEdgeCasesTest` 16, `TimerViewModelRestoreTest` 13, `TimerViewModelRouteArgsTest` 6, `PhaseAlertDispatchTest` 6, `PhaseCueTest` 6, `PhaseTextContrastTest` 8, `SpeechTimerViewModelTest` 12, `SpeechTimerViewModelFailureModesTest` 8, `SortSpeakersTest` 5, `AppThemeTest` 2, `DateConvertersTest` 3, `StartupThemeTest` 4. Helpers: `MainDispatcherRule`, `FakeSpeakerDao`, `FakeAlertPlayer`. `PhaseTextContrastTest` checks every text color drawn on the phase backgrounds (4.5:1 small text, 3:1 for the dimmed paused digits).

Every `TimerViewModel` test using the default `DelayTimerTicker` must create its ViewModels through the `newVm` helper and wrap the body in `runTimerTest` (see `TimerViewModelEdgeCasesTest`), because `start()` leaves an unbounded tick loop until `reset()` or `onCleared()`. Pre-filled `SavedStateHandle` plus the fake clock covers restore logic; a real kill is the manual `am kill` step above.

Instrumented tests (`app/src/androidTest`, 68 tests) run on an emulator and are never run in CI. `TimerFlowsTest` (17), `TimerRouteNavigationTest` (4), `StartupInstrumentedTest` (1), and `DataStoreManagerTest` (3) drive the real app with the real Koin graph and Room DB and assert on visible text and content descriptions only. The rest are Compose and accessibility tests: `TimerFaceSemanticsTest` (10, `TimerFace` at chosen phases: live regions, spoken digits, Paused, bounds, progress bar), `CustomSpeechSemanticsTest` (3), `SavedDataSemanticsTest` (3), `InputDialogTest` (1, autofocus), `DialogRotationTest` (4, dialogs and the typed name survive `recreate()`), `TimerTitleFontScaleTest` (6) and the accessibility checks below (`TimerAccessibilityTest` 9, `ScreenAccessibilityTest` 7). They wipe saved speeches and write settings: emulator only, never a physical device. Do not run a bare `./gradlew connectedDebugAndroidTest`: it installs on every attached device. Use the explicit form on one emulator:

```bash
./gradlew assembleDebug assembleDebugAndroidTest
adb -s emulator-NNNN install -r -t app/build/outputs/apk/debug/app-debug.apk
adb -s emulator-NNNN install -r -t app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb -s emulator-NNNN shell am instrument -w -r com.happypuppy.toastmasterstimer.test/androidx.test.runner.AndroidJUnitRunner
```

`emulator-NNNN` is the serial of your own emulator: replace NNNN with its port. Parallel agents or sessions use distinct ports (5554 and 5556 in the Tech Lead's runs) and must only touch their own. When two agents or sessions share the AVD, start every instance with `-read-only` (`emulator -avd Pixel_10 -port NNNN -read-only -no-window -no-audio -no-snapshot-save`); without it the second start fails with "Another emulator instance is running". A read-only AVD starts clean each time, so the app has to be installed again. Stylus handwriting (the Gboard overlay) can swallow typed text; `adb shell settings put secure stylus_handwriting_enabled 0` turns it off for the session.

### Accessibility-check tests

`androidx.compose.ui:ui-test-junit4-accessibility` (Compose BOM managed, `androidTestImplementation`) adds `rule.enableAccessibilityChecks()`. The Accessibility Test Framework then runs on every test action and on an explicit `onRoot().tryPerformAccessibilityChecks()` (dialogs are separate windows: use `onAllNodes(isRoot())`). Errors throw `AccessibilityViewCheckException`. Covered, one test per screen or state: Main, Custom speech, Saved Data (empty and with a row), Settings, the save dialog, the back-confirm dialog (`ScreenAccessibilityTest`), and the timer idle, running, paused, each phase, paused in red and pure-color mode (`TimerAccessibilityTest`, which holds two classes). There is no allow-list: all of them pass with no findings. If a new finding appears, fix it or report it; do not suppress it without a narrow, commented `setSuppressingResultMatcher` entry and a note here. ATF text-contrast checks need screenshots and are not what covers contrast here: the JVM WCAG tests are. Run one class on the emulator with `adb -s emulator-NNNN shell am instrument -w -r -e class com.happypuppy.toastmasterstimer.ScreenAccessibilityTest com.happypuppy.toastmasterstimer.test/androidx.test.runner.AndroidJUnitRunner`.

`TimerTitleFontScaleTest` changes the system font scale (`settings put system font_scale`) and resets it to 1.0 in `@After`. After a crashed or killed run, recover with `adb -s emulator-NNNN shell settings put system font_scale 1.0`.

### Strings

New user-facing strings from Wave 2 (phase labels, spoken phases, plurals, button names, stepper names, per-row labels) are in `res/values/strings.xml` and read with `stringResource`. Older literals are still inline; Wave 5 extracts the remaining strings (and the content descriptions still hardcoded elsewhere).

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

## Release and CI/CD (linked repos)

This repo does not hold the release tooling. The shared CI/CD hub is a separate local git repo:

- CI/CD hub: `/Users/guy/dev/android/cicd` (GCP project `happypuppy-play-cicd`, service accounts `play-ci-testing` and `play-ci-production`, keyless WIF auth). Read before any release work:
  - `cicd/app-state.md`: living source of truth (per-app Play state, package names, keystore locations and aliases, what is live on which track).
  - `cicd/workflows/existing-app-onboard-and-release.md`: the repeatable release checklist.
  - `cicd/14-local-and-self-hosted-pipelines.md`: running the CI, release and promote workflows locally.
  - `cicd/release-log.md`: history and lessons. `cicd/AGENTS.md`: workspace rule, `versionName` is always clean semver, never a commit hash.
- Sibling app on the same pipeline: SimpleDayCounter (Simple Day Counter, package com.happypuppy.daycounter) at /Users/guy/dev/android/SimpleDayCounter; same hub, same release workflows, its AGENTS.md points back here and to the hub
- This repo's own workflows are `.github/workflows/ci.yml`, `release-internal.yml` and `promote.yml` (SHA-pinned). Local mirrors live in the hub and take this repo via `--repo-dir`; run them from the hub directory:

```bash
cd /Users/guy/dev/android/cicd
scripts/local-ci.sh --repo-dir /Users/guy/dev/android/SpeechTimer_Jetpack
scripts/local-release-internal.sh --repo-dir /Users/guy/dev/android/SpeechTimer_Jetpack --keystore <path from app-state.md> --alias <alias from app-state.md> --version-code auto --dry-run
scripts/local-promote.sh --engine gplay --package com.happypuppy.toastmasterstimer --to production --action update-rollout --fraction 0.5
```

- Releasing from either app repo works the same way: the hub scripts take the app repo and package as arguments, so a release session can start in this repo or in the sibling and still find the hub and the other app through the paths above.
- Rules: `--dry-run` first. Uploading to Play, promoting, or changing a rollout is outward-facing: only with the owner's explicit yes. Release from a clean, merged `main` unless the owner says otherwise (the local script refuses a dirty tree). Never commit keystores, `keystore.properties` or service-account files; the hub documents where they live, with locations only. Commit subjects feed the Play release notes (see Build, versioning and release above).

## Known gaps

Screens scroll vertically and `TimerScreen` adjusts spacing below `460.dp` height, but there is no dedicated side-by-side landscape layout.
