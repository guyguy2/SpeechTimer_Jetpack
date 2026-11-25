package com.happypuppy.toastmasterstimer.ui.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.happypuppy.toastmasterstimer.R
import com.happypuppy.toastmasterstimer.data.database.repository.Speaker
import com.happypuppy.toastmasterstimer.data.preferences.DataStoreManager
import com.happypuppy.toastmasterstimer.navigation.Screen
import com.happypuppy.toastmasterstimer.ui.components.InputDialog
import com.happypuppy.toastmasterstimer.ui.components.SwipeToDeleteSpeakerItem
import com.happypuppy.toastmasterstimer.ui.components.speakerSpeechTypeDisplayName
import com.happypuppy.toastmasterstimer.ui.layout.ADAPTIVE_CONTENT_MAX_WIDTH
import com.happypuppy.toastmasterstimer.ui.layout.ADAPTIVE_GRID_MIN_CELL_WIDTH
import com.happypuppy.toastmasterstimer.ui.layout.rememberAdaptiveLayoutSpec
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import java.text.DateFormat
import java.util.Locale

enum class SortOption(@StringRes val labelRes: Int) {
    DATE_NEWEST(R.string.saved_sort_date_newest),
    DATE_OLDEST(R.string.saved_sort_date_oldest),
    NAME_ASC(R.string.saved_sort_name_asc),
    NAME_DESC(R.string.saved_sort_name_desc),
    TYPE_ASC(R.string.saved_sort_type_asc),
    TYPE_DESC(R.string.saved_sort_type_desc);

    companion object {
        fun fromName(raw: String?): SortOption = entries.firstOrNull { it.name == raw } ?: DATE_NEWEST
    }
}

