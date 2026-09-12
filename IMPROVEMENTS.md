# SpeechTimer Jetpack - Improvement Recommendations

## 1. Project Overview

Simple Speech Timer is a single-module Android app (`:app` only) that helps Toastmasters members time speeches against configurable green/yellow/red thresholds. Users pick from four preset speech types or configure a custom duration, then run a fullscreen timer that changes background colour at each threshold and optionally vibrates/sounds. Results are saved to a Room database and can be sorted and shared. The tech stack is Kotlin 2.1 + Jetpack Compose (BOM 2025.02) + Room 2.6 + Koin 3.5 + DataStore Preferences + Compose Navigation 2.8, built with AGP 8.8 and KSP. There is no KMP, no Retrofit/Ktor, and no remote data source. Navigation is a single `NavHost` defined directly in `MainActivity.kt`.

---

## 2. Architecture & Module Structure

**Issues:**

### 2.1 Single module with no layering enforcement
Everything lives under `:app`. There are no module boundaries to prevent presentation code from importing data classes directly or bypassing the repository. For a project of this size that is acceptable today, but as features grow (e.g. widgets, wear companion, export) the absence of `:core:data`, `:core:domain`, `:feature:timer`, `:feature:history` modules will make refactoring painful.

Concrete step: even without splitting modules now, enforce the layering in code: data classes must not be imported directly by Composables (see §5).

### 2.2 Domain layer is entirely absent
`Speaker` (the Room entity) is used directly in the UI. There is no domain model, no use-case layer, and no mapper. Business logic (colour threshold computation, time formatting) is spread across `TimerScreen.kt` and `Utils.kt`.

### 2.3 `NavGraph` and `MainScreen` live in `MainActivity.kt`
`app/src/main/java/.../MainActivity.kt` is 213 lines containing the `Activity`, the full `NavHost`, and the main screen Composable. These should be separate files.

### 2.4 No convention plugins or build-logic module
With a single module this is not urgent, but there are no shared Gradle convention plugins, which will be the first thing needed when a second module is added.

### 2.5 Package structure is inconsistent
`Database.kt` lives at `data.database.repository` but contains the entity, DAO, database class, **and** repository - four concerns in one file. `DateConverters.kt` is at `data.repository` (a sibling package) while it belongs to `data.database`.

---

## 3. Dependency Injection

**Mostly fine for the project size, with two concrete issues:**

### 3.1 `fallbackToDestructiveMigration()` is in production DI with no caveat
`app/src/main/java/.../di/AppModule.kt:21` uses `fallbackToDestructiveMigration()` with the comment "For simplicity in development." The database is at version 2, which means it has already been migrated once. Keeping this in production will silently delete all user speech history on any future schema change without warning the user.

```kotlin
// Before (AppModule.kt:18-23)
Room.databaseBuilder(...)
    .fallbackToDestructiveMigration() // For simplicity in development
    .build()

// After
Room.databaseBuilder(...)
    .addMigrations(MIGRATION_2_3)   // add proper migrations going forward
    .build()
// Remove the comment about development once you ship.
```

### 3.2 `DataStoreManager` is injected via `koinInject()` inside Composables
`TimerScreen.kt:130`, `SettingsScreen.kt:66`, and `Theme.kt:44` all call `koinInject<DataStoreManager>()` directly inside Composable functions. This couples Compose code to Koin, makes previews and tests harder, and contradicts the pattern used everywhere else (pass via parameter or ViewModel). `DataStoreManager` should be injected into the ViewModel or passed from the navigation graph entry, not injected mid-tree.

### 3.3 Single shared `ViewModel` instance passed down from `MainActivity`
`MainActivity.kt:62-65` creates one `koinViewModel<ViewModel>()` at the Activity scope and passes it as a parameter through `NavGraph` to every screen. This means the ViewModel's `uiState` - which holds the speaker list - is never scoped to the screen that needs it. The standard pattern is to call `koinViewModel()` (or `hiltViewModel()`) at each `composable { }` destination, letting the navigation back-stack manage the lifecycle.

### 3.4 Commented-out dependency
`app/build.gradle.kts:55` has `// implementation(libs.koin.compose.viewmodel)` commented out. If it is unused, remove it; if it is needed, add it properly.

---

## 4. Presentation Layer (Compose + ViewModels)

