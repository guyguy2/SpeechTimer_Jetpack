package com.happypuppy.toastmasterstimer.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.happypuppy.toastmasterstimer.R
import com.happypuppy.toastmasterstimer.model.SpeechType
import com.happypuppy.toastmasterstimer.ui.theme.LocalPhaseColors
import com.happypuppy.toastmasterstimer.util.TimerThresholds
import java.util.Locale

@Immutable
data class PresetCueTimes(
    val greenText: String,
    val yellowText: String,
    val redText: String
)

fun formatCueTime(seconds: Int): String {
    val safeSeconds = seconds.coerceAtLeast(0)
    val minutes = safeSeconds / 60
    val remainingSeconds = safeSeconds % 60
    return String.format(Locale.US, "%d:%02d", minutes, remainingSeconds)
}

fun presetCueTimes(minMinutes: Int, maxMinutes: Int): PresetCueTimes {
    val thresholds = TimerThresholds(minMinutes, maxMinutes)
    return PresetCueTimes(
        greenText = formatCueTime(thresholds.greenSeconds),
        yellowText = formatCueTime(thresholds.yellowSeconds),
        redText = formatCueTime(thresholds.redSeconds)
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PresetCard(
    speechType: SpeechType,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val minMinutes = requireNotNull(speechType.presetMinMinutes) { "${speechType.id} has no preset range" }
    val maxMinutes = requireNotNull(speechType.presetMaxMinutes) { "${speechType.id} has no preset range" }
    val cueTimes = remember(minMinutes, maxMinutes) { presetCueTimes(minMinutes, maxMinutes) }
    val title = stringResource(speechType.cardTitleRes)
    val rangeLabel = stringResource(R.string.preset_duration_range, minMinutes, maxMinutes)
    val cardDescription = stringResource(
        R.string.preset_card_cd,
        title,
        rangeLabel,
        cueTimes.greenText,
        cueTimes.yellowText,
        cueTimes.redText
    )

    val beforeGreenPalette = LocalPhaseColors.current.beforeGreen
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 92.dp)
            .semantics { contentDescription = cardDescription },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = if (isFocused) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        },
        interactionSource = interactionSource
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    CueDotTime(color = beforeGreenPalette.markerGreen, timeText = cueTimes.greenText)
                    CueDotTime(color = beforeGreenPalette.markerYellow, timeText = cueTimes.yellowText)
                    CueDotTime(color = beforeGreenPalette.markerRed, timeText = cueTimes.redText)
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ) {
                    Text(
                        text = rangeLabel,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CueDotTime(
    color: Color,
    timeText: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = timeText,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