fun sortSpeakers(speakers: List<Speaker>, option: SortOption): List<Speaker> = when (option) {
    SortOption.DATE_NEWEST -> speakers.sortedByDescending { it.createdAt }
    SortOption.DATE_OLDEST -> speakers.sortedBy { it.createdAt }
    SortOption.NAME_ASC -> speakers.sortedBy { it.name.lowercase(Locale.ROOT) }
    SortOption.NAME_DESC -> speakers.sortedByDescending { it.name.lowercase(Locale.ROOT) }
    SortOption.TYPE_ASC -> speakers.sortedBy { it.speechType.lowercase(Locale.ROOT) }
    SortOption.TYPE_DESC -> speakers.sortedByDescending { it.speechType.lowercase(Locale.ROOT) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedDataScreen(
    navController: NavHostController,
    viewModel: SpeechTimerViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val speakers = uiState.speakers
    val isLoading = uiState.isLoading
    val error = uiState.error
    val context = LocalContext.current
    val resources = LocalResources.current
    val dataStoreManager = koinInject<DataStoreManager>()
    val isVibrationEnabled by dataStoreManager.vibrateEnabledFlow.collectAsStateWithLifecycle(initialValue = true)
    val layoutSpec = rememberAdaptiveLayoutSpec()

    var showSortOptions by rememberSaveable { mutableStateOf(false) }
    var currentSortOption by rememberSaveable { mutableStateOf(SortOption.DATE_NEWEST) }
    LaunchedEffect(dataStoreManager) {
        dataStoreManager.savedSortOptionFlow.collect { stored ->
            currentSortOption = stored
        }
    }

    val sortedSpeakers = remember(speakers, currentSortOption) {
        sortSpeakers(speakers, currentSortOption)
    }

    var editingSpeakerId by rememberSaveable { mutableStateOf<Int?>(null) }
    var showConfirmDialog by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Show delete confirmations with Undo. collectLatest cancels any superseded snackbar via
    // CancellationException so SnackbarResult.Dismissed only clears the multi-undo stack on real
    // timeout or user dismissal.
    LaunchedEffect(viewModel, snackbarHostState, resources) {
        viewModel.events.collectLatest { event ->
            snackbarHostState.currentSnackbarData?.dismiss()
            val message = when (event) {
                is SpeakerEvent.Deleted ->
                    resources.getString(R.string.saved_deleted_snackbar, event.speakerName)
                SpeakerEvent.AllDeleted ->
                    resources.getString(R.string.saved_all_deleted_snackbar)
            }
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = resources.getString(R.string.saved_undo),
                duration = SnackbarDuration.Long
            )
            when (result) {
                SnackbarResult.ActionPerformed -> viewModel.undoDelete()
                SnackbarResult.Dismissed -> viewModel.dismissUndoSnackbar()
            }
        }
    }

    // Surface transient errors without hiding existing saved speeches
    LaunchedEffect(error) {
        if (error != null && sortedSpeakers.isNotEmpty()) {
            snackbarHostState.showSnackbar(
                resources.getString(R.string.saved_error_prefix, error.message.asString(context))
            )
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.saved_title)) },
                actions = {
                    IconButton(onClick = { showSortOptions = true }) {
                        Icon(
                            painter = painterResource(R.drawable.sort_24px),
                            contentDescription = stringResource(R.string.saved_sort_options_cd)
                        )
                    }

                    IconButton(
                        onClick = {
                            if (sortedSpeakers.isNotEmpty()) {
                                val launched = shareSpeakerData(context, sortedSpeakers)
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        if (launched) resources.getString(R.string.saved_sharing_all)
                                        else resources.getString(R.string.saved_share_no_app)
                                    )
                                }
                            }
                        },
                        enabled = sortedSpeakers.isNotEmpty()
                    ) {
                        Icon(
                            Icons.Filled.Share,
                            contentDescription = stringResource(R.string.saved_share_all_cd)
                        )
                    }

                    IconButton(
                        onClick = { showConfirmDialog = true },
                        enabled = sortedSpeakers.isNotEmpty()
                    ) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = stringResource(R.string.saved_clear_all_cd)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.TopCenter
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (error != null && sortedSpeakers.isEmpty()) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .widthIn(max = ADAPTIVE_CONTENT_MAX_WIDTH)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.saved_error_prefix, error.message.asString()),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.error
                    )
                    OutlinedButton(onClick = { viewModel.clearError() }) {
                        Text(stringResource(R.string.common_dismiss))
                    }
                }
            } else if (sortedSpeakers.isEmpty()) {
                EmptyStateContent(
                    onAddSpeech = {
                        navController.navigate(Screen.Main.route) {
                            popUpTo(Screen.Main.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                SpeechDataList(
                    speakers = sortedSpeakers,
                    onEditClick = { speaker ->
                        editingSpeakerId = speaker.id
                    },
                    onShareClick = { speaker ->
                        val launched = shareSingleSpeaker(context, speaker)
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                if (launched) resources.getString(R.string.saved_sharing_single, speaker.name)
                                else resources.getString(R.string.saved_share_no_app)
                            )
                        }
                    },
                    onDeleteClick = { speaker ->
                        viewModel.deleteSpeaker(speaker)
                    },
                    restoreVersions = uiState.restoreVersions,
                    useGrid = layoutSpec.useSavedDataGrid,
                    enableSwipeToDelete = layoutSpec.enableSwipeToDelete,
                    isVibrationEnabled = isVibrationEnabled
                )
            }

            val editingSpeaker = editingSpeakerId?.let { id -> speakers.firstOrNull { it.id == id } }
            if (editingSpeaker != null) {
                InputDialog(
                    onDismiss = { editingSpeakerId = null },
                    onConfirm = { newName ->
                        viewModel.updateSpeakerName(editingSpeaker, newName)
                        editingSpeakerId = null
                    },
                    title = stringResource(R.string.saved_edit_dialog_title),
                    initialText = editingSpeaker.name
                )
            }

            if (showConfirmDialog) {
                AlertDialog(
                    onDismissRequest = { showConfirmDialog = false },
                    title = { Text(stringResource(R.string.saved_clear_all_title)) },
                    text = { Text(stringResource(R.string.saved_clear_all_message)) },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.deleteAllSpeakers()
                                showConfirmDialog = false
                            }
                        ) {
                            Text(stringResource(R.string.saved_clear_all_confirm))
                        }
                    },
                    dismissButton = {
                        OutlinedButton(
                            onClick = { showConfirmDialog = false }
                        ) {
                            Text(stringResource(R.string.common_cancel))
                        }
                    }
                )
            }

            if (showSortOptions) {
                AlertDialog(
                    onDismissRequest = { showSortOptions = false },
                    title = { Text(stringResource(R.string.saved_sort_dialog_title)) },
                    text = {
                        Column {
                            SortOption.entries.forEach { option ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            currentSortOption = option
                                            showSortOptions = false
                                            scope.launch { dataStoreManager.setSavedSortOption(option) }
                                        }
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = currentSortOption == option,
                                        onClick = {
                                            currentSortOption = option
                                            showSortOptions = false
                                            scope.launch { dataStoreManager.setSavedSortOption(option) }
                                        }
                                    )
                                    Text(
                                        text = stringResource(option.labelRes),
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showSortOptions = false }) {
                            Text(stringResource(R.string.common_cancel))
                        }
                    },
                    dismissButton = {}
                )
            }
        }
    }
}

internal const val SAVED_LIST_TAG = "saved_data_list"
internal const val SAVED_GRID_TAG = "saved_data_grid"