### 4.1 ViewModel class is named `ViewModel` (P0)
`app/src/main/java/.../ui/screens/ViewModel.kt:17` declares `class ViewModel(...)`. This name shadows `androidx.lifecycle.ViewModel`, causes confusion in every import, and will silently break IDE auto-complete. It should be `SpeechTimerViewModel` or `SpeakerHistoryViewModel`.

### 4.2 All timer state lives in the Composable, not the ViewModel
`TimerScreen.kt:87-134` owns `ticks`, `isTimerRunning`, `targetBackgroundColor`, `isTimeVisible`, `isUIVisible`, and `previousBackgroundColor` as `remember` state. When the screen is recomposed due to config change (e.g. font-size change while the timer is running, which cannot happen today because orientation is locked, but will when landscape is supported per `MainActivity.kt:59`) all timer state is lost. Timer state should live in the ViewModel.

### 4.3 No state hoisting - business logic inside `LaunchedEffect`
`TimerScreen.kt:148-199` contains a `LaunchedEffect` that runs the tick loop, applies color logic, triggers vibration, and plays sounds - all inside a Composable. This is untestable and violates the single-responsibility rule for Composables. Move the tick loop, threshold evaluation, vibration dispatch, and sound dispatch to the ViewModel (or a dedicated `TimerController` class).

### 4.4 Blinking `LaunchedEffect` leaks on recomposition
`TimerScreen.kt:137-146`: the blinking loop `while(true) { delay(1000); isTimeVisible = !isTimeVisible }` restarts whenever `isTimerRunning` or `ticks` changes. Every tick (every second) the `ticks` key changes, so the `LaunchedEffect` is cancelled and restarted every second - meaning the blink never actually fires because the coroutine is cancelled before the first `delay(1000)` completes.

```kotlin
// Before
LaunchedEffect(isTimerRunning, ticks) {   // ticks changes every second, cancels effect
    if (!isTimerRunning && ticks > 0) {
        while (true) { delay(1000); isTimeVisible = !isTimeVisible }
    }
}

// After: key only on isTimerRunning so the loop runs continuously while paused
LaunchedEffect(isTimerRunning) {
    if (!isTimerRunning && ticks > 0) {
        while (true) { delay(500); isTimeVisible = !isTimeVisible }
    } else {
        isTimeVisible = true
    }
}
```

### 4.5 Snackbar auto-navigate is fragile
`TimerScreen.kt:419-433`: after saving, the code shows a snackbar and then unconditionally navigates to `SavedDataScreen` after a 1.5-second delay, regardless of whether the user dismissed the snackbar or not. The `if (result == SnackbarResult.ActionPerformed)` branch navigates immediately and then the `else` branch **also** waits 1.5 s and navigates - meaning both branches navigate. Use `when (result)` and navigate only once.

### 4.6 Bottom navigation duplicated across three screens
`MainActivity.kt`, `SavedDataScreen.kt`, and `CustomSpeechScreen.kt` each contain an identical `NavigationBar` with the same three items and the same icon switch. The navigation state (`selectedItem`) is local per screen, so the selected tab is not persisted across navigations. Extract a `SpeechTimerBottomBar` component and a shared navigation helper.

### 4.7 `var selectedItem = remember { mutableIntStateOf(0) }` - wrong delegation
`MainActivity.kt:125` and `CustomSpeechScreen.kt:26` use `var selectedItem = remember { mutableIntStateOf(0) }`. Because `by` delegation is not used, `selectedItem` is a `MutableState` wrapper, and writes like `selectedItem.intValue = index` work but are less idiomatic and not read-safe in some Compose tooling. Either use `by` delegation or use `rememberSaveable`.

### 4.8 `SplashScreen.kt` uses `scale` animation for text alpha
`SplashScreen.kt:102`: `modifier = Modifier.scale(textAlpha.value)` is applied to text that should fade in. `scale(0f)` makes the text invisible by scaling to zero (also making it zero size), which is not the same as `alpha(0f)`. When `textAlpha` transitions from 0 to 1 the text scales up from nothing rather than fading in. Use `Modifier.alpha(textAlpha.value)`.

### 4.9 Dynamic color always enabled - `dynamicColor && true` logic error
`Theme.kt:57`: `dynamicColor && true` is always `true`, so the `dynamicColor` parameter passed to `SimpleSpeechTimerTheme` is effectively ignored. The fallback `DarkColorScheme`/`LightColorScheme` defined in `Color.kt` is never used. Either honour the parameter or remove the fallback color schemes.

