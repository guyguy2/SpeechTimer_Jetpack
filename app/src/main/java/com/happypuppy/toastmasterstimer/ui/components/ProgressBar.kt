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
 * with markers indicating transition points.
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
    val safeMax = maxSeconds.coerceAtLeast(1).toFloat()
    val progress = (currentSeconds.toFloat() / safeMax).coerceIn(0f, 1f)

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 300),
        label = "progressAnimation"
    )

    val progressColor = when {
        currentSeconds < greenThreshold -> Color.LightGray.copy(alpha = 0.7f)
        currentSeconds < yellowThreshold -> Color(0xFF00FF00)
        currentSeconds < redThreshold -> Color(0xFFFFFF00)
        else -> Color(0xFFFF4444)
    }

    val greenPosition = (greenThreshold.toFloat() / safeMax).coerceIn(0f, 1f)
    val yellowPosition = (yellowThreshold.toFloat() / safeMax).coerceIn(0f, 1f)
    val redPosition = (redThreshold.toFloat() / safeMax).coerceIn(0f, 1f)

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
        ) {
            drawLine(
                color = Color.DarkGray.copy(alpha = 0.7f),
                start = Offset(0f, size.height / 2),
                end = Offset(size.width, size.height / 2),
                strokeWidth = size.height,
                cap = StrokeCap.Round
            )

            drawLine(
                color = progressColor,
                start = Offset(0f, size.height / 2),
                end = Offset(size.width * animatedProgress, size.height / 2),
                strokeWidth = size.height * 0.9f,
                cap = StrokeCap.Round
            )

            drawLine(
                color = Color.White,
                start = Offset(size.width * greenPosition, 0f),
                end = Offset(size.width * greenPosition, size.height),
                strokeWidth = 2.dp.toPx()
            )

            drawLine(
                color = Color.White,
                start = Offset(size.width * yellowPosition, 0f),
                end = Offset(size.width * yellowPosition, size.height),
                strokeWidth = 2.dp.toPx()
            )

            drawLine(
                color = Color.White,
                start = Offset(size.width * redPosition, 0f),
                end = Offset(size.width * redPosition, size.height),
                strokeWidth = 2.dp.toPx()
            )

            drawCircle(
                color = Color.White.copy(alpha = 0.4f),
                radius = size.height * 1.2f,
                center = Offset(size.width * animatedProgress, size.height / 2)
            )

            drawCircle(
                color = progressColor,
                radius = size.height,
                center = Offset(size.width * animatedProgress, size.height / 2)
            )
        }

        if (showLabels) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            ) {
                Layout(
                    content = {
                        Icon(
                            painter = painterResource(R.drawable.start_24px),
                            modifier = Modifier.size(12.dp),
                            contentDescription = "start",
                            tint = Color.White
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
                        placeables[0].place(0, 0)
                        placeables[1].place(gX, 0)
                        placeables[2].place(yX, 0)
                        placeables[3].place(rX, 0)
                    }
                }
            }
        }
    }
}
