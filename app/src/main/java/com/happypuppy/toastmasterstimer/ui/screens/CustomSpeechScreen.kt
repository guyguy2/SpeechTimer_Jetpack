package com.happypuppy.toastmasterstimer.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberRangeSliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.happypuppy.toastmasterstimer.R
import com.happypuppy.toastmasterstimer.data.preferences.AppTheme
import com.happypuppy.toastmasterstimer.data.preferences.DataStoreManager
import com.happypuppy.toastmasterstimer.model.SpeechType
import com.happypuppy.toastmasterstimer.navigation.TimerRoute
import com.happypuppy.toastmasterstimer.ui.components.ExpressiveToggleGroup
import com.happypuppy.toastmasterstimer.ui.components.ExpressiveToggleOption
import com.happypuppy.toastmasterstimer.ui.components.MinutePicker
import com.happypuppy.toastmasterstimer.ui.components.presetCueTimes
import com.happypuppy.toastmasterstimer.ui.layout.ADAPTIVE_COMPACT_FORM_MAX_WIDTH
import com.happypuppy.toastmasterstimer.ui.theme.LocalPhaseColors
import com.happypuppy.toastmasterstimer.ui.theme.SimpleSpeechTimerTheme
import com.happypuppy.toastmasterstimer.util.CustomRange
import com.happypuppy.toastmasterstimer.util.CustomSpeechRange
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import kotlin.math.roundToInt

internal const val CUSTOM_RANGE_SLIDER_TAG = "custom_range_slider"

internal val CUSTOM_QUICK_PRESETS = listOf(
    CustomRange(3, 5),
    CustomRange(8, 10),
    CustomRange(12, 15),
    CustomRange(20, 30)
)