### 4.10 `exportSchema = false` for Room
`Database.kt:40`: `exportSchema = false` suppresses the schema export. For a production app it is good practice to enable schema export (`exportSchema = true`) and commit the schema JSON files alongside the code so that migrations can be validated automatically.

---

## 5. Data Layer

### 5.1 Entity used directly as UI model (P0)
`Speaker` (`Database.kt:17-23`) is a Room `@Entity` that is collected by the ViewModel, passed to `SavedDataScreen`, and rendered in `SpeakerCard`. Any Room schema change (adding a column, renaming a field) forces Compose UI changes. Define a separate `SpeakerUiModel` and a mapper, or at minimum separate the `@Entity` from the file that defines the DAO and database.

### 5.2 All database operations are one-shot suspends, not Flows
`SpeakerDao` returns `List<Speaker>` from `suspend fun getAllSpeakers()`. Room supports `Flow<List<Speaker>>` which would automatically emit on every insert/delete, eliminating the manual `getAllSpeakers()` calls after every mutation in the ViewModel (`ViewModel.kt:34`, `ViewModel.kt:43`, `ViewModel.kt:52`, `ViewModel.kt:61`).

```kotlin
// Before (Database.kt:27)
@Query("SELECT * FROM speakers ORDER BY created_at DESC")
suspend fun getAllSpeakers(): List<Speaker>

// After
@Query("SELECT * FROM speakers ORDER BY created_at DESC")
fun getAllSpeakers(): Flow<List<Speaker>>
```

Then in the ViewModel, collect once in `init` and remove the four manual refresh calls.

### 5.3 `duration` stored as a formatted string
`Speaker.duration` is stored as a `String` (e.g. `"04:23"`). Storing raw seconds as `Int` is more flexible (enables sorting by actual duration, statistical aggregation) and lets formatting be a UI concern.

### 5.4 `EXAMPLE_COUNTER` and `incrementCounter` are dead code
`DataStoreManager.kt:18,39,44-49` contains `EXAMPLE_COUNTER` and `incrementCounter()` which are not referenced anywhere in the application. Remove them.

### 5.5 `ToggleSetting` component is unused
`ui/components/ToggleSetting.kt` defines a `ToggleSetting` Composable that is not called anywhere in the app. Remove it or use it.

### 5.6 `DateConverters` is in the wrong package
`data.repository.DateConverters` is referenced by the `@Database` annotation which must be at the same or child package level for visibility. It should be in `data.database` alongside `Database.kt`.

---

## 6. Concurrency & Coroutines

### 6.1 No explicit `Dispatchers` - relies on Room's internal dispatcher
`SpeakerRepository` wraps DAO suspend functions directly. Room automatically dispatches to a background thread for `suspend` DAO methods, so this is correct. However, the ViewModel launches all coroutines on `viewModelScope` (which uses `Dispatchers.Main.immediate`) without switching to `Dispatchers.IO`. This is fine for Room suspend calls but would be a problem if any blocking I/O is added later. Consider making the dispatchers injectable for testability.

### 6.2 Timer loop in `LaunchedEffect` uses `Dispatchers.Main`
The `while(true) { delay(1000); ticks++ }` loop in `TimerScreen.kt:149-199` runs on the Main dispatcher. For a simple counter this is fine, but vibrator and ringtone calls (`timers.kt:165-188`) are also made inline here on Main without checking if they need to be dispatched to another thread. `RingtoneManager.getRingtone` + `ringtone.play()` perform I/O and should be wrapped in `withContext(Dispatchers.IO)`.

### 6.3 `rememberCoroutineScope()` used for navigation side effects
`TimerScreen.kt:246` and `SavedDataScreen.kt:71` use `rememberCoroutineScope()` for Snackbar and navigation calls. This is acceptable, but using `LaunchedEffect` with a dedicated trigger event (MVI-style one-shot event) would make the flow easier to test and less prone to the double-navigate bug described in §4.5.

---

## 7. Error Handling

### 7.1 Silent exception swallowing
`TimerScreen.kt:186-188` and `SettingsScreen.kt:240-243`:

```kotlin
} catch (e: Exception) {
    // Handle potential exceptions
}
```

These swallows all exceptions from ringtone playback silently. At minimum, log the exception. Ideally surface a user-visible error or disable the sound toggle if audio is unavailable.

### 7.2 `uiState.error` is never cleared
`ViewModel.kt:45`: when a DB error occurs, `error` is set in the state. Nothing ever sets it back to `null` on the next successful load (`ViewModel.kt:43` only sets `isLoading = false` and `speakers`). Subsequent successful operations leave the error banner visible.

