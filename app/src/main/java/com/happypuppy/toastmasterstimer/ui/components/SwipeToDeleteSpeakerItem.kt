package com.happypuppy.toastmasterstimer.ui.components

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.happypuppy.toastmasterstimer.R
import com.happypuppy.toastmasterstimer.data.database.repository.Speaker
import com.happypuppy.toastmasterstimer.ui.screens.SpeakerCard
import com.happypuppy.toastmasterstimer.ui.theme.LocalIsDarkTheme

const val SWIPE_DELETE_BACKGROUND_TAG = "swipe_delete_background"
const val SWIPE_DELETE_ICON_TAG = "swipe_delete_icon"

internal const val SWIPE_THRESHOLD_ICON_SCALE = 1.18f
internal const val DARK_PRE_THRESHOLD_ALPHA = 0.45f
private const val SWIPE_DISMISS_POSITIONAL_THRESHOLD_FRACTION = 0.45f

internal val SwipeDeleteBackgroundColorKey = SemanticsPropertyKey<Color>("SwipeDeleteBackgroundColor")

/**
 * Resolved visual state for [SwipeToDeleteSpeakerItem]'s reveal background and delete icon.
 * Kept as a pure value model so WCAG contrast and state transitions are verified on the JVM.
 */
data class SwipeDeleteVisuals(
    val backgroundColor: Color,
    val iconTint: Color,
    val iconAlignment: Alignment,
    val showIcon: Boolean,
    val isThresholdCrossed: Boolean
)

/**
 * Resolves two-stage destructive swipe visuals from the active [colorScheme], theme polarity,
 * current [dismissDirection], and commit [targetValue] (past the positional threshold).
 */
fun resolveSwipeDeleteVisuals(
    colorScheme: ColorScheme,
    isDarkTheme: Boolean,
    dismissDirection: SwipeToDismissBoxValue,
    targetValue: SwipeToDismissBoxValue
): SwipeDeleteVisuals {
    if (dismissDirection == SwipeToDismissBoxValue.Settled) {
        return SwipeDeleteVisuals(
            backgroundColor = Color.Transparent,
            iconTint = Color.Transparent,
            iconAlignment = Alignment.CenterEnd,
            showIcon = false,
            isThresholdCrossed = false
        )
    }

    val iconAlignment = when (dismissDirection) {
        SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
        SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
        SwipeToDismissBoxValue.Settled -> Alignment.CenterEnd
    }
    val isThresholdCrossed = targetValue != SwipeToDismissBoxValue.Settled

    val (backgroundColor, iconTint) = when {
        !isDarkTheme && !isThresholdCrossed ->
            colorScheme.errorContainer to colorScheme.onErrorContainer
        !isDarkTheme ->
            colorScheme.error to colorScheme.onError
        !isThresholdCrossed ->
            colorScheme.errorContainer.copy(alpha = DARK_PRE_THRESHOLD_ALPHA)
                .compositeOver(colorScheme.surfaceContainerHigh) to colorScheme.onErrorContainer
        else ->
            colorScheme.errorContainer to colorScheme.onErrorContainer
    }

    return SwipeDeleteVisuals(
        backgroundColor = backgroundColor,
        iconTint = iconTint,
        iconAlignment = iconAlignment,
        showIcon = true,
        isThresholdCrossed = isThresholdCrossed
    )
}

internal fun isReducedMotionEnabled(context: Context): Boolean {
    val scale = try {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        )
    } catch (_: Throwable) {
        1f
    }
    return scale == 0f
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeToDeleteSpeakerItem(
    speaker: Speaker,
    isVibrationEnabled: Boolean,
    onEditClick: () -> Unit,
    onShareClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    restoreVersion: Int = 0
) {
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    val isDarkTheme = LocalIsDarkTheme.current
    val reducedMotion = remember(context) { isReducedMotionEnabled(context) }
    var deleteDispatched by remember(speaker.id, restoreVersion) { mutableStateOf(false) }

    val dismissState = rememberSwipeToDismissBoxState(
        positionalThreshold = { it * SWIPE_DISMISS_POSITIONAL_THRESHOLD_FRACTION }
    )

    LaunchedEffect(speaker.id, restoreVersion) {
        deleteDispatched = false
        if (dismissState.currentValue != SwipeToDismissBoxValue.Settled) {
            dismissState.snapTo(SwipeToDismissBoxValue.Settled)
        }
    }

    val isThresholdCrossed = dismissState.targetValue != SwipeToDismissBoxValue.Settled
    var wasThresholdCrossed by remember { mutableStateOf(false) }
    LaunchedEffect(isThresholdCrossed, isVibrationEnabled) {
        if (isThresholdCrossed && !wasThresholdCrossed && isVibrationEnabled) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
        }
        wasThresholdCrossed = isThresholdCrossed
    }

    val deleteOnce = {
        if (!deleteDispatched) {
            deleteDispatched = true
            if (isVibrationEnabled) {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            onDelete()
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        onDismiss = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                deleteOnce()
            }
        },
        backgroundContent = {
            val visuals = resolveSwipeDeleteVisuals(
                colorScheme = MaterialTheme.colorScheme,
                isDarkTheme = isDarkTheme,
                dismissDirection = dismissState.dismissDirection,
                targetValue = dismissState.targetValue
            )
            val backgroundColor = if (reducedMotion) {
                visuals.backgroundColor
            } else {
                animateColorAsState(
                    targetValue = visuals.backgroundColor,
                    label = "backgroundColor"
                ).value
            }
            val iconScale = if (reducedMotion) {
                1f
            } else {
                animateFloatAsState(
                    targetValue = if (visuals.isThresholdCrossed) SWIPE_THRESHOLD_ICON_SCALE else 1f
                ).value
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(backgroundColor)
                    .testTag(SWIPE_DELETE_BACKGROUND_TAG)
                    .semantics { set(SwipeDeleteBackgroundColorKey, backgroundColor) },
                contentAlignment = visuals.iconAlignment
            ) {
                if (visuals.showIcon) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.common_delete),
                        tint = visuals.iconTint,
                        modifier = Modifier
                            .testTag(SWIPE_DELETE_ICON_TAG)
                            .padding(horizontal = 20.dp)
                            .scale(iconScale)
                    )
                }
            }
        }
    ) {
        SpeakerCard(
            speaker = speaker,
            onEditClick = onEditClick,
            onShareClick = onShareClick,
            onDeleteClick = deleteOnce
        )
    }
}
