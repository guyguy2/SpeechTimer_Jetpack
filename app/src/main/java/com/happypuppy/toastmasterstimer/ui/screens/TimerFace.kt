package com.happypuppy.toastmasterstimer.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.happypuppy.toastmasterstimer.R
import com.happypuppy.toastmasterstimer.ui.components.TimerProgressBar
import com.happypuppy.toastmasterstimer.util.TimerPhase
import com.happypuppy.toastmasterstimer.util.TimerThresholds
import com.happypuppy.toastmasterstimer.util.formatTime

/**
 * The phase-coloured part of the timer screen, driven by plain values so it can be shown at any
 * phase without a ViewModel, Koin or navigation.
 */
@Composable
internal fun TimerFace(
    phase: TimerPhase,
    ticks: Int,
    isTimerRunning: Boolean,
    isUiVisible: Boolean,
    isRestored: Boolean,
    thresholds: TimerThresholds,
    innerPadding: PaddingValues,
    onRestart: () -> Unit,
    onToggleRunning: () -> Unit,
    onSave: () -> Unit
) {
    val targetBackgroundColor = when (phase) {
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

    val timerTextColor = when (phase) {
        TimerPhase.BEFORE_GREEN -> MaterialTheme.colorScheme.onBackground
        TimerPhase.GREEN -> PhaseOnGreenColor
        TimerPhase.YELLOW -> PhaseOnYellowColor
        TimerPhase.RED -> PhaseOnRedColor
    }

    val cue = phaseCue(phase)
    val phaseLabel = stringResource(cue.labelRes)
    val resources = LocalResources.current
    val spokenTime = stringResource(
        R.string.timer_spoken_with_phase,
        spokenDuration(
            totalSeconds = ticks,
            minutesText = { resources.getQuantityString(R.plurals.timer_spoken_minutes, it, it) },
            secondsText = { resources.getQuantityString(R.plurals.timer_spoken_seconds, it, it) }
        ),
        stringResource(cue.spokenRes)
    )
    val isPaused = !isTimerRunning && ticks > 0

    val buttonSize = 64.dp
    val buttonAspectRatio by animateFloatAsState(
        targetValue = if (isTimerRunning) 2.0f else 1.5f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "aspectRatio"
    )

    // Background is placed BEFORE innerPadding so the phase color extends edge-to-edge behind system bars
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(animatedBackgroundColor)
            .padding(innerPadding),
        contentAlignment = Alignment.Center
    ) {
        if (isUiVisible) {
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
                        if (isRestored) {
                            Text(
                                text = "Timer restored",
                                style = MaterialTheme.typography.labelLarge,
                                color = timerTextColor,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }

                        PhaseChip(label = phaseLabel, icon = cue.icon, color = timerTextColor)

                        // One spoken description ("1 minute 4 seconds, green phase") instead of the raw
                        // "01:04". No live region here: the digits change every second.
                        Text(
                            text = formatTime(ticks),
                            style = TextStyle(
                                fontSize = if (isCompactHeight) 48.sp else 60.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isPaused) timerTextColor.copy(alpha = PausedDigitsAlpha) else timerTextColor
                            ),
                            modifier = Modifier.semantics { contentDescription = spokenTime }
                        )

                        // The row is always laid out so pausing does not move the buttons below it. Outside
                        // the paused state it is invisible and has no semantics (no "Paused" node).
                        Text(
                            text = stringResource(R.string.timer_paused),
                            style = MaterialTheme.typography.titleMedium,
                            color = timerTextColor,
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .then(
                                    if (isPaused) Modifier
                                    else Modifier.graphicsLayer { alpha = 0f }.clearAndSetSemantics { }
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
                                    onClick = onRestart,
                                    modifier = Modifier
                                        .size(buttonSize)
                                        .clip(CircleShape),
                                    shape = CircleShape,
                                    contentPadding = PaddingValues(4.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.restart_alt_24px),
                                        contentDescription = stringResource(R.string.timer_action_restart),
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
                                onClick = onToggleRunning,
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
                                    contentDescription = stringResource(
                                        if (isTimerRunning) R.string.timer_action_pause else R.string.timer_action_start
                                    ),
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
                                    onClick = onSave,
                                    contentPadding = PaddingValues(4.dp),
                                    modifier = Modifier
                                        .size(buttonSize)
                                        .clip(CircleShape),
                                    shape = CircleShape
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.save_24px),
                                        contentDescription = stringResource(R.string.timer_action_save),
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
        } else {
            // Pure-color mode draws nothing for the phase, but keeps it in the semantics tree so a
            // screen reader still hears phase changes (same polite live region as the visible chip).
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .semantics {
                        contentDescription = phaseLabel
                        liveRegion = LiveRegionMode.Polite
                    }
            )
        }
    }
}

/**
 * The visible phase cue: an icon plus the phase label. It is its own semantics node and carries the
 * screen's only live region, so it is announced when the phase changes and never while the time ticks.
 */
@Composable
private fun PhaseChip(label: String, icon: PhaseIcon, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .padding(bottom = 8.dp)
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }
    ) {
        // The label carries the meaning, so the icon is decorative for screen readers.
        when (icon) {
            PhaseIcon.HOURGLASS -> Icon(painterResource(R.drawable.hourglass_top_24px), null, tint = color)
            PhaseIcon.CHECK -> Icon(Icons.Filled.CheckCircle, null, tint = color)
            PhaseIcon.WARNING -> Icon(Icons.Filled.Warning, null, tint = color)
            PhaseIcon.STOP -> Icon(painterResource(R.drawable.stop_24px), null, tint = color)
        }
        Text(text = label, style = MaterialTheme.typography.titleMedium, color = color)
    }
}