### 7.3 No typed error model
`MainScreenUiState.error` is `String?`. A `sealed class DataError` (or Kotlin `Result<T>`) would allow the UI to differentiate recoverable vs unrecoverable errors and take appropriate action.

### 7.4 No Result wrapper anywhere
There is no `Result<T>` or `Either<Error, T>` wrapper around repository calls. All error propagation is via caught `Exception` with `e.message` as the only discriminator.

---

## 8. Testing

### 8.1 No real tests exist (P0)
The only test files are the boilerplate scaffold tests:
- `ExampleUnitTest.kt` - asserts `2 + 2 == 4`
- `ExampleInstrumentedTest.kt` - asserts the package name

There are zero tests for:
- `formatTime()` (trivial to unit-test)
- `SpeakerRepository` (testable with an in-memory Room DB)
- `ViewModel` state transitions (testable with `kotlinx-coroutines-test` + Turbine)
- Colour threshold logic
- Navigation routes

### 8.2 Missing test dependencies
`app/build.gradle.kts` has no `kotlinx-coroutines-test`, no `turbine`, and no `mockk`/`kotlin-test` dependency. Even `kotlin.test` is absent from the version catalog.

### 8.3 Timer logic is untestable in its current form
Because the timer loop, threshold logic, and side effects all live inside `TimerScreen.kt` Composables, none of it can be unit-tested without a full Compose test harness. Moving to ViewModel state (§4.2) is a prerequisite.

---

## 9. Build & Gradle

### 9.1 `isMinifyEnabled = false` in release build
`app/build.gradle.kts:22`: `isMinifyEnabled = false` in the release build type. Minification and R8 shrinking significantly reduce APK size and make reverse engineering harder. For a simple app with Koin (which uses reflection internally), enabling minification requires adding ProGuard rules, but `proguard-rules.pro` is currently empty. Start with `isMinifyEnabled = true` + `isShrinkResources = true` and add Koin's keep rules.

### 9.2 Room version is one major version behind
`libs.versions.toml`: `room = "2.6.1"`. Room 2.7.x introduced KMP support and additional query improvements. Consider upgrading.

### 9.3 Koin BOM is pinned to 3.5.0 while 4.x is current
`libs.versions.toml`: `koin-bom = "3.5.0"`. Koin 4.x brought significant performance improvements and is the current stable series. The `koin.compose.viewmodel` artifact (commented out in `build.gradle.kts`) is the API reorganised in Koin 4.

### 9.4 `exportSchema = false` suppresses migration validation
Referenced in §5.10. Add `room { schemaDirectory("$projectDir/schemas") }` (already in `build.gradle.kts` actually - good) but change the annotation to `exportSchema = true`.

### 9.5 `datastore` and `datastore-preferences` are separate entries but the same version
`libs.versions.toml` has `datastore = "1.1.3"` and `datastorePreferences = "1.1.3"` as two separate version aliases pointing to the same value. Consolidate to a single `datastore = "1.1.3"` alias.

### 9.6 No Lint configuration
There is no `lint {}` block in `build.gradle.kts` and no `lint.xml`. At minimum, enable `abortOnError = true` for release builds and enable `checkDependencies = true`.

---

## 10. Performance

### 10.1 Timer causes one recomposition per second across the whole screen
`ticks` is a plain `remember { mutableIntStateOf(0) }` at the top of `TimerScreen`. Every second it increments, recomposing the entire `TimerScreen` subtree including the `Scaffold`, `TopAppBar`, `Row`, and all three buttons. Hoist `ticks` to the ViewModel as a `StateFlow` and collect only the sub-composables that need it, or at minimum split the time display into its own stateful child composable.

### 10.2 `animateColorAsState` and `animateFloatAsState` run on every recomposition
Both animations in `TimerScreen.kt:111-128` are evaluated on every recomposition. These are correctly placed above the loop but the `ticks` key issue (§4.4) means recompositions happen every second, which keeps animation objects being recreated. Move state to ViewModel to fix the root cause.

### 10.3 `SimpleDateFormat` created inside a `@Composable` on every composition
`SavedDataScreen.kt:302` creates `SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())` inside `SpeakerCard`, which is called once per list item on every recomposition. Wrap in `remember { }` or move to the ViewModel/mapper layer.

