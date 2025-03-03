package com.happypuppy.toastmasterstimer.ui.screens

import android.media.RingtoneManager
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import com.happypuppy.toastmasterstimer.R
import com.happypuppy.toastmasterstimer.data.preferences.DataStoreManager
import com.happypuppy.toastmasterstimer.navigation.Screen
import com.happypuppy.toastmasterstimer.ui.components.InputDialog
import com.happypuppy.toastmasterstimer.ui.components.TimerProgressBar
import com.happypuppy.toastmasterstimer.util.formatTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerScreen(
    navController: NavHostController,
    title: String = "Timer Screen",
    minMinutes: Int = 1,
    maxMinutes: Int = 2,
    viewModel: ViewModel = koinViewModel() // Get ViewModel using Koin
) {
    // Timer state
    var ticks by remember { mutableIntStateOf(0) }
    var isTimerRunning by remember { mutableStateOf(false) }
    var targetBackgroundColor by remember { mutableStateOf(Color.Transparent) }
    var isTimeVisible by remember { mutableStateOf(true) }
    var isUIVisible by remember { mutableStateOf(true) }

    // Constants for color thresholds (unchanged)
    val greenThreshold = minMinutes * 60 // Convert minutes to seconds
    val redThreshold = maxMinutes * 60 // Convert minutes to seconds
    val yellowThreshold = (greenThreshold + redThreshold) / 2 // Middle point
    val maxDuration = redThreshold + 2 // Maximum timer duration in seconds

    // Extract speech type from title
    val speechType = remember {
        // Extract speech type from title (e.g., "TABLE TOPICS (1 TO 2 MIN)" -> "TABLE TOPICS")
        val regex = "(.*?)\\s*\\(.*\\)".toRegex()
        val matchResult = regex.find(title)
        matchResult?.groupValues?.get(1) ?: title
    }

    // Formatted time for saving to database
    val formattedTime = remember(ticks) { formatTime(ticks) }

    // Animated background color and UI elements (unchanged)
    val animatedBackgroundColor by animateColorAsState(
        targetValue = targetBackgroundColor,
        animationSpec = tween(durationMillis = 800),
        label = "backgroundColor"
    )

    // Calculate derived dimensions based on container width (unchanged)
    val buttonSize = 64.dp

    // Animate aspect ratio to modify shape between circle and ellipse (unchanged)
    val buttonAspectRatio by animateFloatAsState(
        targetValue = if (isTimerRunning) 2.0f else 1.5f,  // 1.0 = circle, 2.0 = ellipse (2x wider)
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "aspectRatio"
    )

    val dataStoreManager = koinInject<DataStoreManager>()
    val vibrationEnabled by dataStoreManager.vibrateEnabledFlow.collectAsState(initial = true)
    val soundEnabled by dataStoreManager.soundEnabledFlow.collectAsState(initial = false)
    val context = LocalContext.current
    var previousBackgroundColor by remember { mutableStateOf(Color.Transparent) }

    // Add blinking effect when timer is paused but not when reset to 0 (unchanged)
    LaunchedEffect(isTimerRunning, ticks) {
        if (!isTimerRunning && ticks > 0) {
            while (true) {
                delay(1000)
                isTimeVisible = !isTimeVisible
            }
        } else {
            isTimeVisible = true  // Always visible when running or reset to 0
        }
    }

    // Timer effect with smooth color transitions
    LaunchedEffect(isTimerRunning) {
        if (isTimerRunning) {
            while (true) {
                delay(1000)
                ticks++

                val newBackgroundColor = when {
                    ticks < greenThreshold -> Color.Transparent
                    ticks < yellowThreshold -> Color(0xFF006400)
                    ticks < redThreshold -> Color(0xFFFFDC28)
                    else -> Color.Red
                }

                // Check if background color has changed
                if (newBackgroundColor != previousBackgroundColor && newBackgroundColor != Color.Transparent) {
                    // Play haptic feedback if enabled
                    if (vibrationEnabled) {
                        val vibrator = ContextCompat.getSystemService(context, Vibrator::class.java)
                        if (newBackgroundColor == Color.Red) {
                            vibrator?.vibrate(
                                VibrationEffect.createWaveform(longArrayOf(0, 300, 100, 300), -1))
                        } else {
                            vibrator?.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE))
                        }
                    }

                    // Play sound if enabled
                    if (soundEnabled) {
                        try {
                            val notification =
                                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                            val ringtone = RingtoneManager.getRingtone(context, notification)
                            if (newBackgroundColor == Color.Red) {
                                ringtone.play()
                            } else {
                                ringtone.play()
                            }
                        } catch (e: Exception) {
                            // Handle potential exceptions
                        }
                    }

                    // Update the target color and previous color
                    targetBackgroundColor = newBackgroundColor
                    previousBackgroundColor = newBackgroundColor
                } else {
                    targetBackgroundColor = newBackgroundColor
                }
            }
        }
    }

    // Lifecycle management (unchanged)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                // Timer can be paused here if needed
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    var isClicked by remember { mutableStateOf(false) }
                    IconButton(onClick = {
                        isClicked = !isClicked
                        isUIVisible = !isClicked // Toggle UI visibility based on isClicked state
                    }) {
                        Icon(
                            painter = painterResource(
                                if (isClicked) R.drawable.visibility_off_24px
                                else R.drawable.visibility_24px
                            ),
                            contentDescription = "visibility"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        var showDialog by remember { mutableStateOf(false) }
        var enteredText by remember { mutableStateOf("") }
        val snackbarHostState = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()

        // Determine the timer text color based on theme and current phase
        val isLightTheme = MaterialTheme.colorScheme.background.luminance() > 0.5
        val timerTextColor = when {
            // Light mode: always black
            isLightTheme -> Color.Black
            // Dark mode: white normally, black during yellow phase
            !isLightTheme && ticks >= yellowThreshold && ticks < redThreshold -> Color.Black
            else -> Color.White
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(animatedBackgroundColor),
            contentAlignment = Alignment.Center
        ) {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.TopCenter)
            )

            if (isUIVisible) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Empty space at the top (1/3 of screen)
                    Spacer(modifier = Modifier.weight(1f))

                    // Timer display (positioned at approximately 2/3 of screen height)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(2f),
                        verticalArrangement = Arrangement.Top
                    ) {
                        Text(
                            text = if (isTimeVisible) formatTime(ticks) else "     ",
                            style = TextStyle(
                                fontSize = 60.sp,
                                fontWeight = FontWeight.Bold,
                                color = timerTextColor
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Progress bar with enhanced visibility
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
                                maxSeconds = maxDuration,
                                greenThreshold = greenThreshold,
                                yellowThreshold = yellowThreshold,
                                redThreshold = redThreshold,
                                modifier = Modifier.fillMaxWidth(),
                                showLabels = true
                            )
                        }
                    }

                    // Control buttons at bottom
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 32.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Reset button - only visible when ticks > 0
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            if (ticks > 0) {
                                Button(
                                    onClick = {
                                        ticks = 0
                                        isTimerRunning = false
                                        targetBackgroundColor = Color.Transparent
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

                        // Play/Pause button - always visible
                        Box(
                            modifier = Modifier.weight(1.5f),
                            contentAlignment = Alignment.Center
                        ) {
                            Button(
                                onClick = { isTimerRunning = !isTimerRunning },
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

                        // Save button - only visible when paused AND ticks > 0
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
                }
            }
        }

        if (showDialog && isUIVisible) {
            InputDialog(
                onDismiss = { showDialog = false },
                onConfirm = { name ->
                    // Save the speech record to the database
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
                            // Navigate immediately if action is clicked
                            navController.navigate(Screen.SavedDataScreen.route)
                        } else {
                            // Auto-navigate after a short delay
                            delay(1500)
                            navController.navigate(Screen.SavedDataScreen.route)
                        }
                    }
                    showDialog = false
                },
                title = "Save Time",
                initialText = enteredText
            )
        }
    }
}