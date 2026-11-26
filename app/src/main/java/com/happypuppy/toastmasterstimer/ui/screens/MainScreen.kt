package com.happypuppy.toastmasterstimer.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.happypuppy.toastmasterstimer.R
import com.happypuppy.toastmasterstimer.data.database.repository.Speaker
import com.happypuppy.toastmasterstimer.data.preferences.AppTheme
import com.happypuppy.toastmasterstimer.data.preferences.DataStoreManager
import com.happypuppy.toastmasterstimer.model.SpeechType
import com.happypuppy.toastmasterstimer.navigation.Screen
import com.happypuppy.toastmasterstimer.navigation.TimerRoute
import com.happypuppy.toastmasterstimer.ui.components.CueTimingStrip
import com.happypuppy.toastmasterstimer.ui.components.PresetCard
import com.happypuppy.toastmasterstimer.ui.components.presetCueTimes
import com.happypuppy.toastmasterstimer.ui.components.speakerSpeechTypeDisplayName
import com.happypuppy.toastmasterstimer.ui.layout.ADAPTIVE_CONTENT_MAX_WIDTH
import com.happypuppy.toastmasterstimer.ui.layout.rememberAdaptiveLayoutSpec
import com.happypuppy.toastmasterstimer.ui.theme.LocalPhaseColors
import com.happypuppy.toastmasterstimer.ui.theme.SimpleSpeechTimerTheme
import com.happypuppy.toastmasterstimer.util.CustomRange
import com.happypuppy.toastmasterstimer.util.TimerPhase
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import java.util.Date

internal const val MAX_RECENT_SPEAKERS = 3
internal const val MAIN_CONTENT_COLUMN_TAG = "main_content_column"

/** Returns up to [limit] most recent speakers ordered newest-first. */
fun recentSpeakers(speakers: List<Speaker>, limit: Int = MAX_RECENT_SPEAKERS): List<Speaker> =
    speakers
        .sortedByDescending { it.createdAt }
        .take(limit.coerceAtLeast(0))

@Composable
fun MainScreen(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    viewModel: SpeechTimerViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val dataStoreManager = koinInject<DataStoreManager>()
    val lastCustomRange by dataStoreManager.lastCustomRangeFlow.collectAsStateWithLifecycle(initialValue = null)
    val layoutSpec = rememberAdaptiveLayoutSpec()

    MainScreenContent(
        speakers = uiState.speakers,
        useTwoColumnPresets = layoutSpec.useTwoColumnPresets,
        onPresetClick = { type ->
            val minMinutes = requireNotNull(type.presetMinMinutes) { "${type.id} has no preset range" }
            val maxMinutes = requireNotNull(type.presetMaxMinutes) { "${type.id} has no preset range" }
            navController.navigate(TimerRoute(type.id, minMinutes, maxMinutes))
        },
        onOpenSavedData = {
            navController.navigate(Screen.SavedDataScreen.route) {
                popUpTo(Screen.Main.route) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        },
        onOpenSettings = { navController.navigate(Screen.Settings.route) },
        modifier = modifier,
        lastCustomRange = lastCustomRange,
        onLastCustomClick = { range ->
            navController.navigate(TimerRoute(SpeechType.CUSTOM.id, range.minMinutes, range.maxMinutes))
        },
        onEditLastCustomClick = {
            navController.navigate(Screen.CustomSpeechScreen.route) {
                popUpTo(Screen.Main.route) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenContent(
    speakers: List<Speaker>,
    useTwoColumnPresets: Boolean,
    onPresetClick: (SpeechType) -> Unit,
    onOpenSavedData: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    lastCustomRange: CustomRange? = null,
    onLastCustomClick: (CustomRange) -> Unit = {},
    onEditLastCustomClick: () -> Unit = {}
) {
    val recent = remember(speakers) { recentSpeakers(speakers) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.app_name)) },
                navigationIcon = {
                    Icon(
                        painter = painterResource(R.drawable.hourglass_top_24px),
                        contentDescription = null,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.nav_settings)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = ADAPTIVE_CONTENT_MAX_WIDTH)
                    .fillMaxWidth()
                    .testTag(MAIN_CONTENT_COLUMN_TAG)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                if (useTwoColumnPresets) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SpeechType.presets.chunked(2).forEach { rowPresets ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                rowPresets.forEach { type ->
                                    PresetCard(
                                        speechType = type,
                                        onClick = { onPresetClick(type) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (rowPresets.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SpeechType.presets.forEach { type ->
                            PresetCard(
                                speechType = type,
                                onClick = { onPresetClick(type) }
                            )
                        }
                    }
                }

                if (lastCustomRange != null) {
                    LastCustomCard(
                        range = lastCustomRange,
                        onLaunchClick = { onLastCustomClick(lastCustomRange) },
                        onEditClick = onEditLastCustomClick
                    )
                }

                if (recent.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.main_recent_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            TextButton(onClick = onOpenSavedData) {
                                Text(stringResource(R.string.main_view_all))
                            }
                        }

                        recent.forEach { speaker ->
                            RecentSpeechCard(
                                speaker = speaker,
                                onClick = onOpenSavedData
                            )
                        }
                    }
                } else {
                    TimingColorGuideCard()
                }
            }
        }
    }
}

@Composable
private fun LastCustomCard(
    range: CustomRange,
    onLaunchClick: () -> Unit,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cueTimes = remember(range.minMinutes, range.maxMinutes) {
        presetCueTimes(range.minMinutes, range.maxMinutes)
    }
    val customTitle = stringResource(SpeechType.CUSTOM.labelRes)
    val rangeLabel = stringResource(R.string.preset_duration_range, range.minMinutes, range.maxMinutes)
    val cardDescription = stringResource(
        R.string.main_last_custom_card_cd,
        customTitle,
        rangeLabel,
        cueTimes.greenText,
        cueTimes.yellowText,
        cueTimes.redText
    )
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 84.dp),
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
        }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 84.dp)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = LocalIndication.current,
                        onClick = onLaunchClick
                    )
                    .semantics { contentDescription = cardDescription }
                    .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = stringResource(R.string.main_last_custom_label),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = customTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    CueTimingStrip(cueTimes = cueTimes)
                }

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
            }

            IconButton(
                onClick = onEditClick,
                modifier = Modifier
                    .padding(end = 4.dp)
                    .size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = stringResource(R.string.main_last_custom_edit_cd),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun RecentSpeechCard(
    speaker: Speaker,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val displaySpeechType = speakerSpeechTypeDisplayName(speaker)
    val phase = remember(speaker.phaseReached) { parseTimerPhase(speaker.phaseReached) }
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        border = if (isFocused) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        },
        interactionSource = interactionSource
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = speaker.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = displaySpeechType,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (phase != null) {
                    val cue = phaseCue(phase)
                    val beforeGreen = LocalPhaseColors.current.beforeGreen
                    val phaseColor = when (phase) {
                        TimerPhase.BEFORE_GREEN -> MaterialTheme.colorScheme.onSurfaceVariant
                        TimerPhase.GREEN -> beforeGreen.markerGreen
                        TimerPhase.YELLOW -> beforeGreen.markerYellow
                        TimerPhase.RED -> beforeGreen.markerRed
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        PhaseIndicatorIcon(icon = cue.icon, tint = phaseColor)
                        Text(
                            text = stringResource(cue.labelRes),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Text(
                text = speaker.duration,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 12.dp)
            )
        }
    }
}