### 10.4 Splash screen adds ~2.1 seconds of artificial cold start delay
`SplashScreen.kt:66`: a hardcoded `delay(1500)` after the animation. Android 12+ has a native splash screen API (`androidx.core:core-splashscreen`) that shows the splash during the actual cold start window at zero cost. The Compose-based splash adds 2.1 s (600 ms animation + 150 ms gap + 600 ms text + 1500 ms wait - overlap) of dead time before the user reaches content.

### 10.5 Orientation lock prevents landscape/tablet use
`MainActivity.kt:59`: `requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT`. This is flagged as a `//todo` in the code itself. The timer screen's layout would benefit from a landscape-aware arrangement.

---

## 11. Security

### 11.1 No secrets in codebase - clean
There are no API keys, tokens, or credentials anywhere in the codebase. The app uses no network layer.

### 11.2 `android:allowBackup="true"` with no backup exclusion rules
`AndroidManifest.xml`: `allowBackup="true"` is set but `data_extraction_rules.xml` and `backup_rules.xml` are entirely commented-out templates. The Room database (containing all user speech history) will be included in ADB backups and cloud backups by default. If user data should be private, explicitly exclude the database:

```xml
<!-- res/xml/backup_rules.xml -->
<full-backup-content>
    <exclude domain="database" path="app_database" />
</full-backup-content>
```

### 11.3 Deprecated `Vibrator` API used without version check
`TimerScreen.kt:166` and `SettingsScreen.kt:215` use `ContextCompat.getSystemService(context, Vibrator::class.java)`. `Vibrator` is deprecated since API 31 in favour of `VibratorManager`. Since `minSdk = 31`, use `VibratorManager` exclusively.

```kotlin
// Before
val vibrator = ContextCompat.getSystemService(context, Vibrator::class.java)

// After (minSdk 31+)
val vibratorManager = context.getSystemService(VibratorManager::class.java)
val vibrator = vibratorManager.defaultVibrator
```

### 11.4 Ringtone not stopped after playback
`TimerScreen.kt:183`: `ringtone.play()` is called but `ringtone.stop()` is never called. If the user navigates away while the ringtone is playing it will continue. Store the `Ringtone` reference and stop it in `DisposableEffect.onDispose`.

---

## 12. Accessibility & UX

### 12.1 Timer colour changes are the sole notification - colour-blind users
The core UX relies on green/yellow/red background colour changes. Users with red-green colour blindness (deuteranopia) cannot distinguish green from red. The vibration and sound features mitigate this but are opt-in and off-by-default (sound). Consider adding a colour-blind-friendly palette option (e.g. blue/yellow/orange) or adding text labels to the phases.

### 12.2 `contentDescription = "visibility"` is not descriptive
`TimerScreen.kt:236`: `contentDescription = "visibility"` does not describe the action. Talkback will say "visibility, button." Use "Show timer UI" or "Hide timer UI" based on current state.

### 12.3 `contentDescription = "restart"`, `"play"`, `"pause"`, `"save"` are lowercase and terse
These are functional but TalkBack reads them verbatim. "Restart timer", "Start timer", "Pause timer", "Save result" are more useful for screen reader users.

### 12.4 Bottom navigation tab icons lack contentDescriptions in one place
`MainScreen` in `MainActivity.kt:155`: the navigation icon in the `TopAppBar` uses `contentDescription = "Menu"` but the icon is a static hourglass that performs no action. It should be `contentDescription = null` for decorative icons.

### 12.5 No string resources - all UI strings are hardcoded
Every UI string (`"TABLE TOPICS (1 TO 2 MIN)"`, `"Saved Speech Data"`, `"Are you sure..."`, etc.) is hardcoded in Kotlin source files. This prevents localization. Move all user-facing strings to `res/values/strings.xml`.

### 12.6 Portrait-only lock (also a UX issue)
Covered in §10.5. Tablets and foldables cannot rotate to landscape, which limits the user base.

---

## 13. Code Quality

### 13.1 Large commented-out code blocks
`SavedDataScreen.kt:478-526`: a fully commented-out `shareSpeakerData` implementation (ASCII table formatter) is ~50 lines of dead code. The active implementation appears at line 450. Remove the dead variant.

`ProgressBar.kt:152-213`: multiple blocks of commented-out `Text` labels replaced by `Box` indicators. The commented text is still present. Clean up.

`CustomSpeechScreen.kt:33-38` and `69-73`: commented-out navigation icon and snackbar code. Remove or un-comment.

