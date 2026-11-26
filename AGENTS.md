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

## Agent Workflow & Tooling

- **Use subagents as needed:** Delegate parallel codebase research, independent code/architecture reviews, or multi-file analysis to subagents to keep the main context focused; pass bounded scope and concrete deliverables.
- **Use the `android` CLI when needed:** Use `android docs`, `android preview`, `android analyze`, `android emulator`/`device`/`run`/`install`, and `android screen`/`layout`/`input` proactively.
- **Use the `Pixel_10` emulator for testing as needed:** Run instrumented/accessibility tests and R8-minified signed release smoke tests on `Pixel_10` before shipping. Always pass `-s emulator-NNNN`, never target a personal phone, and use `-read-only` for parallel instances (`5554` Day Counter, `5556` Speech Timer, `5558` Memory Lights).
- **Always prefer local CI/CD steps over GitHub Actions whenever possible:** Run verification, builds, emulator tests, releases, and promotion locally via `/Users/guy/dev/android/cicd` (`rex/pipeline.lua` and `scripts/local-*.sh`) rather than GitHub Actions.

## Android & UX Best Practices

- **State & lifecycle:** Keep unidirectional data flow with immutable `StateFlow` UI state, pure/testable domain helpers, `rememberSaveable` / `SavedStateHandle` for transient UI state, and lifecycle-aware cleanup.
- **Accessibility & inclusivity:** Ensure interactive targets are at least `48.dp × 48.dp`; never communicate state by color alone (pair with icons, shapes, or text); provide meaningful TalkBack semantics/live regions, WCAG contrast, large font scale (`2.0x`), and reduced-motion (`animator_duration_scale == 0`) support.
- **Strings & localization:** Keep all user-facing strings, plurals, and content descriptions in `res/values/strings.xml` using XML entities (`&#169;`, `&#8211;`, `&#8230;`); never hardcode user-visible literals or leak raw exception messages into UI.
- **UI polish & consistency:** Use Material 3 color roles and typography cleanly, edge-to-edge insets without system-bar clipping, adaptive layouts across portrait/landscape/tablets, and dynamic `AppVersionInfo` (`PackageManager`) in the About dialog.
- **Quality, R8 & Play readiness:** Keep JVM unit tests fast and deterministic; maintain explicit `app/proguard-rules.pro` keep rules and smoke-test signed R8-minified release builds on `Pixel_10`; enforce `targetSdk >= 36`, 16 KB ELF alignment, `ndk.debugSymbolLevel = "SYMBOL_TABLE"`, and clean semantic `versionName` (never commit hashes).

## Architecture

