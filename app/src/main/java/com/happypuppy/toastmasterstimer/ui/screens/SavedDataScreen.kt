package com.happypuppy.toastmasterstimer.ui.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.happypuppy.toastmasterstimer.R
import com.happypuppy.toastmasterstimer.data.database.repository.Speaker
import com.happypuppy.toastmasterstimer.navigation.Screen
import com.happypuppy.toastmasterstimer.ui.components.SpeechTimerBottomBar
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Locale

enum class SortOption(val displayName: String) {
    DATE_NEWEST("Date (Newest first)"),
    DATE_OLDEST("Date (Oldest first)"),
    NAME_ASC("Name (A-Z)"),
    NAME_DESC("Name (Z-A)"),
    TYPE_ASC("Speech Type (A-Z)"),
    TYPE_DESC("Speech Type (Z-A)")
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
    val uiState by viewModel.uiState.collectAsState()
    val speakers = uiState.speakers
    val isLoading = uiState.isLoading
    val error = uiState.error
    val context = LocalContext.current

    var showSortOptions by rememberSaveable { mutableStateOf(false) }
    var currentSortOption by remember { mutableStateOf(SortOption.DATE_NEWEST) }

    val sortedSpeakers = remember(speakers, currentSortOption) {
        sortSpeakers(speakers, currentSortOption)
    }

    var showConfirmDialog by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Show delete confirmations only after the database write actually succeeds
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is SpeakerEvent.Deleted ->
                    snackbarHostState.showSnackbar("Deleted ${event.speakerName}'s speech data")

                SpeakerEvent.AllDeleted ->
                    snackbarHostState.showSnackbar("All speech data cleared")
            }
        }
    }

    // Surface transient errors without hiding existing saved speeches
    LaunchedEffect(error) {
        if (error != null && sortedSpeakers.isNotEmpty()) {
            snackbarHostState.showSnackbar("Error: $error")
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Saved Speech Data") },
                actions = {
                    IconButton(onClick = { showSortOptions = true }) {
                        Icon(
                            painter = painterResource(R.drawable.sort_24px),
                            contentDescription = "Sort Options"
                        )
                    }

                    IconButton(
                        onClick = {
                            if (sortedSpeakers.isNotEmpty()) {
                                val launched = shareSpeakerData(context, sortedSpeakers)
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        if (launched) "Sharing all speech data"
                                        else "No app available to share speech data"
                                    )
                                }
                            }
                        },
                        enabled = sortedSpeakers.isNotEmpty()
                    ) {
                        Icon(
                            Icons.Filled.Share,
                            contentDescription = "Share All Data"
                        )
                    }

                    IconButton(
                        onClick = { showConfirmDialog = true },
                        enabled = sortedSpeakers.isNotEmpty()
                    ) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "Clear All Data"
                        )
                    }
                }
            )
        },
        bottomBar = {
            SpeechTimerBottomBar(
                currentRoute = Screen.SavedDataScreen.route,
                navController = navController
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (error != null && sortedSpeakers.isEmpty()) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Error: $error",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.error
                    )
                    OutlinedButton(onClick = { viewModel.clearError() }) {
                        Text("Dismiss")
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
                    }
                )
            } else {
                SpeechDataList(
                    speakers = sortedSpeakers,
                    onShareClick = { speaker ->
                        val launched = shareSingleSpeaker(context, speaker)
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                if (launched) "Sharing ${speaker.name}'s speech data"
                                else "No app available to share speech data"
                            )
                        }
                    },
                    onDeleteClick = { speaker ->
                        viewModel.deleteSpeaker(speaker)
                    }
                )
            }

            if (showConfirmDialog) {
                AlertDialog(
                    onDismissRequest = { showConfirmDialog = false },
                    title = { Text("Clear All Data") },
                    text = { Text("Are you sure you want to delete all saved speech data? This action cannot be undone.") },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.deleteAllSpeakers()
                                showConfirmDialog = false
                            }
                        ) {
                            Text("Delete All")
                        }
                    },
                    dismissButton = {
                        OutlinedButton(
                            onClick = { showConfirmDialog = false }
                        ) {
                            Text("Cancel")
                        }
                    }
                )
            }

            if (showSortOptions) {
                AlertDialog(
                    onDismissRequest = { showSortOptions = false },
                    title = { Text("Sort By") },
                    text = {
                        Column {
                            SortOption.entries.forEach { option ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            currentSortOption = option
                                            showSortOptions = false
                                        }
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = currentSortOption == option,
                                        onClick = {
                                            currentSortOption = option
                                            showSortOptions = false
                                        }
                                    )
                                    Text(
                                        text = option.displayName,
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showSortOptions = false }) {
                            Text("Cancel")
                        }
                    },
                    dismissButton = {}
                )
            }
        }
    }
}

@Composable
fun SpeechDataList(
    speakers: List<Speaker>,
    onShareClick: (Speaker) -> Unit,
    onDeleteClick: (Speaker) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items = speakers, key = { it.id }) { speaker ->
            SpeakerCard(
                speaker = speaker,
                onShareClick = { onShareClick(speaker) },
                onDeleteClick = { onDeleteClick(speaker) }
            )
        }
    }
}

@Composable
fun SpeakerCard(
    speaker: Speaker,
    onShareClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val locale = LocalLocale.current.platformLocale
    val dateFormat = remember(locale) { SimpleDateFormat("MMM dd, yyyy HH:mm", locale) }
    val formattedDate = remember(speaker.createdAt, dateFormat) { dateFormat.format(speaker.createdAt) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
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
                text = speaker.speechType,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

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
                IconButton(onClick = onShareClick) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = stringResource(R.string.saved_row_share, speaker.name, speaker.speechType),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(onClick = onDeleteClick) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(R.string.saved_row_delete, speaker.name, speaker.speechType),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyStateContent(onAddSpeech: () -> Unit) {
    Column(
        modifier = Modifier
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
            text = "No Saved Speech Data",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Start timing a speech to save data",
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
            Text("Add New Speech")
        }
    }
}

private fun formatSpeakerSummary(speaker: Speaker, dateFormat: SimpleDateFormat): String {
    val formattedDate = dateFormat.format(speaker.createdAt)
    return """
        Name: ${speaker.name}
        Speech Type: ${speaker.speechType}
        Duration: ${speaker.duration}
        Recorded on: $formattedDate
    """.trimIndent()
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
    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    val shareText = buildString {
        appendLine("Simple Speech Timer - Speaker Data")
        appendLine()
        appendLine(formatSpeakerSummary(speaker, dateFormat))
        appendLine()
        append("Created with Simple Speech Timer App")
    }
    return launchShareChooser(context, shareText, "Share Speech Data")
}

fun shareSpeakerData(context: Context, speakers: List<Speaker>): Boolean {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    val shareText = speakers.joinToString(
        prefix = "Simple Speech Timer - Speaker Data\n\n",
        separator = "\n--------------------------\n",
        postfix = "\n\nCreated with Simple Speech Timer App for Android"
    ) { speaker ->
        formatSpeakerSummary(speaker, dateFormat)
    }
    return launchShareChooser(context, shareText, "Share All Speech Data")
}