`MainActivity.kt:175-179` and `187-192`: commented-out snackbar code and commented-out `Text` content. Remove dead code.

### 13.2 `//todo` and `//todo remove duplicate code` comments
`MainActivity.kt:58`: `//todo locked to portrait - support landscape` - file a real issue.
`SavedDataScreen.kt:449`: `//todo remove duplicate code` - the duplication between `shareSingleSpeaker` and `shareSpeakerData` is the `SimpleDateFormat` instantiation and the string template. Extract a `formatSpeakerEntry(speaker)` helper.

### 13.3 `SpeechTimerButton` parses route strings at call time, not definition time
`SpeechTimerButton.kt:20-23`: extracts `minMinutes`/`maxMinutes` from the button label text via regex at composition time, every recomposition. The four preset speech types have known parameters. Define them as data objects:

```kotlin
data class SpeechType(val label: String, val minMinutes: Int, val maxMinutes: Int)

val PRESET_SPEECH_TYPES = listOf(
    SpeechType("TABLE TOPICS", 1, 2),
    SpeechType("EVALUATION", 2, 3),
    SpeechType("ICE BREAKER", 4, 6),
    SpeechType("SPEECH", 5, 7),
)
```

### 13.4 Route strings are not type-safe
`Navigation.kt` uses string-based routes with path parameters (`"timer_screen/{title}/{minMinutes}/{maxMinutes}"`). Navigation 2.8+ supports type-safe navigation with `@Serializable` route objects. This also eliminates the manual `navArgument` declarations in `MainActivity.kt:90-95` and the raw string concatenation in `SpeechTimerButton.kt:29` and `CustomSpeechScreen.kt:147`.

### 13.5 `dynamicColor && true` is a logic error
`Theme.kt:57`: covered in §4.9. The boolean constant `true` makes the condition always true.

### 13.6 `@OptIn(ExperimentalMaterial3Api::class)` on classes that no longer need it
`MinutePicker.kt:21`: `@OptIn(ExperimentalMaterial3Api::class)` is present but `MinutePicker` uses only stable Material3 components (`Card`, `IconButton`, `Text`). The annotation is a no-op here.

### 13.7 `fillMaxHeight().fillMaxSize()` duplication
`MainActivity.kt:195-196`: `Modifier.fillMaxHeight().fillMaxSize()` - `fillMaxSize()` already implies `fillMaxHeight()`. Remove `fillMaxHeight()`.

### 13.8 `EXAMPLE_COUNTER` is a package-level `val` with a misleading name
`DataStoreManager.kt:18`: `val EXAMPLE_COUNTER = intPreferencesKey("example_counter")` is a leftover from a template. Remove it (see §5.4).

---

## 14. Quick Wins vs. Larger Refactors

### Quick Wins (effort: S, can be done in < 1 hour each)

| # | Item | File | Effort | Priority | Status |
|---|------|------|--------|----------|--------|
| QW-1 | Rename `class ViewModel` to `SpeechTimerViewModel` | `ViewModel.kt:17` | S | P0 | DONE 3cfd5d6 (Wave 1) |
| QW-2 | Fix blinking `LaunchedEffect` key (`ticks` -> remove) | `TimerScreen.kt:137` | S | P0 | NOT A BUG, see Wave 1 notes |
| QW-3 | Fix `Modifier.scale(textAlpha.value)` -> `Modifier.alpha(...)` in SplashScreen | `SplashScreen.kt:102,113` | S | P1 | open |
| QW-4 | Remove `EXAMPLE_COUNTER` / `incrementCounter` dead code | `DataStoreManager.kt:18,44` | S | P1 | open |
| QW-5 | Remove `ToggleSetting.kt` (unused component) | `ToggleSetting.kt` | S | P1 | open |
| QW-6 | Fix `dynamicColor && true` logic error in Theme | `Theme.kt:57` | S | P1 | open |
| QW-7 | Deduplicate `fillMaxHeight().fillMaxSize()` | `MainActivity.kt:195` | S | P2 | open |
| QW-8 | Remove all commented-out code blocks | Multiple files | S | P2 | open |
| QW-9 | Consolidate `datastore`/`datastorePreferences` version aliases | `libs.versions.toml` | S | P2 | open |
| QW-10 | Use `VibratorManager` instead of deprecated `Vibrator` (minSdk=31) | `TimerScreen.kt:166`, `SettingsScreen.kt:215` | S | P1 | DONE 73fc711 (Wave 1) |
| QW-11 | Fix double-navigate after save (Snackbar action) | `TimerScreen.kt:419-433` | S | P1 | NOT A BUG, see Wave 1 notes |
| QW-12 | Improve `contentDescription` values for accessibility | `TimerScreen.kt:236`, buttons | S | P2 | open |
| QW-13 | Stop ringtone on DisposableEffect dispose | `TimerScreen.kt:183` | S | P1 | open |
| QW-14 | Enable backup exclusion rules for Room DB | `backup_rules.xml` | S | P1 | open |
### Medium Refactors (effort: M, 1-4 hours each)