- **DI**: Koin, started in `MainApp`. All bindings live in `di/AppModule.kt` (Room DB, DAO, `SpeakerRepository`, `DataStoreManager`, both ViewModels). `app/proguard-rules.pro` explicitly keeps `di.**`, `SpeechTimerViewModel`, `TimerViewModel`, `data.database.**`, and `DateConverters` for R8 — update it if you add new ViewModels or Room `TypeConverter`s.
- **Navigation & startup**: `NavGraph` is defined in `MainActivity.kt` (`startDestination = Screen.Main.route`, no Compose splash screen); routes are the `Screen` sealed class in `navigation/Navigation.kt` plus the `@Serializable` `TimerRoute(speechTypeId, minMinutes, maxMinutes, customLabel)` in `navigation/TimerRoute.kt`.
  - `SpeechTimerViewModel` is Activity-scoped (created in `MainActivity` and passed down); `TimerViewModel` is scoped to the timer destination's `NavBackStackEntry`.
  - `SpeechType` (`model/SpeechType.kt`) defines the four presets and `CUSTOM` with stable `id`s. `SpeechType.label` stays the stable English string stored in Room; `@StringRes val labelRes: Int` and composable helpers in `ui/components/SpeechTypeDisplay.kt` (`SpeechType.displayTitle`, `TimerRoute.displayTitle`, `speakerSpeechTypeDisplayName`) provide localized display text.
  - `TimerRoute` carries typed config (`speechTypeId`, `minMinutes`, `maxMinutes`, optional `customLabel`) via `SavedStateHandle` under its property names. `toRoute()` cannot be unit-tested on the JVM because the unit-test `android.jar` returns default values from `Bundle`/`SavedStateHandle`; `TimerRouteNavigationTest` verifies the real round trip on an emulator.
  - **System splash (`core-splashscreen 1.2.0`)**: `Theme.App.Starting` (`values/themes.xml`, `values/colors.xml`, `values-night/colors.xml`, `ic_splash.xml`) stays on screen until `StartupTheme` (`StartupTheme.kt`) resolves the first combined `(DataStoreManager.themeFlow, DataStoreManager.dynamicColorEnabledFlow)` emission (or `FIRST_READ_TIMEOUT_MS = 2_000L` expires, or an `IOException` falls back to `StartupThemeState(AppTheme.SYSTEM_DEFAULT, false)`), so the first Compose frame renders with the stored theme and dynamic-color setting without flicker.
  - **Theming & semantic phase tokens (Wave 6)**: `ui/theme/Color.kt`, `ui/theme/Theme.kt`, and `ui/theme/Type.kt` define the complete Material 3 Brand Default light/dark color schemes (Toastmasters-inspired Loyal Blue primary, True Maroon secondary, Warm Gold tertiary) and full typography roles; `values/colors.xml` and `values-night/colors.xml` are pinned to the brand light/dark `background` and `primary`. Dynamic color is opt-in via Settings (`dynamicColorEnabledFlow`, default `false`). `ui/theme/PhaseColors.kt` provides `PhaseColorScheme` / `PhasePalette` via `LocalPhaseColors` (and `LocalIsDarkTheme`) in `SimpleSpeechTimerTheme` for all four `TimerPhase`s (canvas background, on-background, secondary text, chip container, progress track/border/indicator, threshold markers, and primary/secondary control containers), covered by strict WCAG 2.1 JVM contrast tests (`PhaseTextContrastTest`, `BrandColorSchemeTest`).
- **Timer logic** is split into two layers:
  - `util/TimerThresholds.kt`: pure threshold math. Green starts at min, red at max, yellow at the midpoint, and `maxDurationSeconds` is `redSeconds + 2` for the progress bar. `TimerPhase` is `BEFORE_GREEN/GREEN/YELLOW/RED`.
  - `ui/screens/TimerViewModel.kt`: constructor-driven (`savedStateHandle`, injectable `elapsedRealtimeMs` lambda defaulting to `SystemClock.elapsedRealtime`, optional `initialThresholds`, and injectable `TimerTicker`). Precedence at `init`: restored clock state > `TimerRoute` args in `SavedStateHandle` > `initialThresholds` > default `1..2` minutes. Elapsed time is computed from a monotonic clock (`accumulatedMs` + delta since `startedAtMs`), not by counting ticks, so it must not drift or lose sub-second progress on pause/resume. `TimerTicker` only triggers `syncElapsed()` and its `Job` is cancelled in `pause()`, `reset()`, and `onCleared()`. Phase changes are emitted once each through `phaseAlerts` (a `SharedFlow`), which `TimerScreen` collects to fire vibration/sound.
  - `TimerViewModel` mirrors `accumulatedMs`, `startedAtMs`, the phase and the thresholds into a `SavedStateHandle` (a required constructor parameter, injected by Koin) and rebuilds from it, so a timer survives process death (not a reboot or swipe-away). A restored state sets `isRestored`, which `TimerScreen` shows as "Timer restored" until the user acts. Restart takes a `snapshot()` and offers Undo (`undoReset`) through a snackbar.
  - `alert/AlertPlayer.kt` is the interface (`isSoundPlaying: StateFlow<Boolean>`, `vibrate`, `playSound`, `stopSound`) plus `dispatchPhaseAlert` (which cue for which phase and setting); `AndroidAlertPlayer` plays vibration with `VibrationAttributes` USAGE_ALARM (`AudioAttributes` below API 33) and sound with alarm `AudioAttributes` plus transient audio focus (`OnAudioFocusChangeListener` stops on `AUDIOFOCUS_LOSS` / `AUDIOFOCUS_LOSS_TRANSIENT`). JVM tests use `FakeAlertPlayer`.
  - `TimerScreen` also manages `LocalView.current.keepScreenOn` while running (gated by `DataStoreManager.keepScreenOnEnabledFlow`, default `true`), tints its transparent `TopAppBar` to match the full-bleed animated phase background, dynamically switches system status/navigation bar icon polarity (`WindowCompat.getInsetsController`) based on the active phase background luminance (restoring the normal theme polarity on dispose), hides the `TopAppBar` chrome in speaker-facing mode (`!isUiVisible`), hosts its snackbar in `Scaffold(snackbarHost = ...)` (a host placed under the content overlay cannot be tapped), asks before leaving with time on the clock, and stops the active sound on reset/dispose.
  - `ui/screens/TimerFace.kt` is the stateless phase-coloured content of the timer (plain values in, callbacks out, no ViewModel, Koin or navigation), so tests can show it at any phase. It renders viewport-scaled tabular `"MM:SS"` digits (`ui/screens/TimerLayoutMetrics.kt`), a semantic `PhaseChip` pill, a recolored `TimerProgressBar` driven by `LocalPhaseColors`, a dedicated side-by-side landscape layout (`maxWidth > maxHeight && maxWidth >= 500.dp`), and a floating `>= 48.dp` exit button in speaker-facing mode. `ui/screens/PhaseCue.kt` holds the pure mapping: `phaseCue(phase)` gives the label and spoken-phase string resources plus a `PhaseIcon` key (hourglass, check, warning, stop: distinct shapes), and `spokenDuration` turns seconds into "1 minute 4 seconds" through injected plural formatters.
  - **Phase semantics contract** (pinned by `TimerFaceSemanticsTest`): the phase is never color only. A visible chip (icon plus label, for example "Green: minimum reached") is its own semantics node and is one of exactly two polite live regions, so a screen reader hears phase changes and nothing that ticks. The other is the semantics-only node that replaces the chip in speaker-facing / pure-color mode (the hide-controls eye toggle): no phase text or digits are drawn, a full-size node with the same description and live region stays in the tree, and an accessible `>= 48.dp` "Show controls" button exits speaker mode. The time digits keep their text (`01:04`) but expose one spoken description ("1 minute 4 seconds, green phase") and never have a live region. A paused timer shows a steady "Paused" label and dimmed digits (`PausedDigitsAlpha`), no blinking; the Paused row is always laid out (invisible and without semantics unless paused) so the buttons do not move. The progress bar is one node with `progressBarRangeInfo` and the description "Time progress".