@Composable
private fun TimingColorGuideCard(modifier: Modifier = Modifier) {
    val beforeGreen = LocalPhaseColors.current.beforeGreen

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = stringResource(R.string.main_timing_guide_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            TimingGuideRow(
                icon = PhaseIcon.CHECK,
                tint = beforeGreen.markerGreen,
                text = stringResource(R.string.main_timing_guide_green)
            )
            TimingGuideRow(
                icon = PhaseIcon.WARNING,
                tint = beforeGreen.markerYellow,
                text = stringResource(R.string.main_timing_guide_yellow)
            )
            TimingGuideRow(
                icon = PhaseIcon.STOP,
                tint = beforeGreen.markerRed,
                text = stringResource(R.string.main_timing_guide_red)
            )
        }
    }
}

@Composable
private fun TimingGuideRow(
    icon: PhaseIcon,
    tint: Color,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        PhaseIndicatorIcon(icon = icon, tint = tint)
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
internal fun PhaseIndicatorIcon(
    icon: PhaseIcon,
    tint: Color
) {
    when (icon) {
        PhaseIcon.HOURGLASS -> Icon(
            painter = painterResource(R.drawable.hourglass_top_24px),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(16.dp)
        )
        PhaseIcon.CHECK -> Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(16.dp)
        )
        PhaseIcon.WARNING -> Icon(
            imageVector = Icons.Filled.Warning,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(16.dp)
        )
        PhaseIcon.STOP -> Icon(
            painter = painterResource(R.drawable.stop_24px),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MainScreenEmptyLightPreview() {
    SimpleSpeechTimerTheme(themePreference = AppTheme.LIGHT, dynamicColor = false) {
        MainScreenContent(
            speakers = emptyList(),
            useTwoColumnPresets = false,
            onPresetClick = {},
            onOpenSavedData = {},
            onOpenSettings = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MainScreenRecentDarkPreview() {
    SimpleSpeechTimerTheme(themePreference = AppTheme.DARK, dynamicColor = false) {
        MainScreenContent(
            speakers = listOf(
                Speaker(
                    id = 1,
                    name = "Alex Morgan",
                    speechType = "TABLE TOPICS",
                    duration = "01:42",
                    createdAt = Date(2_000L),
                    speechTypeId = SpeechType.TABLE_TOPICS.id,
                    phaseReached = TimerPhase.YELLOW.name
                ),
                Speaker(
                    id = 2,
                    name = "Sam Lee",
                    speechType = "SPEECH",
                    duration = "06:15",
                    createdAt = Date(1_000L),
                    speechTypeId = SpeechType.SPEECH.id,
                    phaseReached = TimerPhase.GREEN.name
                )
            ),
            useTwoColumnPresets = false,
            onPresetClick = {},
            onOpenSavedData = {},
            onOpenSettings = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MainScreenLastCustomPreview() {
    SimpleSpeechTimerTheme(themePreference = AppTheme.LIGHT, dynamicColor = false) {
        MainScreenContent(
            speakers = emptyList(),
            useTwoColumnPresets = false,
            onPresetClick = {},
            onOpenSavedData = {},
            onOpenSettings = {},
            lastCustomRange = CustomRange(3, 5)
        )
    }
}

@Preview(showBackground = true, widthDp = 900, heightDp = 700)
@Composable
private fun MainScreenExpandedGridPreview() {
    SimpleSpeechTimerTheme(themePreference = AppTheme.LIGHT, dynamicColor = false) {
        MainScreenContent(
            speakers = listOf(
                Speaker(
                    id = 1,
                    name = "Alex Morgan",
                    speechType = "TABLE TOPICS",
                    duration = "01:42",
                    createdAt = Date(2_000L),
                    speechTypeId = SpeechType.TABLE_TOPICS.id,
                    phaseReached = TimerPhase.GREEN.name
                )
            ),
            useTwoColumnPresets = true,
            onPresetClick = {},
            onOpenSavedData = {},
            onOpenSettings = {}
        )
    }
}