| # | Item | Files Affected | Effort | Priority | Status |
|---|------|---------------|--------|----------|--------|
| M-1 | Move DAO to return `Flow<List<Speaker>>` and remove manual refresh calls | `Database.kt`, `ViewModel.kt` | M | P0 | DONE 42da168 (Wave 1) |
| M-2 | Move timer state + threshold logic to ViewModel | `TimerScreen.kt`, `ViewModel.kt` | M | P0 | PARTIAL: thresholds extracted to `TimerThresholds` 229497b; tick loop and side effects still in Composable |
| M-3 | Extract `NavGraph` + `MainScreen` out of `MainActivity.kt` | `MainActivity.kt` | M | P1 | open |
| M-4 | Extract shared `SpeechTimerBottomBar` component | `MainActivity.kt`, `SavedDataScreen.kt`, `CustomSpeechScreen.kt` | M | P1 | open |
| M-5 | Swap `fallbackToDestructiveMigration` for real migrations | `AppModule.kt`, `Database.kt` | M | P0 | open |
| M-6 | Remove `DataStoreManager` injection inside Composables; pass via ViewModel | `TimerScreen.kt`, `SettingsScreen.kt`, `Theme.kt` | M | P1 | open |
| M-7 | Replace `SpeechTimerButton` regex parsing with typed `SpeechType` data class | `SpeechTimerButton.kt`, `MainActivity.kt` | M | P1 | open |
| M-8 | Add real unit tests: `formatTime`, ViewModel state, Repository | New test files | M | P0 | DONE for formatTime + ViewModel + thresholds (3b6319d, 42da168, 27ff156, 229497b); Room repository test still open (needs Robolectric or androidTest) |
| M-9 | Enable `isMinifyEnabled = true` + add Koin ProGuard rules | `build.gradle.kts`, `proguard-rules.pro` | M | P1 | open |
| M-10 | Move all UI strings to `strings.xml` | All screen files | M | P2 | open |
### Larger Refactors (effort: L, requires design decisions)

| # | Item | Effort | Priority |
|---|------|--------|----------|
| L-1 | Introduce `SpeakerUiModel` / mapper to decouple Room entity from UI | L | P1 |
| L-2 | Migrate to type-safe Navigation 2.8 `@Serializable` routes | L | P2 |
| L-3 | Add colour-blind-friendly theme option to settings | L | P2 |
| L-4 | Move to MVI pattern with sealed `UiAction` / `UiEvent` for one-shot events | L | P2 |
| L-5 | Store `duration` as `Int` seconds instead of formatted string | L | P2 |
| L-6 | Add landscape / tablet layout support (remove orientation lock) | L | P2 |
| L-7 | Migrate to `core-splashscreen` to eliminate 2-second artificial delay | L | P2 |

---

## 15. Wave Log

### Wave 1 (2026-09-12)

Baseline: `assembleDebug` green, 1 unit test (`ExampleUnitTest`). Final: green, 18 unit tests across 4 classes.

Commits:

| Hash | Change | Items |
|------|--------|-------|
| 3cfd5d6 | Rename `class ViewModel` to `SpeechTimerViewModel` | QW-1 / 4.1 |
| 3b6319d | Add `kotlinx-coroutines-test` (1.7.3, matches runtime) and `FormatTimeTest` | M-8 / 8.1, 8.2 |
| 42da168 | `SpeakerDao.getAllSpeakers()` returns `Flow`; ViewModel collects once; `SavedDataScreen` no longer re-queries; `SpeechTimerViewModelTest` + `FakeSpeakerDao` + `MainDispatcherRule` | M-1 / 5.2, M-8 |
| 27ff156 | DB write failures are caught and surfaced in `uiState.error`; a successful load clears `error` | 7.2 (+ new: uncaught exceptions on insert/delete) |
| 229497b | `TimerThresholds` / `TimerPhase` pure logic extracted from `TimerScreen`, with tests | 8.3, first slice of M-2 |
| 73fc711 | `VibratorManager.defaultVibrator` replaces deprecated `Vibrator` lookup | QW-10 / 11.3 |