- **Custom speech durations & Main `"Last Custom"` card (Wave 8)**:
  - `util/CustomSpeechRange.kt` defines `@Immutable data class CustomRange(minMinutes, maxMinutes)`, `DEFAULT_MIN_MINUTES = 8`, `DEFAULT_MAX_MINUTES = 10`, and `coerceRange`/`updateStart`/`updateEnd` clamping so `1 <= start <= 59` and `start < end <= 60`, keeping thresholds distinct. Use its helpers rather than clamping in UI code.
  - `ui/components/ExpressiveControls.kt` isolates `@OptIn(ExperimentalMaterial3ExpressiveApi::class)` (`ButtonGroup`, `FilledTonalToggleButton`) behind wrappers that respect `isReducedMotionEnabled` and wrap into `FlowRow` at large font scales (`2.0x`).
  - `CustomSpeechScreen` (`SpeechTimePicker` + stateless `CustomSpeechScreenContent`) hydrates `DataStoreManager.lastCustomRangeFlow` once without overwriting edits/rotation, offers quick presets (`ExpressiveToggleGroup`), a discrete 1..60 min `RangeSlider` (`>= 48.dp` thumbs), hold-to-repeat `MinutePicker` steppers, and a live Green/Yellow/Red thresholds preview (`presetCueTimes` + `PhaseIndicatorIcon`), persisting `setLastCustomRange` on Start. `MainScreen` shows a compact `LastCustomCard` below the 4 `PresetCard`s when `lastCustomRange != null` with one-tap launch into `TimerRoute` and an Edit shortcut to `CustomSpeechScreen`.
