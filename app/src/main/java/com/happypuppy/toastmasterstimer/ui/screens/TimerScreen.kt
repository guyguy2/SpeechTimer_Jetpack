package com.happypuppy.toastmasterstimer.ui.screens

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.navigation.NavHostController
import com.happypuppy.toastmasterstimer.R
import com.happypuppy.toastmasterstimer.alert.AndroidAlertPlayer
import com.happypuppy.toastmasterstimer.alert.dispatchPhaseAlert
import com.happypuppy.toastmasterstimer.data.preferences.DataStoreManager
import com.happypuppy.toastmasterstimer.navigation.Screen
import com.happypuppy.toastmasterstimer.navigation.TimerRoute
import com.happypuppy.toastmasterstimer.ui.components.InputDialog
import com.happypuppy.toastmasterstimer.ui.components.displayTitle
import com.happypuppy.toastmasterstimer.ui.theme.LocalIsDarkTheme
import com.happypuppy.toastmasterstimer.ui.theme.LocalPhaseColors
import com.happypuppy.toastmasterstimer.util.formatTime
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerScreen(
    navController: NavHostController,
    route: TimerRoute,
    viewModel: SpeechTimerViewModel = koinViewModel(),
    timerViewModel: TimerViewModel = koinViewModel()
) {
    val timerState by timerViewModel.uiState.collectAsStateWithLifecycle()
    val ticks = timerState.elapsedSeconds
    val isTimerRunning = timerState.isRunning
    val currentPhase = timerState.phase
    val isUIVisible = timerState.isUiVisible
    val thresholds = timerState.thresholds

    val dataStoreManager = koinInject<DataStoreManager>()
    val keepScreenOnEnabled by dataStoreManager.keepScreenOnEnabledFlow.collectAsStateWithLifecycle(initialValue = true)
    val vibrationEnabled by dataStoreManager.vibrateEnabledFlow.collectAsStateWithLifecycle(initialValue = true)
    val soundEnabled by dataStoreManager.soundEnabledFlow.collectAsStateWithLifecycle(initialValue = false)
    val currentVibrationEnabled by rememberUpdatedState(vibrationEnabled)
    val currentSoundEnabled by rememberUpdatedState(soundEnabled)

    val phasePalette = LocalPhaseColors.current.paletteFor(currentPhase)
    val defaultLightSystemBars = !LocalIsDarkTheme.current
    val phaseLightSystemBars = phasePalette.background.luminance() > 0.5f

    // Keep the screen awake while the timer is actively running when enabled in Settings
    val view = LocalView.current
    DisposableEffect(view, isTimerRunning, keepScreenOnEnabled) {
        view.keepScreenOn = isTimerRunning && keepScreenOnEnabled
        onDispose {
            view.keepScreenOn = false
        }
    }

    // Match status and navigation bar icon contrast to the active phase background and restore on exit.
    DisposableEffect(view, phaseLightSystemBars, defaultLightSystemBars) {
        val window = (view.context as? Activity)?.window
        if (window != null) {
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = phaseLightSystemBars
            controller.isAppearanceLightNavigationBars = phaseLightSystemBars
        }
        onDispose {
            val restoreWindow = (view.context as? Activity)?.window
            if (restoreWindow != null) {
                val controller = WindowCompat.getInsetsController(restoreWindow, view)
                controller.isAppearanceLightStatusBars = defaultLightSystemBars
                controller.isAppearanceLightNavigationBars = defaultLightSystemBars
            }
        }
    }

    val context = LocalContext.current
    // Owns the active ringtone so it is stopped cleanly on reset or screen exit
    val alertPlayer = remember(context) { AndroidAlertPlayer(context) }
    DisposableEffect(alertPlayer) {
        onDispose {
            alertPlayer.stopSound()
        }
    }

    // Collect one-shot phase-transition alerts
    LaunchedEffect(timerViewModel) {
        timerViewModel.phaseAlerts.collect { phase ->
            dispatchPhaseAlert(alertPlayer, phase, currentVibrationEnabled, currentSoundEnabled)
        }
    }

    // Repeat the cue once when returning to a timer that crossed a phase while the app was away
    LaunchedEffect(timerViewModel) {
        timerViewModel.catchUpCues.collect { phase ->
            dispatchPhaseAlert(alertPlayer, phase, currentVibrationEnabled, currentSoundEnabled)
        }
    }
    // findViewTreeLifecycleOwner avoids the deprecated LocalLifecycleOwner until lifecycle-runtime-compose is added
    val lifecycleOwner = view.findViewTreeLifecycleOwner()
    DisposableEffect(lifecycleOwner, timerViewModel) {
        if (lifecycleOwner == null) return@DisposableEffect onDispose { }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> timerViewModel.onAppBackgrounded()
                Lifecycle.Event.ON_START -> timerViewModel.onAppForegrounded()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val resources = LocalResources.current
    val title = route.displayTitle()
    val speechType = route.speechType.label

    val formattedTime = remember(ticks) { formatTime(ticks) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Leaving the screen discards the timer, so ask first once there is time on the clock
    var showLeaveDialog by rememberSaveable { mutableStateOf(false) }
    var showDialog by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled = ticks > 0) { showLeaveDialog = true }

    // Restart resets at once; the snackbar offers Undo, which is dropped as soon as the timer moves on
    var undoJob by remember { mutableStateOf<Job?>(null) }

    val triggerRestart = {
        alertPlayer.stopSound()
        val snapshot = timerViewModel.snapshot()
        timerViewModel.reset()
        undoJob?.cancel()
        undoJob = scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = resources.getString(R.string.timer_reset_snackbar),
                actionLabel = resources.getString(R.string.saved_undo),
                duration = SnackbarDuration.Long
            )
            if (result == SnackbarResult.ActionPerformed) {
                timerViewModel.undoReset(snapshot)
            }
        }
    }

    val triggerToggleRunning = {
        undoJob?.cancel()
        timerViewModel.toggleRunning()
    }

    val screenFocusRequester = remember { FocusRequester() }
    LaunchedEffect(showDialog, showLeaveDialog) {
        if (!showDialog && !showLeaveDialog) {
            runCatching { screenFocusRequester.requestFocus() }
        }
    }

    val topBarContentColor by animateColorAsState(
        targetValue = phasePalette.onBackground,
        animationSpec = tween(durationMillis = 400)
    )

    Scaffold(
        modifier = Modifier
            .focusRequester(screenFocusRequester)
            .focusable()
            .onPreviewKeyEvent { keyEvent ->
                if (showDialog || showLeaveDialog) return@onPreviewKeyEvent false
                if (keyEvent.type != KeyEventType.KeyDown || keyEvent.nativeKeyEvent.repeatCount != 0) {
                    return@onPreviewKeyEvent false
                }
                when (keyEvent.key) {
                    Key.Spacebar -> {
                        triggerToggleRunning()
                        true
                    }
                    Key.R -> if (ticks > 0) {
                        triggerRestart()
                        true
                    } else {
                        false
                    }
                    Key.Escape -> {
                        if (ticks > 0) showLeaveDialog = true else navController.navigateUp()
                        true
                    }
                    else -> false
                }
            },
        containerColor = phasePalette.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            // In speaker-facing mode (!isUIVisible) hide the TopAppBar so the full screen is
            // uninterrupted phase color; TimerFace renders a floating exit button in safe insets.
            if (isUIVisible) {
                // The title may wrap to two lines, so the bar grows with the font scale instead of truncating.
                val fontScale = LocalDensity.current.fontScale
                TopAppBar(
                    expandedHeight = TopAppBarDefaults.TopAppBarExpandedHeight * maxOf(1f, fontScale),
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent,
                        navigationIconContentColor = topBarContentColor,
                        titleContentColor = topBarContentColor,
                        actionIconContentColor = topBarContentColor
                    ),
                    title = {
                        Text(
                            text = title,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                if (ticks > 0) showLeaveDialog = true else navController.navigateUp()
                            }
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.common_back)
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { timerViewModel.toggleUiVisibility() }) {
                            Icon(
                                painter = painterResource(R.drawable.visibility_24px),
                                contentDescription = stringResource(R.string.timer_hide_controls)
                            )
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        TimerFace(
            phase = currentPhase,
            ticks = ticks,
            isTimerRunning = isTimerRunning,
            isUiVisible = isUIVisible,
            isRestored = timerState.isRestored,
            thresholds = thresholds,
            innerPadding = innerPadding,
            onRestart = triggerRestart,
            onToggleRunning = triggerToggleRunning,
            onSave = { showDialog = true },
            onToggleUiVisibility = { timerViewModel.toggleUiVisibility() }
        )

        if (showLeaveDialog) {
            // Built from BasicAlertDialog so Stay comes before Leave in the accessibility tree
            // (the stock AlertDialog in material3 1.5 puts the confirm button first for screen readers).
            BasicAlertDialog(onDismissRequest = { showLeaveDialog = false }) {
                Surface(
                    shape = AlertDialogDefaults.shape,
                    color = AlertDialogDefaults.containerColor,
                    tonalElevation = AlertDialogDefaults.TonalElevation,
                    modifier = Modifier.widthIn(min = 280.dp, max = 560.dp)
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(
                            text = stringResource(R.string.timer_leave_title),
                            style = MaterialTheme.typography.headlineSmall,
                            color = AlertDialogDefaults.titleContentColor
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.timer_leave_message),
                            style = MaterialTheme.typography.bodyMedium,
                            color = AlertDialogDefaults.textContentColor
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(modifier = Modifier.align(Alignment.End)) {
                            TextButton(onClick = { showLeaveDialog = false }) {
                                Text(stringResource(R.string.timer_leave_stay))
                            }
                            TextButton(
                                onClick = {
                                    showLeaveDialog = false
                                    navController.navigateUp()
                                }
                            ) {
                                Text(stringResource(R.string.timer_leave_confirm))
                            }
                        }
                    }
                }
            }
        }

        if (showDialog && isUIVisible) {
            InputDialog(
                onDismiss = { showDialog = false },
                onConfirm = { name ->
                    viewModel.saveSpeaker(
                        name = name,
                        speechType = speechType,
                        duration = formattedTime,
                        speechTypeId = route.speechType.id,
                        minSeconds = thresholds.greenSeconds,
                        maxSeconds = thresholds.redSeconds,
                        elapsedSeconds = ticks,
                        phaseReached = currentPhase.name
                    )

                    scope.launch {
                        val result = snackbarHostState.showSnackbar(
                            message = resources.getString(R.string.timer_saved_snackbar, name),
                            actionLabel = resources.getString(R.string.timer_saved_view_data),
                            duration = SnackbarDuration.Short
                        )

                        if (result == SnackbarResult.ActionPerformed) {
                            navController.navigate(Screen.SavedDataScreen.route) {
                                popUpTo(Screen.Main.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                    showDialog = false
                },
                title = stringResource(R.string.timer_save_dialog_title)
            )
        }
    }
}
