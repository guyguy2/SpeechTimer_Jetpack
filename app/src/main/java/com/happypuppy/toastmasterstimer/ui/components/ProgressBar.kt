package com.happypuppy.toastmasterstimer.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.happypuppy.toastmasterstimer.R
import kotlin.math.roundToInt

/**
 * A custom progress bar that changes color based on the current progress
 * with markers indicating transition points
 *
 * @param currentSeconds Current time in seconds
 * @param maxSeconds Maximum time in seconds
 * @param greenThreshold Seconds at which to switch from transparent to green
 * @param yellowThreshold Seconds at which to switch from green to yellow
 * @param redThreshold Seconds at which to switch from yellow to red
 * @param modifier Optional modifier for the component
 * @param showLabels Whether to show text labels under the markers
 */
@Composable
fun TimerProgressBar(
    currentSeconds: Int,
    maxSeconds: Int,
    greenThreshold: Int,
    yellowThreshold: Int,
    redThreshold: Int,
    modifier: Modifier = Modifier,
    showLabels: Boolean = true
) {
    // Calculate progress from 0.0 to 1.0
    val progress = minOf(currentSeconds.toFloat() / maxSeconds.toFloat(), 1f)

    // Animate the progress value
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 300),
        label = "progressAnimation"
    )

    // Determine the color of the progress bar with enhanced visibility
    val progressColor = when {
        currentSeconds < greenThreshold -> Color.LightGray.copy(alpha = 0.7f) // Light gray instead of transparent
        currentSeconds < yellowThreshold -> Color(0xFF00FF00) // Bright Green for better visibility
        currentSeconds < redThreshold -> Color(0xFFFFFF00) // Bright Yellow
        else -> Color(0xFFFF4444) // Brighter Red
    }

    // Calculate relative positions of thresholds
    val greenPosition = greenThreshold.toFloat() / maxSeconds.toFloat()
    val yellowPosition = yellowThreshold.toFloat() / maxSeconds.toFloat()
    val redPosition = redThreshold.toFloat() / maxSeconds.toFloat()

    Column(modifier = modifier) {
        // Draw the progress bar with enhanced visibility and markers
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp) // Increased height for better visibility
        ) {
            // Background track with higher contrast
            drawLine(
                color = Color.DarkGray.copy(alpha = 0.7f), // Darker background for contrast
                start = Offset(0f, size.height / 2),
                end = Offset(size.width, size.height / 2),
                strokeWidth = size.height,
                cap = StrokeCap.Round
            )

            // Progress indicator with semi-transparent white outline for contrast
            drawLine(
                color = progressColor,
                start = Offset(0f, size.height / 2),
                end = Offset(size.width * animatedProgress, size.height / 2),
                strokeWidth = size.height * 0.9f, // Slightly smaller to show outline effect
                cap = StrokeCap.Round
            )

            // Draw marker for green threshold
            drawLine(
                color = Color.White,
                start = Offset(size.width * greenPosition, 0f),
                end = Offset(size.width * greenPosition, size.height),
                strokeWidth = 2.dp.toPx()
            )

            // Draw marker for yellow threshold
            drawLine(
                color = Color.White,
                start = Offset(size.width * yellowPosition, 0f),
                end = Offset(size.width * yellowPosition, size.height),
                strokeWidth = 2.dp.toPx()
            )

            // Draw marker for red threshold
            drawLine(
                color = Color.White,
                start = Offset(size.width * redPosition, 0f),
                end = Offset(size.width * redPosition, size.height),
                strokeWidth = 2.dp.toPx()
            )

            // Draw indicator circle with white outline for better visibility
            drawCircle(
                color = Color.White.copy(alpha = 0.4f),
                radius = size.height * 1.2f, // Slightly larger for outline effect
                center = Offset(size.width * animatedProgress, size.height / 2)
            )

            // Draw main indicator circle
            drawCircle(
                color = progressColor,
                radius = size.height,
                center = Offset(size.width * animatedProgress, size.height / 2)
            )
        }

        // Add marker labels aligned with their respective markers
        if (showLabels) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            ) {
                // We use a custom layout to precisely position each label
                Layout(
                    content = {
                        Icon(
                            painter = painterResource(R.drawable.start_24px),
                            modifier = Modifier.size(12.dp),
                            contentDescription = "start"
                        )

                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(Color(0xFF00FF00), CircleShape)
                        )

                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(Color(0xFFFFFF00), CircleShape)
                        )

                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(Color(0xFFFF4444), CircleShape)
                        )
                    }
                ) { measurables, constraints ->
                    // Measure each label
                    val placeables = measurables.map { it.measure(constraints) }

                    // Calculate the positions for each label
                    val gLabelWidth = placeables[1].width
                    val yLabelWidth = placeables[2].width
                    val rLabelWidth = placeables[3].width

                    // Find the height of the tallest label
                    val height = placeables.maxOf { it.height }

                    // Calculate x positions for each label to center them under their markers
                    val startX = 0
                    val gX = (constraints.maxWidth * greenPosition).roundToInt() - (gLabelWidth / 2)
                    val yX = (constraints.maxWidth * yellowPosition).roundToInt() - (yLabelWidth / 2)
                    val rX = (constraints.maxWidth * redPosition).roundToInt() - (rLabelWidth / 2)

                    // Layout with exact positioning
                    layout(constraints.maxWidth, height) {
                        // Place start label at the beginning
                        placeables[0].place(startX, 0)

                        // Place G label centered under green marker
                        placeables[1].place(gX, 0)

                        // Place Y label centered under yellow marker
                        placeables[2].place(yX, 0)

                        // Place R label centered under red marker
                        placeables[3].place(rX, 0)
                    }
                }
            }
        }
    }
}