- **Persistence**:
  - Room: a single `speakers` table (saved speech results) at schema v3 (`MIGRATION_2_3` adds `speech_type_id`, `min_seconds`, `max_seconds`, `elapsed_seconds`, and `phase_reached`, backfilling preset ids/ranges and `UNKNOWN_SPEECH_TYPE_ID = "unknown"`). Entity, DAO (with `@Update`), `AppDatabase`, `MIGRATION_2_3` and `SpeakerRepository` all live in `data/database/Database.kt`, and the **package is `data.database.repository`**, not `data.database`. `DateConverters` lives separately in `data/repository/DateConverters.kt` (`data.repository` package).
  - DataStore (`data/preferences/DataStoreManager.kt`): theme, dynamic color (default off), keep screen on (default on), vibrate (default on), sound (default off), saved speeches sort order (`savedSortOptionFlow`, default `SortOption.DATE_NEWEST`), and last-used custom speech range (`lastCustomRangeFlow`, default `null`), all with `IOException` fallback. Injected directly into composables (`SimpleSpeechTimerTheme`, `MainScreen`, `SpeechTimePicker`, `SettingsScreen`, `TimerScreen`, `SavedDataScreen`) via `koinInject()`.
- **Screen ViewModels & utilities**:
  - `UiText` (`util/UiText.kt`) carries text out of non-UI code: `DynamicString`, `StringResource(id, args)`, `PluralResource(id, count, args)`, resolved with `asString()` (composable) or `asString(context)`. `SpeakerError` (`ui/screens/SpeakerError.kt`) defines the typed failures (`LoadFailed`, `SaveFailed`, `UpdateFailed`, `DeleteFailed`, `DeleteAllFailed`, `RestoreFailed`).
  - `SpeechTimerViewModel` wraps the repository, exposes `uiState` (`error: SpeakerError?`) plus one-shot `events` (`SpeakerEvent.Deleted(speaker)` / `SpeakerEvent.AllDeleted`), and routes DB writes (`saveSpeaker`, `updateSpeakerName`, `restoreSpeaker`, `deleteSpeaker`, `deleteAllSpeakers`) through `runMutation` so failures become typed `SpeakerError`s instead of crashes or leaked raw exception messages.
  - `SavedDataScreen` displays target range (`formatTargetRangeMinutes`), localized speech type (`speakerSpeechTypeDisplayName`), phase reached (`parseTimerPhase` + `phaseCue`), and locale-aware timestamps (`DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, ...)`) on cards, offers Undo via snackbar on single-row delete, supports editing speaker names via `InputDialog`, persists sort order (`SortOption` with `@StringRes val labelRes: Int`) in DataStore, and formats share text via pure helpers in `SavedDataFormatter.kt` (`SpeakerShareLabels.fromResources`, including `speechTypeLabels`).
  - `AppVersionInfo` (`util/Utils.kt`) reads version info via `PackageManager` instead of `BuildConfig` (`buildConfig` is not enabled).

## Alerts are foreground-only

Owner decision: no foreground service, no exact alarms, no notification permission. Cues are only promised while the timer screen is open; Settings and the timer screen say so. On return after a phase was crossed while away, `TimerViewModel.onAppForegrounded` fires one catch-up cue (skipped when a live alert for that phase fired within the last 2 s, for example when a frozen process wakes up).

Observed on an API 37 emulator, 2026-11-23: about 60 to 90 s after leaving the foreground the process becomes `cached=true isFrozen=true` (`adb shell dumpsys activity processes`). A GREEN cue fired live with usage ALARM, later cues only arrived when the app was reopened. USAGE_ALARM alone does not make background cues reliable; the catch-up cue covers it.

Manual check of vibration attributes: start a 1-2 min timer, press Home, wait past 60 s, then run `adb -s emulator-NNNN shell dumpsys vibrator_manager` and look at the `com.happypuppy.toastmasterstimer` entries under "Recent vibrations". Expect `usage: ALARM` and a status other than `ignored_background` (before the AlertPlayer change it was `ignored_background`, `usage: TOUCH`). Check restore with `adb -s emulator-NNNN shell am kill com.happypuppy.toastmasterstimer` while the app is backgrounded, then relaunch: the time is restored with "Timer restored".

## Testing

