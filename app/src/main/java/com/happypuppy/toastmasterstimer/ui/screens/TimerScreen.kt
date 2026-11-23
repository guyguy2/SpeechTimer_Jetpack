package com.happypuppy.toastmasterstimer.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.navigation.NavHostController
import com.happypuppy.toastmasterstimer.R
import com.happypuppy.toastmasterstimer.alert.AndroidAlertPlayer
import com.happypuppy.toastmasterstimer.alert.dispatchPhaseAlert
import com.happypuppy.toastmasterstimer.data.preferences.DataStoreManager
import com.happypuppy.toastmasterstimer.navigation.Screen
import com.happypuppy.toastmasterstimer.ui.components.InputDialog
import com.happypuppy.toastmasterstimer.ui.components.TimerProgressBar
import com.happypuppy.toastmasterstimer.util.TimerPhase
import com.happypuppy.toastmasterstimer.util.formatTime
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

const val FOREGROUND_HINT = "Keep this screen open for alerts"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerScreen(
    navController: NavHostController,
    title: String = "Timer Screen",
    minMinutes: Int = 1,
    maxMinutes: Int = 2,
    viewModel: SpeechTimerViewModel = koinViewModel(),
    timerViewModel: TimerViewModel = koinViewModel()
) {
    LaunchedEffect(minMinutes, maxMinutes) {
        timerViewModel.configure(minMinutes, maxMinutes)
    }

    val timerState by timerViewModel.uiState.collectAsState()
    val ticks = timerState.elapsedSeconds
    val isTimerRunning = timerState.isRunning
    val currentPhase = timerState.phase
    val isUIVisible = timerState.isUiVisible
    val thresholds = timerState.thresholds

    // Keep the screen awake while the timer is actively running
    val view = LocalView.current
    DisposableEffect(view, isTimerRunning) {
        view.keepScreenOn = isTimerRunning
        onDispose {
            view.keepScreenOn = false
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

    val dataStoreManager = koinInject<DataStoreManager>()
    val vibrationEnabled by dataStoreManager.vibrateEnabledFlow.collectAsState(initial = true)
    val soundEnabled by dataStoreManager.soundEnabledFlow.collectAsState(initial = false)
    val currentVibrationEnabled by rememberUpdatedState(vibrationEnabled)
    val currentSoundEnabled by rememberUpdatedState(soundEnabled)

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

    var isTimeVisible by remember { mutableStateOf(true) }
    LaunchedEffect(isTimerRunning, ticks) {
        if (!isTimerRunning && ticks > 0) {
            while (true) {
                delay(1000)
                isTimeVisible = !isTimeVisible
            }
        } else {
            isTimeVisible = true
        }
    }

    val speechType = remember(title) {
        val regex = "(.*?)\\s*\\(.*\\)".toRegex()
        val matchResult = regex.find(title)
        matchResult?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() } ?: title
    }

    val formattedTime = remember(ticks) { formatTime(ticks) }

    val targetBackgroundColor = when (currentPhase) {
        TimerPhase.BEFORE_GREEN -> Color.Transparent
        TimerPhase.GREEN -> PhaseGreenColor
        TimerPhase.YELLOW -> PhaseYellowColor
        TimerPhase.RED -> PhaseRedColor
    }

    val animatedBackgroundColor by animateColorAsState(
        targetValue = targetBackgroundColor,
        animationSpec = tween(durationMillis = 800),
        label = "backgroundColor"
    )

    val timerTextColor = when (currentPhase) {
        TimerPhase.BEFORE_GREEN -> MaterialTheme.colorScheme.onBackground
        TimerPhase.GREEN -> PhaseOnGreenColor
        TimerPhase.YELLOW -> PhaseOnYellowColor
        TimerPhase.RED -> PhaseOnRedColor
    }

    val buttonSize = 64.dp
    val buttonAspectRatio by animateFloatAsState(
        targetValue = if (isTimerRunning) 2.0f else 1.5f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "aspectRatio"
    )

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Leaving the screen discards the timer, so ask first once there is time on the clock
    var showLeaveDialog by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled = ticks > 0) { showLeaveDialog = true }

    // Restart resets at once; the snackbar offers Undo, which is dropped as soon as the timer moves on
    var undoJob by remember { mutableStateOf<Job?>(null) }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (ticks > 0) showLeaveDialog = true else navController.navigateUp()
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { timerViewModel.toggleUiVisibility() }) {
                        Icon(
                            painter = painterResource(
                                if (!isUIVisible) R.drawable.visibility_off_24px
                                else R.drawable.visibility_24px
                            ),
                            contentDescription = if (isUIVisible) "Hide controls" else "Show controls"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        var showDialog by remember { mutableStateOf(false) }

        // Background is placed BEFORE innerPadding so the phase color extends edge-to-edge behind system bars
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(animatedBackgroundColor)
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            if (isUIVisible) {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val isCompactHeight = maxHeight < 460.dp

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = if (isCompactHeight) Arrangement.SpaceBetween else Arrangement.SpaceEvenly,
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = if (isCompactHeight) 12.dp else 24.dp)
                    ) {
                        if (!isCompactHeight) {
                            Spacer(modifier = Modifier.height(24.dp))
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (timerState.isRestored) {
                                Text(
                                    text = "Timer restored",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = timerTextColor,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }

                            Text(
                                text = if (isTimeVisible) formattedTime else "     ",
                                style = TextStyle(
                                    fontSize = if (isCompactHeight) 48.sp else 60.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = timerTextColor
                                )
                            )

                            Spacer(modifier = Modifier.height(if (isCompactHeight) 12.dp else 16.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .padding(horizontal = 16.dp)
                                    .background(
                                        color = Color.Black.copy(alpha = 0.4f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(vertical = 4.dp, horizontal = 2.dp)
                            ) {
                                TimerProgressBar(
                                    currentSeconds = ticks,
                                    maxSeconds = thresholds.maxDurationSeconds,
                                    greenThreshold = thresholds.greenSeconds,
                                    yellowThreshold = thresholds.yellowSeconds,
                                    redThreshold = thresholds.redSeconds,
                                    modifier = Modifier.fillMaxWidth(),
                                    showLabels = true
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(if (isCompactHeight) 16.dp else 32.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = if (isCompactHeight) 8.dp else 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                if (ticks > 0) {
                                    Button(
                                        onClick = {
                                            alertPlayer.stopSound()
                                            val snapshot = timerViewModel.snapshot()
                                            timerViewModel.reset()
                                            undoJob?.cancel()
                                            undoJob = scope.launch {
                                                val result = snackbarHostState.showSnackbar(
                                                    message = "Timer reset",
                                                    actionLabel = "Undo",
                                                    duration = SnackbarDuration.Long
                                                )
                                                if (result == SnackbarResult.ActionPerformed) {
                                                    timerViewModel.undoReset(snapshot)
                                                }
                                            }
                                        },
                                        modifier = Modifier
                                            .size(buttonSize)
                                            .clip(CircleShape),
                                        shape = CircleShape,
                                        contentPadding = PaddingValues(4.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.restart_alt_24px),
                                            contentDescription = "restart",
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Box(
                                modifier = Modifier.weight(1.5f),
                                contentAlignment = Alignment.Center
                            ) {
                                Button(
                                    onClick = {
                                        undoJob?.cancel()
                                        timerViewModel.toggleRunning()
                                    },
                                    modifier = Modifier
                                        .height(buttonSize)
                                        .width(buttonSize * buttonAspectRatio)
                                        .clip(CircleShape),
                                    shape = CircleShape
                                ) {
                                    Icon(
                                        painter = painterResource(
                                            if (isTimerRunning) R.drawable.pause_24px
                                            else R.drawable.play_arrow_24px
                                        ),
                                        contentDescription = if (isTimerRunning) "pause" else "play",
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!isTimerRunning && ticks > 0) {
                                    Button(
                                        onClick = { showDialog = true },
                                        contentPadding = PaddingValues(4.dp),
                                        modifier = Modifier
                                            .size(buttonSize)
                                            .clip(CircleShape),
                                        shape = CircleShape
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.save_24px),
                                            contentDescription = "save",
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Text(
                            text = FOREGROUND_HINT,
                            style = MaterialTheme.typography.bodySmall,
                            color = timerTextColor.copy(alpha = HintTextAlpha),
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }

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
                            text = "Leave timer?",
                            style = MaterialTheme.typography.headlineSmall,
                            color = AlertDialogDefaults.titleContentColor
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Your time will be lost unless you save it first.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AlertDialogDefaults.textContentColor
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(modifier = Modifier.align(Alignment.End)) {
                            TextButton(onClick = { showLeaveDialog = false }) {
                                Text("Stay")
                            }
                            TextButton(
                                onClick = {
                                    showLeaveDialog = false
                                    navController.navigateUp()
                                }
                            ) {
                                Text("Leave")
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
                        duration = formattedTime
                    )

                    scope.launch {
                        val result = snackbarHostState.showSnackbar(
                            message = "Saved ${name}'s speech data",
                            actionLabel = "VIEW DATA",
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
                title = "Save Time"
            )
        }
    }
}
