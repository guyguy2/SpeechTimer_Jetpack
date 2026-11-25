package com.happypuppy.toastmasterstimer.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.unit.dp
import com.happypuppy.toastmasterstimer.R
import com.happypuppy.toastmasterstimer.ui.theme.LocalPhaseColors
import com.happypuppy.toastmasterstimer.ui.theme.PhasePalette
import com.happypuppy.toastmasterstimer.util.TimerPhase
import kotlin.math.roundToInt

/**
 * Elapsed-time progress bar with threshold markers styled from semantic [PhasePalette] tokens
 * so track, progress fill, and threshold markers stay high-contrast on every phase background.
 */
@Composable
fun TimerProgressBar(
    currentSeconds: Int,
    maxSeconds: Int,
    greenThreshold: Int,
    yellowThreshold: Int,
    redThreshold: Int,
    modifier: Modifier = Modifier,
    showLabels: Boolean = true,
    phase: TimerPhase = when {
        currentSeconds < greenThreshold -> TimerPhase.BEFORE_GREEN
        currentSeconds < yellowThreshold -> TimerPhase.GREEN
        currentSeconds < redThreshold -> TimerPhase.YELLOW
        else -> TimerPhase.RED
    },
    palette: PhasePalette = LocalPhaseColors.current.paletteFor(phase)
) {
    val safeMax = maxSeconds.coerceAtLeast(1).toFloat()
    val progress = (currentSeconds.toFloat() / safeMax).coerceIn(0f, 1f)

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 300),
        label = "progressAnimation"
    )

    val greenPosition = (greenThreshold.toFloat() / safeMax).coerceIn(0f, 1f)
    val yellowPosition = (yellowThreshold.toFloat() / safeMax).coerceIn(0f, 1f)
    val redPosition = (redThreshold.toFloat() / safeMax).coerceIn(0f, 1f)

    val progressDescription = stringResource(R.string.timer_progress_description)
    Column(
        modifier = modifier.clearAndSetSemantics {
            // One node for the whole bar; the dots and the start marker are decoration.
            contentDescription = progressDescription
            progressBarRangeInfo = ProgressBarRangeInfo(
                current = currentSeconds.toFloat().coerceIn(0f, safeMax),
                range = 0f..safeMax
            )
        }
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp)
        ) {
            val trackHeight = 10.dp.toPx().coerceAtMost(size.height * 0.7f)
            val trackTop = (size.height - trackHeight) / 2f
            val cornerRadius = CornerRadius(trackHeight / 2f, trackHeight / 2f)
            val borderStroke = 1.5.dp.toPx()

            // Recessed track fill
            drawRoundRect(
                color = palette.progressTrack,
                topLeft = Offset(0f, trackTop),
                size = Size(size.width, trackHeight),
                cornerRadius = cornerRadius
            )

            // Active progress indicator
            val indicatorEnd = (size.width * animatedProgress).coerceIn(0f, size.width)
            if (indicatorEnd > 0f) {
                drawLine(
                    color = palette.progressIndicator,
                    start = Offset(trackHeight / 2f, size.height / 2f),
                    end = Offset(indicatorEnd.coerceAtLeast(trackHeight / 2f), size.height / 2f),
                    strokeWidth = trackHeight - borderStroke,
                    cap = StrokeCap.Round
                )
            }

            // Crisp track outline for >= 3:1 non-text visibility against every phase canvas
            drawRoundRect(
                color = palette.progressTrackBorder,
                topLeft = Offset(0f, trackTop),
                size = Size(size.width, trackHeight),
                cornerRadius = cornerRadius,
                style = Stroke(width = borderStroke)
            )

            // Threshold ticks
            val tickStroke = 2.dp.toPx()
            listOf(greenPosition, yellowPosition, redPosition).forEach { pos ->
                val x = size.width * pos
                drawLine(
                    color = palette.progressTrackBorder,
                    start = Offset(x, trackTop - 2.dp.toPx()),
                    end = Offset(x, trackTop + trackHeight + 2.dp.toPx()),
                    strokeWidth = tickStroke,
                    cap = StrokeCap.Round
                )
            }

            // Progress thumb
            val thumbCenter = Offset(indicatorEnd, size.height / 2f)
            val thumbRadius = (trackHeight * 0.72f).coerceAtMost(size.height / 2f)
            drawCircle(
                color = palette.progressIndicator,
                radius = thumbRadius,
                center = thumbCenter
            )
            drawCircle(
                color = palette.markerBorder,
                radius = thumbRadius,
                center = thumbCenter,
                style = Stroke(width = 1.5.dp.toPx())
            )
        }

        if (showLabels) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
            ) {
                Layout(
                    content = {
                        Icon(
                            painter = painterResource(R.drawable.start_24px),
                            modifier = Modifier.size(14.dp),
                            contentDescription = null,
                            tint = palette.onBackground
                        )

                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(palette.markerGreen, CircleShape)
                                .border(1.5.dp, palette.markerBorder, CircleShape)
                        )

                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(palette.markerYellow, CircleShape)
                                .border(1.5.dp, palette.markerBorder, CircleShape)
                        )

                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(palette.markerRed, CircleShape)
                                .border(1.5.dp, palette.markerBorder, CircleShape)
                        )
                    }
                ) { measurables, constraints ->
                    val placeables = measurables.map { it.measure(constraints) }
                    val gLabelWidth = placeables[1].width
                    val yLabelWidth = placeables[2].width
                    val rLabelWidth = placeables[3].width
                    val height = placeables.maxOf { it.height }

                    fun clampedX(position: Float, labelWidth: Int): Int {
                        val maxX = (constraints.maxWidth - labelWidth).coerceAtLeast(0)
                        return ((constraints.maxWidth * position).roundToInt() - (labelWidth / 2))
                            .coerceIn(0, maxX)
                    }

                    val gX = clampedX(greenPosition, gLabelWidth)
                    val yX = clampedX(yellowPosition, yLabelWidth)
                    val rX = clampedX(redPosition, rLabelWidth)

                    layout(constraints.maxWidth, height) {
                        placeables[0].place(0, (height - placeables[0].height) / 2)
                        placeables[1].place(gX, (height - placeables[1].height) / 2)
                        placeables[2].place(yX, (height - placeables[2].height) / 2)
                        placeables[3].place(rX, (height - placeables[3].height) / 2)
                    }
                }
            }
        }
    }
}