JVM unit tests (`app/src/test`, `./gradlew testDebugUnitTest`, 168 tests): `TimerContractTest` 10, `TimerThresholdsTest` 5, `CustomSpeechRangeTest` 5, `FormatTimeTest` 7, `SpeechTypeTest` 11, `TimerRouteTest` 4, `TimerViewModelTest` 11, `TimerViewModelEdgeCasesTest` 16, `TimerViewModelRestoreTest` 13, `TimerViewModelRouteArgsTest` 6, `PhaseAlertDispatchTest` 6, `PhaseCueTest` 6, `PhaseTextContrastTest` 6, `BrandColorSchemeTest` 3, `TimerLayoutMetricsTest` 3, `SpeechTimerViewModelTest` 17, `SpeechTimerViewModelFailureModesTest` 9, `SortSpeakersTest` 6, `SpeakerShareFormatterTest` 10, `AppThemeTest` 2, `DateConvertersTest` 3, `StartupThemeTest` 5, `StringLiteralGuardTest` 4. Helpers: `MainDispatcherRule`, `FakeSpeakerDao`, `FakeAlertPlayer`. `PhaseTextContrastTest` checks every text, button, and non-text UI color pair across all 4 phases in both light and dark schemes (>= 4.5:1 small text/labels/chip/buttons, >= 3:1 large & paused digits and progress bar/markers), and `BrandColorSchemeTest` checks core brand light/dark contrast and `colors.xml` splash alignment.

Every `TimerViewModel` test using the default `DelayTimerTicker` must create its ViewModels through the `newVm` helper and wrap the body in `runTimerTest` (see `TimerViewModelEdgeCasesTest`), because `start()` leaves an unbounded tick loop until `reset()` or `onCleared()`. Pre-filled `SavedStateHandle` plus the fake clock covers restore logic; a real kill is the manual `am kill` step above.

Instrumented tests (`app/src/androidTest`, 86 tests) run on an emulator and are never run in CI. `TimerFlowsTest` (20), `TimerRouteNavigationTest` (4), `StartupInstrumentedTest` (1), `DataStoreManagerTest` (5), `Migration2To3Test` (1, `MigrationTestHelper` v2 -> v3), and `LocalizationResourcesTest` (5) drive the real app/DB/DataStore/resources and assert on visible text, content descriptions, and resource resolution. The rest are Compose and accessibility tests: `TimerFaceSemanticsTest` (10, `TimerFace` at chosen phases: live regions, spoken digits, Paused, bounds, progress bar), `TimerLayoutInstrumentedTest` (4, phone portrait, phone landscape side-by-side, tablet scaled digits, speaker-facing mode), `CustomSpeechSemanticsTest` (3), `SavedDataSemanticsTest` (3), `InputDialogTest` (1, autofocus), `DialogRotationTest` (6, dialogs, typed name, and persisted sort survive `recreate()`), `TimerTitleFontScaleTest` (6) and the accessibility checks below (`TimerAccessibilityTest` 10, `ScreenAccessibilityTest` 7). They wipe saved speeches and write settings: emulator only, never a physical device. Do not run a bare `./gradlew connectedDebugAndroidTest`: it installs on every attached device. Use the explicit form on one emulator:

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

### Strings and localization

All user-facing strings, content descriptions, plurals, and typed errors live in `res/values/strings.xml` and are read with `stringResource` / `pluralStringResource` / `UiText` (Wave 2 + Wave 5). `res/xml/locales_config.xml` declares `en` and is wired via `android:localeConfig="@xml/locales_config"` in `AndroidManifest.xml` (`android:supportsRtl="true"`).

Literal guard: `StringLiteralGuardTest` scans `app/src/main/java` and fails on a string literal with a letter in `Text("...")`, `text|title|description|placeholder|message|actionLabel|onClickLabel|contentDescription|label|supportingText|hint = "..."`, `onClick(label = "...")`, `CustomAccessibilityAction("...")`, and `Text(if (c) "A" else "B")` / `text = if (c) "A" else "B"`. Allow-list: the Compose animation labels `backgroundColor`, `aspectRatio`, and `progressAnimation`.

Pseudolocales (debug builds only, `isPseudoLocalesEnabled = true` in `app/build.gradle.kts`; release builds exclude them): `en-XA` expands and accents text, `ar-XB` mirrors layout/text for RTL. Switch and reset per-app locale on an emulator with:

```bash
adb -s emulator-NNNN shell cmd locale set-app-locales com.happypuppy.toastmasterstimer --locales en-XA   # or ar-XB
adb -s emulator-NNNN shell am force-stop com.happypuppy.toastmasterstimer
adb -s emulator-NNNN shell cmd locale set-app-locales com.happypuppy.toastmasterstimer                   # reset to device locale
```

