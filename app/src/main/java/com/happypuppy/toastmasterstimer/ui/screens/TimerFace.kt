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
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.happypuppy.toastmasterstimer.R
import com.happypuppy.toastmasterstimer.ui.components.TimerProgressBar
import com.happypuppy.toastmasterstimer.ui.theme.LocalPhaseColors
import com.happypuppy.toastmasterstimer.ui.theme.PausedDigitsAlpha
import com.happypuppy.toastmasterstimer.ui.theme.PhasePalette
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
    onSave: () -> Unit,
    onToggleUiVisibility: (() -> Unit)? = null
) {
    val phasePalette = LocalPhaseColors.current.paletteFor(phase)

    val animatedBackgroundColor by animateColorAsState(
        targetValue = phasePalette.background,
        animationSpec = tween(durationMillis = 800),
        label = "backgroundColor"
    )

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
            .padding(innerPadding)
            .consumeWindowInsets(innerPadding)
            .displayCutoutPadding(),
        contentAlignment = Alignment.Center
    ) {
        if (isUiVisible) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val maxWidthDp = maxWidth.value
                val maxHeightDp = maxHeight.value
                val isSideBySide = TimerLayoutMetrics.isSideBySide(maxWidthDp, maxHeightDp)
                val isCompactHeight = maxHeightDp < TimerLayoutMetrics.COMPACT_HEIGHT_THRESHOLD_DP
                val fontScale = LocalDensity.current.fontScale
                val digitFontSizeSp = TimerLayoutMetrics.digitFontSizeSp(
                    maxWidthDp = maxWidthDp,
                    maxHeightDp = maxHeightDp,
                    fontScale = fontScale,
                    isSideBySide = isSideBySide
                )

                if (isSideBySide) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1.2f)
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            TimerReadout(
                                phase = phase,
                                phaseLabel = phaseLabel,
                                phaseIcon = cue.icon,
                                ticks = ticks,
                                spokenTime = spokenTime,
                                isPaused = isPaused,
                                isRestored = isRestored,
                                thresholds = thresholds,
                                phasePalette = phasePalette,
                                digitFontSizeSp = digitFontSizeSp,
                                isCompact = true,
                                progressWidthFraction = 0.95f
                            )
                        }

                        Column(
                            modifier = Modifier
                                .weight(0.85f)
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            TimerLandscapeControls(
                                ticks = ticks,
                                isTimerRunning = isTimerRunning,
                                buttonAspectRatio = buttonAspectRatio,
                                phasePalette = phasePalette,
                                onRestart = onRestart,
                                onToggleRunning = onToggleRunning,
                                onSave = onSave
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = stringResource(R.string.timer_foreground_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = phasePalette.secondaryOnBackground,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = if (isCompactHeight) Arrangement.SpaceBetween else Arrangement.SpaceEvenly,
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = if (isCompactHeight) 12.dp else 24.dp)
                    ) {
                        if (!isCompactHeight) {
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        TimerReadout(
                            phase = phase,
                            phaseLabel = phaseLabel,
                            phaseIcon = cue.icon,
                            ticks = ticks,
                            spokenTime = spokenTime,
                            isPaused = isPaused,
                            isRestored = isRestored,
                            thresholds = thresholds,
                            phasePalette = phasePalette,
                            digitFontSizeSp = digitFontSizeSp,
                            isCompact = isCompactHeight,
                            progressWidthFraction = 0.88f
                        )

                        Spacer(modifier = Modifier.height(if (isCompactHeight) 16.dp else 28.dp))

                        TimerPortraitControls(
                            ticks = ticks,
                            isTimerRunning = isTimerRunning,
                            buttonSize = 64.dp,
                            buttonAspectRatio = buttonAspectRatio,
                            phasePalette = phasePalette,
                            onRestart = onRestart,
                            onToggleRunning = onToggleRunning,
                            onSave = onSave,
                            verticalPadding = if (isCompactHeight) 8.dp else 16.dp
                        )

                        Text(
                            text = stringResource(R.string.timer_foreground_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = phasePalette.secondaryOnBackground,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        } else {
            // Speaker-facing (pure-color) mode draws no phase text or digits, but keeps the phase in
            // the semantics tree so a screen reader still hears phase changes (polite live region).
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .semantics {
                        contentDescription = phaseLabel
                        liveRegion = LiveRegionMode.Polite
                    }
            )

            if (onToggleUiVisibility != null) {
                FilledTonalIconButton(
                    onClick = onToggleUiVisibility,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = phasePalette.secondaryControlContainer,
                        contentColor = phasePalette.onSecondaryControlContainer
                    ),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .size(48.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.visibility_off_24px),
                        contentDescription = stringResource(R.string.timer_show_controls)
                    )
                }
            }
        }
    }
}