@Composable
fun SpeechTimePicker(navController: NavHostController) {
    val dataStoreManager = koinInject<DataStoreManager>()
    val coroutineScope = rememberCoroutineScope()

    var startMinutes by rememberSaveable { mutableIntStateOf(CustomSpeechRange.DEFAULT_MIN_MINUTES) }
    var endMinutes by rememberSaveable { mutableIntStateOf(CustomSpeechRange.DEFAULT_MAX_MINUTES) }
    var hasHydratedInitialRange by rememberSaveable { mutableStateOf(false) }
    var hasUserEdited by rememberSaveable { mutableStateOf(false) }
    var isStarting by remember { mutableStateOf(false) }

    LaunchedEffect(dataStoreManager) {
        if (!hasHydratedInitialRange && !hasUserEdited) {
            val savedRange = dataStoreManager.lastCustomRangeFlow.first()
            if (savedRange != null && !hasUserEdited) {
                startMinutes = savedRange.minMinutes
                endMinutes = savedRange.maxMinutes
            }
            hasHydratedInitialRange = true
        }
    }

    CustomSpeechScreenContent(
        startMinutes = startMinutes,
        endMinutes = endMinutes,
        onStartMinutesChange = { newStart ->
            val (clampedStart, clampedEnd) = CustomSpeechRange.updateStart(
                newStart = newStart,
                currentEnd = endMinutes
            )
            startMinutes = clampedStart
            endMinutes = clampedEnd
            hasUserEdited = true
        },
        onEndMinutesChange = { newEnd ->
            val (clampedStart, clampedEnd) = CustomSpeechRange.updateEnd(
                currentStart = startMinutes,
                newEnd = newEnd
            )
            startMinutes = clampedStart
            endMinutes = clampedEnd
            hasUserEdited = true
        },
        onRangeChange = { newStart, newEnd ->
            val coerced = CustomSpeechRange.coerceRange(newStart, newEnd)
            startMinutes = coerced.minMinutes
            endMinutes = coerced.maxMinutes
            hasUserEdited = true
        },
        onStartTimer = {
            if (!isStarting) {
                isStarting = true
                coroutineScope.launch {
                    try {
                        dataStoreManager.setLastCustomRange(startMinutes, endMinutes)
                        navController.navigate(TimerRoute(SpeechType.CUSTOM.id, startMinutes, endMinutes))
                    } finally {
                        isStarting = false
                    }
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CustomSpeechScreenContent(
    startMinutes: Int,
    endMinutes: Int,
    onStartMinutesChange: (Int) -> Unit,
    onEndMinutesChange: (Int) -> Unit,
    onRangeChange: (Int, Int) -> Unit,
    onStartTimer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val quickPresetOptions = CUSTOM_QUICK_PRESETS.map { preset ->
        ExpressiveToggleOption(
            value = preset,
            label = stringResource(R.string.preset_duration_range, preset.minMinutes, preset.maxMinutes)
        )
    }
    val selectedQuickPreset = CUSTOM_QUICK_PRESETS.firstOrNull {
        it.minMinutes == startMinutes && it.maxMinutes == endMinutes
    }

    val rangeSliderState = rememberRangeSliderState(
        startValue = startMinutes.toFloat(),
        endValue = endMinutes.toFloat(),
        steps = CustomSpeechRange.MAX_END_MINUTES - CustomSpeechRange.MIN_START_MINUTES - 1,
        trackRange = CustomSpeechRange.MIN_START_MINUTES.toFloat()..CustomSpeechRange.MAX_END_MINUTES.toFloat()
    )
    LaunchedEffect(startMinutes, endMinutes) {
        val targetStart = startMinutes.toFloat()
        val targetEnd = endMinutes.toFloat()
        if (rangeSliderState.startValue != targetStart) {
            rangeSliderState.startValue = targetStart
        }
        if (rangeSliderState.endValue != targetEnd) {
            rangeSliderState.endValue = targetEnd
        }
    }

    val startThumbInteractionSource = remember { MutableInteractionSource() }
    val endThumbInteractionSource = remember { MutableInteractionSource() }
    val sliderColors = SliderDefaults.colors()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.custom_speech_title)) }
            )
        }
    ) { paddingValues ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = ADAPTIVE_COMPACT_FORM_MAX_WIDTH)
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(top = 16.dp, bottom = 8.dp)
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = stringResource(R.string.custom_speech_quick_presets),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold
                            )
                            ExpressiveToggleGroup(
                                options = quickPresetOptions,
                                selectedValue = selectedQuickPreset,
                                onSelect = { preset ->
                                    onRangeChange(preset.minMinutes, preset.maxMinutes)
                                }
                            )
                        }

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            itemVerticalAlignment = Alignment.CenterVertically
                        ) {
                            MinutePicker(
                                label = stringResource(R.string.custom_speech_from),
                                value = startMinutes,
                                onValueChange = onStartMinutesChange,
                                range = CustomSpeechRange.startRange,
                                increaseDescription = stringResource(R.string.stepper_increase_minimum),
                                decreaseDescription = stringResource(R.string.stepper_decrease_minimum)
                            )

                            Text(
                                text = stringResource(R.string.custom_speech_to_connector),
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )

                            MinutePicker(
                                label = stringResource(R.string.custom_speech_to),
                                value = endMinutes,
                                onValueChange = onEndMinutesChange,
                                range = CustomSpeechRange.endRange(startMinutes),
                                increaseDescription = stringResource(R.string.stepper_increase_maximum),
                                decreaseDescription = stringResource(R.string.stepper_decrease_maximum)
                            )
                        }

                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.custom_speech_range_slider_label),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = stringResource(
                                        R.string.preset_duration_range,
                                        startMinutes,
                                        endMinutes
                                    ),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            RangeSlider(
                                state = rangeSliderState,
                                onValueChange = { range ->
                                    val rawStart = range.start.roundToInt()
                                    val rawEnd = range.endInclusive.roundToInt()
                                    val (clampedStart, clampedEnd) = if (rawStart != startMinutes) {
                                        CustomSpeechRange.updateStart(rawStart, rawEnd)
                                    } else {
                                        CustomSpeechRange.updateEnd(rawStart, rawEnd)
                                    }
                                    rangeSliderState.startValue = clampedStart.toFloat()
                                    rangeSliderState.endValue = clampedEnd.toFloat()
                                    onRangeChange(clampedStart, clampedEnd)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag(CUSTOM_RANGE_SLIDER_TAG),
                                colors = sliderColors,
                                startThumbInteractionSource = startThumbInteractionSource,
                                endThumbInteractionSource = endThumbInteractionSource,
                                startThumb = {
                                    Box(
                                        modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        SliderDefaults.Thumb(
                                            interactionSource = startThumbInteractionSource,
                                            colors = sliderColors
                                        )
                                    }
                                },
                                endThumb = {
                                    Box(
                                        modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        SliderDefaults.Thumb(
                                            interactionSource = endThumbInteractionSource,
                                            colors = sliderColors
                                        )
                                    }
                                },
                                track = { state ->
                                    SliderDefaults.Track(
                                        rangeSliderState = state,
                                        colors = sliderColors,
                                        drawTick = { _, _ -> }
                                    )
                                }
                            )
                        }

                        Text(
                            text = pluralStringResource(
                                R.plurals.custom_speech_summary,
                                endMinutes,
                                startMinutes,
                                endMinutes
                            ),
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )

                        CustomThresholdsPreviewCard(
                            startMinutes = startMinutes,
                            endMinutes = endMinutes
                        )
                    }

                    Button(
                        onClick = onStartTimer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 16.dp)
                            .heightIn(min = 56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.custom_speech_start_timer),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CustomThresholdsPreviewCard(
    startMinutes: Int,
    endMinutes: Int,
    modifier: Modifier = Modifier
) {
    val cueTimes = remember(startMinutes, endMinutes) {
        presetCueTimes(startMinutes, endMinutes)
    }
    val beforeGreen = LocalPhaseColors.current.beforeGreen
    val previewDescription = stringResource(
        R.string.custom_speech_preview_cd,
        cueTimes.greenText,
        cueTimes.yellowText,
        cueTimes.redText
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { contentDescription = previewDescription },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = stringResource(R.string.custom_speech_preview_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ThresholdPreviewItem(
                    icon = PhaseIcon.CHECK,
                    tint = beforeGreen.markerGreen,
                    text = stringResource(R.string.custom_speech_preview_green, cueTimes.greenText)
                )
                ThresholdPreviewItem(
                    icon = PhaseIcon.WARNING,
                    tint = beforeGreen.markerYellow,
                    text = stringResource(R.string.custom_speech_preview_yellow, cueTimes.yellowText)
                )
                ThresholdPreviewItem(
                    icon = PhaseIcon.STOP,
                    tint = beforeGreen.markerRed,
                    text = stringResource(R.string.custom_speech_preview_red, cueTimes.redText)
                )
            }
        }
    }
}

@Composable
private fun ThresholdPreviewItem(
    icon: PhaseIcon,
    tint: Color,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        PhaseIndicatorIcon(icon = icon, tint = tint)
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CustomSpeechScreenLightPreview() {
    SimpleSpeechTimerTheme(themePreference = AppTheme.LIGHT, dynamicColor = false) {
        CustomSpeechScreenContent(
            startMinutes = 8,
            endMinutes = 10,
            onStartMinutesChange = {},
            onEndMinutesChange = {},
            onRangeChange = { _, _ -> },
            onStartTimer = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CustomSpeechScreenDarkPreview() {
    SimpleSpeechTimerTheme(themePreference = AppTheme.DARK, dynamicColor = false) {
        CustomSpeechScreenContent(
            startMinutes = 20,
            endMinutes = 30,
            onStartMinutesChange = {},
            onEndMinutesChange = {},
            onRangeChange = { _, _ -> },
            onStartTimer = {}
        )
    }
}