The setting survives `install -r` and instrumented tests assert English text, so always reset it before running them.

## Database migrations

`AppDatabase` is at version 3 with `exportSchema = true` and explicit `MIGRATION_2_3` registered in `di/AppModule.kt`; schemas (`2.json`, `3.json`) are written to `app/schemas/` and committed. The builder only allows destructive fallback **from v1**. Any schema change to v4+ needs an explicit `Migration`, or users will crash on upgrade. Never move `AppDatabase` out of `com.happypuppy.toastmasterstimer.data.database.repository` without migrating `app/schemas/` and `proguard-rules.pro` in the same step.

## Build, versioning and release

- `versionCode`/`versionName` default to `21` / `2.1.1` in `app/build.gradle.kts` but are overridden by `-PversionCode/-PversionName` or `VERSION_CODE/VERSION_NAME` env vars. Play's highest uploaded code is 21, so any release must be `>= 22`.
- `release-internal.yml` runs on pushes to `main`, `v*` tags, and manual `workflow_dispatch` (which supports a `publish=false` dry run): `versionCode = 10000 + run_number`, `versionName` = tag (without leading `v`), else `2.2.<run_number>`. It runs unit tests, `lintRelease`, `bundleRelease`, an offline `gplay preflight` check (`--max-size 100M --fail-on error`), and publishes to the Play **internal** track via `com.github.triplet.play`, authenticated keylessly through Workload Identity Federation.
- `promote.yml` (manual dispatch) handles `promote`, `update-rollout`, `complete`, and `halt` via `promoteArtifact`, using `PLAY_PROD_SA_EMAIL` when targeting `production`.
- Release builds enable R8, resource shrinking, and `ndk.debugSymbolLevel = "SYMBOL_TABLE"`, and fail the build if `targetSdk < 36` (Play policy gate in `app/build.gradle.kts`).
- Local signing reads `keystore.properties` (git-ignored) or `ANDROID_KEYSTORE_*` env vars; without them the release build is unsigned. Never commit keystores or `keystore.properties`.
- `app/src/main/play/release-notes/` is generated by CI from commit subjects (`wip`, `ci`, `chore`, `docs`, `build`, and `merge` commits are dropped; conventional prefixes and trailing `(Wave N)` are stripped; 500 char cap) and is git-ignored. Write commit subjects accordingly.
- Dependabot is configured in `.github/dependabot.yml`; GitHub Actions are pinned by commit SHA.

## Release and CI/CD (linked repos)

This repo does not hold the release tooling. **Always prefer local CI/CD steps (`rex/pipeline.lua` + `scripts/local-*.sh`) over GitHub Actions whenever possible.** The shared CI/CD hub is a separate local git repo:

- CI/CD hub: `/Users/guy/dev/android/cicd` (GCP project `happypuppy-play-cicd`, service accounts `play-ci-testing` and `play-ci-production`, keyless WIF / `gcloud` ADC auth). Read before any release work:
  - `cicd/app-state.md`: living source of truth (per-app Play state, package names, keystore locations and aliases, what is live on which track).
  - `cicd/workflows/existing-app-onboard-and-release.md`: the repeatable release checklist.
  - `cicd/14-local-and-self-hosted-pipelines.md`: running the CI, release and promote workflows locally.
  - `cicd/release-log.md`: history and lessons. `cicd/AGENTS.md`: workspace rules (clean semver `versionName`, local-first CI/CD policy).
- Sibling apps on the same pipeline:
  - `SimpleDayCounter` (Simple Day Counter, package `com.happypuppy.daycounter`, alias `daycounter`) at `/Users/guy/dev/android/SimpleDayCounter`
  - `MemoryLights` (Memory Lights, package `com.happypuppy.memorylights`, alias `memorylights`) at `/Users/guy/dev/android/MemoryLights`
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

Main, Custom Speech, Saved Data, and Settings screens still scroll vertically on landscape/tablets (adaptive navigation rail / Wave 7-8 polish pending); `TimerScreen` has a dedicated side-by-side landscape layout (`TimerLayoutMetrics.isSideBySide`).