Analysis corrections (items that turned out not to be bugs):

- **QW-2 / 4.4 (blinking effect)**: the effect is keyed on `(isTimerRunning, ticks)`. While running, `ticks` changes every second and the effect restarts, but it takes the `else` branch (no loop), so restarting is harmless. While paused, `ticks` stops changing, so the blink loop runs uninterrupted. The blink works today. The proposed fix in 4.4 (key only on `isTimerRunning`) would introduce a bug: pressing Reset while paused keeps `isTimerRunning == false`, so the effect would not restart and the display would keep blinking at 00:00. If this is ever revisited, key on `!isTimerRunning && ticks > 0`.
- **QW-11 / 4.5 (double navigate)**: the `if / else` only navigates once per branch. What remains true is the UX fragility: dismissing the snackbar still auto-navigates 1.5 s later. That is a product decision, left as-is.

New issues found in Wave 1 (not fixed):

- `app/schemas/` contains two stale schema folders for the old package `com.guy.myapplication2` (version 1 only). `exportSchema = false` means no schema for version 2 exists, so M-5 (real migrations) has no verified baseline to write `MIGRATION_1_2`/`MIGRATION_2_3` against. Recommend `exportSchema = true`, bump nothing, commit the generated `2.json`, and delete the stale folders.
- Unused imports: `android.R.attr.fontStyle` (`SpeechTimerButton.kt:3`), `android.R.id.shareText` (`SavedDataScreen.kt:3`), `kotlin.text.format` (`SavedDataScreen.kt:30`). `MainActivity.TAG` is unused.
- `TimerScreen.kt` `DisposableEffect(lifecycleOwner)` registers a `LifecycleEventObserver` whose `ON_PAUSE` branch is empty: dead scaffolding. Note the timer keeps ticking in the background because the loop lives in a `LaunchedEffect`; decide whether that is desired before touching it.
- `TimerScreen.kt` sound branch: `if (newBackgroundColor == Color.Red) ringtone.play() else ringtone.play()` - both branches identical.
- Scaffold tests `ExampleUnitTest` and `ExampleInstrumentedTest` are still present.
- Lint (`lintDebug`, 72 issues, none error-severity): `SourceLockedOrientationActivity` (MainActivity:59, known 10.5), `ObsoleteSdkInt` for `mipmap-anydpi-v26` (minSdk 31), `VectorRaster` for `hour_glass_svg.xml` and `splash_screen_svg.xml` (vectors larger than 200x200), duplicate `ic_launcher_foreground.png` / `ic_launcher_monochrome.png`, `OldTargetApi` (targetSdk 35), and dependency-update notices.
- `SpeechTimerViewModel` initial `isLoading = true` until the first Room emission; `SavedDataScreen` shows a spinner for that instant. Intentional, noting for awareness.

## 16. Next-Session Resume Block

- Wave number: next is **Wave 2**.
- Build: `JAVA_HOME=<JDK 17> ./gradlew assembleDebug testDebugUnitTest` is green; 18 unit tests.
- Test infra in place: `app/src/test/.../MainDispatcherRule.kt`, `data/database/FakeSpeakerDao.kt` (in-memory DAO with `queryError` / `insertError` hooks). JUnit4 + `kotlinx-coroutines-test`; no Turbine/MockK/Robolectric.
- Suggested Wave 2 order:
  1. M-2 remainder: move `ticks` / `isTimerRunning` / phase into `SpeechTimerViewModel` (or a `TimerController`) using `TimerThresholds`; then the blink, vibration and ringtone side effects become one-shot events. Unblocks 10.1/10.2 and QW-13.
  2. M-5: enable `exportSchema = true`, commit `schemas/.../2.json`, replace `fallbackToDestructiveMigration()` (destroys user history on any schema change).
  3. Quick wins with no product decisions: QW-3, QW-4, QW-5, QW-6, QW-7, the unused imports above, and 3.4 (commented-out dependency).
  4. QW-14 (backup exclusion) and QW-11 auto-navigate behaviour need a product call first; see Questions in the Wave 1 report.
- Do not commit `.idea/` changes or `.claude/` (local tooling); `.gitignore` already exists and was left alone.
