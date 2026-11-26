package com.happypuppy.toastmasterstimer.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.happypuppy.toastmasterstimer.R
import kotlinx.coroutines.delay

private const val HOLD_INITIAL_DELAY_MS = 400L
private const val HOLD_REPEAT_INTERVAL_MS = 90L

@Composable
fun MinutePicker(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange,
    increaseDescription: String,
    decreaseDescription: String,
    modifier: Modifier = Modifier
) {
    val currentValue by rememberUpdatedState(value)
    val currentRange by rememberUpdatedState(range)
    val currentOnValueChange by rememberUpdatedState(onValueChange)

    val canIncrease = value < range.last
    val canDecrease = value > range.first

    val increaseInteraction = remember { MutableInteractionSource() }
    val decreaseInteraction = remember { MutableInteractionSource() }
    val isIncreasePressed by increaseInteraction.collectIsPressedAsState()
    val isDecreasePressed by decreaseInteraction.collectIsPressedAsState()
    var didHoldIncrease by remember { mutableStateOf(false) }
    var didHoldDecrease by remember { mutableStateOf(false) }

    LaunchedEffect(isIncreasePressed) {
        if (!isIncreasePressed) return@LaunchedEffect
        didHoldIncrease = false
        delay(HOLD_INITIAL_DELAY_MS)
        while (currentValue < currentRange.last) {
            didHoldIncrease = true
            currentOnValueChange(currentValue + 1)
            delay(HOLD_REPEAT_INTERVAL_MS)
        }
    }

    LaunchedEffect(isDecreasePressed) {
        if (!isDecreasePressed) return@LaunchedEffect
        didHoldDecrease = false
        delay(HOLD_INITIAL_DELAY_MS)
        while (currentValue > currentRange.first) {
            didHoldDecrease = true
            currentOnValueChange(currentValue - 1)
            delay(HOLD_REPEAT_INTERVAL_MS)
        }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Card(
            modifier = Modifier
                .widthIn(min = 120.dp)
                .padding(8.dp),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                IconButton(
                    onClick = {
                        if (didHoldIncrease) {
                            didHoldIncrease = false
                        } else if (value < range.last) {
                            onValueChange(value + 1)
                        }
                    },
                    enabled = canIncrease,
                    interactionSource = increaseInteraction,
                    modifier = Modifier
                        .size(48.dp)
                        .align(Alignment.CenterHorizontally)
                        .semantics { contentDescription = increaseDescription }
                ) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowUp,
                        contentDescription = null
                    )
                }

                Text(
                    text = value.toString(),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                IconButton(
                    onClick = {
                        if (didHoldDecrease) {
                            didHoldDecrease = false
                        } else if (value > range.first) {
                            onValueChange(value - 1)
                        }
                    },
                    enabled = canDecrease,
                    interactionSource = decreaseInteraction,
                    modifier = Modifier
                        .size(48.dp)
                        .align(Alignment.CenterHorizontally)
                        .semantics { contentDescription = decreaseDescription }
                ) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = null
                    )
                }
            }
        }

        Text(
            text = pluralStringResource(R.plurals.custom_speech_minutes_unit, value),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}