@Composable
fun SpeechDataList(
    speakers: List<Speaker>,
    onEditClick: (Speaker) -> Unit,
    onShareClick: (Speaker) -> Unit,
    onDeleteClick: (Speaker) -> Unit,
    modifier: Modifier = Modifier,
    restoreVersions: Map<Int, Int> = emptyMap(),
    useGrid: Boolean = false,
    enableSwipeToDelete: Boolean = true,
    isVibrationEnabled: Boolean = true
) {
    if (useGrid) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = ADAPTIVE_GRID_MIN_CELL_WIDTH),
            modifier = modifier
                .widthIn(max = ADAPTIVE_CONTENT_MAX_WIDTH)
                .fillMaxSize()
                .testTag(SAVED_GRID_TAG),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            gridItems(
                items = speakers,
                key = { speaker -> "${speaker.id}_${restoreVersions[speaker.id] ?: 0}" }
            ) { speaker ->
                SpeakerCard(
                    speaker = speaker,
                    onEditClick = { onEditClick(speaker) },
                    onShareClick = { onShareClick(speaker) },
                    onDeleteClick = { onDeleteClick(speaker) }
                )
            }
        }
    } else {
        LazyColumn(
            modifier = modifier
                .widthIn(max = ADAPTIVE_CONTENT_MAX_WIDTH)
                .fillMaxSize()
                .testTag(SAVED_LIST_TAG),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(
                items = speakers,
                key = { speaker -> "${speaker.id}_${restoreVersions[speaker.id] ?: 0}" }
            ) { speaker ->
                if (enableSwipeToDelete) {
                    SwipeToDeleteSpeakerItem(
                        speaker = speaker,
                        isVibrationEnabled = isVibrationEnabled,
                        onEditClick = { onEditClick(speaker) },
                        onShareClick = { onShareClick(speaker) },
                        onDelete = { onDeleteClick(speaker) }
                    )
                } else {
                    SpeakerCard(
                        speaker = speaker,
                        onEditClick = { onEditClick(speaker) },
                        onShareClick = { onShareClick(speaker) },
                        onDeleteClick = { onDeleteClick(speaker) }
                    )
                }
            }
        }
    }
}

@Composable
fun SpeakerCard(
    speaker: Speaker,
    onEditClick: () -> Unit,
    onShareClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val locale = LocalLocale.current.platformLocale
    val dateFormat = remember(locale) {
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, locale)
    }
    val formattedDate = remember(speaker.createdAt, dateFormat) { dateFormat.format(speaker.createdAt) }
    val targetRange = remember(speaker.minSeconds, speaker.maxSeconds) {
        formatTargetRangeMinutes(speaker.minSeconds, speaker.maxSeconds)
    }
    val phase = remember(speaker.phaseReached) { parseTimerPhase(speaker.phaseReached) }
    val displaySpeechType = speakerSpeechTypeDisplayName(speaker)

    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val editLabel = stringResource(R.string.common_edit)
    val deleteLabel = stringResource(R.string.common_delete)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown &&
                    keyEvent.nativeKeyEvent.repeatCount == 0 &&
                    (keyEvent.key == Key.Delete ||
                        keyEvent.key == Key.Backspace ||
                        keyEvent.key == Key.NumPadDelete)
                ) {
                    onDeleteClick()
                    true
                } else {
                    false
                }
            }
            .focusable(interactionSource = interactionSource)
            .semantics {
                customActions = listOf(
                    CustomAccessibilityAction(editLabel) {
                        onEditClick()
                        true
                    },
                    CustomAccessibilityAction(deleteLabel) {
                        onDeleteClick()
                        true
                    }
                )
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = if (isFocused) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            null
        }
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = speaker.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = speaker.duration,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = displaySpeechType,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (targetRange != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.saved_target_range_minutes, targetRange),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (phase != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(phaseCue(phase).labelRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = formattedDate,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onEditClick) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = stringResource(R.string.saved_row_edit, speaker.name, displaySpeechType),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(onClick = onShareClick) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = stringResource(R.string.saved_row_share, speaker.name, displaySpeechType),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(onClick = onDeleteClick) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(R.string.saved_row_delete, speaker.name, displaySpeechType),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyStateContent(
    onAddSpeech: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .widthIn(max = ADAPTIVE_CONTENT_MAX_WIDTH)
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.podium_24px),
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.saved_empty_title),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.saved_empty_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onAddSpeech,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.timer_24px),
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.saved_empty_add_cta))
        }
    }
}

private fun launchShareChooser(context: Context, text: String, chooserTitle: String): Boolean {
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        putExtra(Intent.EXTRA_TEXT, text)
        type = "text/plain"
    }
    return try {
        context.startActivity(Intent.createChooser(sendIntent, chooserTitle))
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
}

fun shareSingleSpeaker(context: Context, speaker: Speaker): Boolean {
    val dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.getDefault())
    val labels = SpeakerShareLabels.fromResources(context.resources)
    val shareText = formatSingleSpeakerShareText(speaker, dateFormat.format(speaker.createdAt), labels)
    return launchShareChooser(context, shareText, context.getString(R.string.share_chooser_single))
}

fun shareSpeakerData(context: Context, speakers: List<Speaker>): Boolean {
    val dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.getDefault())
    val labels = SpeakerShareLabels.fromResources(context.resources)
    val shareText = formatAllSpeakersShareText(speakers, dateFormat::format, labels)
    return launchShareChooser(context, shareText, context.getString(R.string.share_chooser_all))
}