@Composable
private fun TimerReadout(
    phase: TimerPhase,
    phaseLabel: String,
    phaseIcon: PhaseIcon,
    ticks: Int,
    spokenTime: String,
    isPaused: Boolean,
    isRestored: Boolean,
    thresholds: TimerThresholds,
    phasePalette: PhasePalette,
    digitFontSizeSp: Float,
    isCompact: Boolean,
    progressWidthFraction: Float
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (isRestored) {
            Text(
                text = stringResource(R.string.timer_restored),
                style = MaterialTheme.typography.labelLarge,
                color = phasePalette.onBackground,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        PhaseChip(label = phaseLabel, icon = phaseIcon, phasePalette = phasePalette)

        val digitStyle = MaterialTheme.typography.displayLarge.copy(
            fontSize = digitFontSizeSp.sp,
            lineHeight = (digitFontSizeSp * 1.05f).sp,
            fontWeight = FontWeight.Bold,
            fontFeatureSettings = "tnum"
        )

        // One spoken description ("1 minute 4 seconds, green phase") instead of the raw "01:04".
        // No live region here: the digits change every second.
        Text(
            text = formatTime(ticks),
            style = digitStyle,
            color = if (isPaused) {
                phasePalette.onBackground.copy(alpha = PausedDigitsAlpha)
            } else {
                phasePalette.onBackground
            },
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.semantics { contentDescription = spokenTime }
        )

        // The row is always laid out so pausing does not move the controls. Outside the paused
        // state it is invisible and has no semantics (no "Paused" node).
        Text(
            text = stringResource(R.string.timer_paused),
            style = MaterialTheme.typography.titleMedium,
            color = phasePalette.secondaryOnBackground,
            modifier = Modifier
                .padding(top = 4.dp)
                .then(
                    if (isPaused) Modifier
                    else Modifier.graphicsLayer { alpha = 0f }.clearAndSetSemantics { }
                )
        )

        Spacer(modifier = Modifier.height(if (isCompact) 12.dp else 18.dp))

        TimerProgressBar(
            currentSeconds = ticks,
            maxSeconds = thresholds.maxDurationSeconds,
            greenThreshold = thresholds.greenSeconds,
            yellowThreshold = thresholds.yellowSeconds,
            redThreshold = thresholds.redSeconds,
            phase = phase,
            palette = phasePalette,
            modifier = Modifier
                .fillMaxWidth(progressWidthFraction)
                .padding(horizontal = 8.dp),
            showLabels = true
        )
    }
}

@Composable
private fun TimerPortraitControls(
    ticks: Int,
    isTimerRunning: Boolean,
    buttonSize: Dp,
    buttonAspectRatio: Float,
    phasePalette: PhasePalette,
    onRestart: () -> Unit,
    onToggleRunning: () -> Unit,
    onSave: () -> Unit,
    verticalPadding: Dp
) {
    val primaryColors = ButtonDefaults.buttonColors(
        containerColor = phasePalette.primaryControlContainer,
        contentColor = phasePalette.onPrimaryControlContainer
    )
    val secondaryColors = ButtonDefaults.buttonColors(
        containerColor = phasePalette.secondaryControlContainer,
        contentColor = phasePalette.onSecondaryControlContainer
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = verticalPadding),
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
                    colors = secondaryColors,
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
                colors = primaryColors,
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
                    colors = secondaryColors,
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
}

@Composable
private fun TimerLandscapeControls(
    ticks: Int,
    isTimerRunning: Boolean,
    buttonAspectRatio: Float,
    phasePalette: PhasePalette,
    onRestart: () -> Unit,
    onToggleRunning: () -> Unit,
    onSave: () -> Unit
) {
    val primarySize = 60.dp
    val secondarySize = 56.dp
    val primaryColors = ButtonDefaults.buttonColors(
        containerColor = phasePalette.primaryControlContainer,
        contentColor = phasePalette.onPrimaryControlContainer
    )
    val secondaryColors = ButtonDefaults.buttonColors(
        containerColor = phasePalette.secondaryControlContainer,
        contentColor = phasePalette.onSecondaryControlContainer
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Button(
            onClick = onToggleRunning,
            colors = primaryColors,
            modifier = Modifier
                .height(primarySize)
                .width(primarySize * buttonAspectRatio)
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

        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(secondarySize),
                contentAlignment = Alignment.Center
            ) {
                if (ticks > 0) {
                    Button(
                        onClick = onRestart,
                        colors = secondaryColors,
                        modifier = Modifier
                            .size(secondarySize)
                            .clip(CircleShape),
                        shape = CircleShape,
                        contentPadding = PaddingValues(4.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.restart_alt_24px),
                            contentDescription = stringResource(R.string.timer_action_restart),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            Box(
                modifier = Modifier.size(secondarySize),
                contentAlignment = Alignment.Center
            ) {
                if (!isTimerRunning && ticks > 0) {
                    Button(
                        onClick = onSave,
                        colors = secondaryColors,
                        modifier = Modifier
                            .size(secondarySize)
                            .clip(CircleShape),
                        shape = CircleShape,
                        contentPadding = PaddingValues(4.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.save_24px),
                            contentDescription = stringResource(R.string.timer_action_save),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * The visible phase cue: an icon plus the phase label inside a high-contrast pill. It is its own
 * semantics node and carries the screen's only live region, so it is announced when the phase
 * changes and never while the time ticks.
 */
@Composable
private fun PhaseChip(label: String, icon: PhaseIcon, phasePalette: PhasePalette) {
    Surface(
        shape = CircleShape,
        color = phasePalette.chipContainer,
        contentColor = phasePalette.onChipContainer,
        modifier = Modifier
            .padding(bottom = 12.dp)
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // The label carries the meaning, so the icon is decorative for screen readers.
            when (icon) {
                PhaseIcon.HOURGLASS -> Icon(painterResource(R.drawable.hourglass_top_24px), null)
                PhaseIcon.CHECK -> Icon(Icons.Filled.CheckCircle, null)
                PhaseIcon.WARNING -> Icon(Icons.Filled.Warning, null)
                PhaseIcon.STOP -> Icon(painterResource(R.drawable.stop_24px), null)
            }
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = phasePalette.onChipContainer
            )
        }
    }
}
