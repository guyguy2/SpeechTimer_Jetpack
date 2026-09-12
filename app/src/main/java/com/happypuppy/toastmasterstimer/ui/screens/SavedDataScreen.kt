package com.happypuppy.toastmasterstimer.ui.screens

import android.R.id.shareText
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.happypuppy.toastmasterstimer.R
import com.happypuppy.toastmasterstimer.data.database.repository.Speaker
import com.happypuppy.toastmasterstimer.navigation.Screen
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlin.text.format

// Define sorting options
enum class SortOption(val displayName: String) {
    DATE_NEWEST("Date (Newest first)"),
    DATE_OLDEST("Date (Oldest first)"),
    NAME_ASC("Name (A-Z)"),
    NAME_DESC("Name (Z-A)"),
    TYPE_ASC("Speech Type (A-Z)"),
    TYPE_DESC("Speech Type (Z-A)")
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

    // State for sort options
    var showSortOptions by remember { mutableStateOf(false) }
    var currentSortOption by remember { mutableStateOf(SortOption.DATE_NEWEST) }

    // Apply sorting to the speaker list
    val sortedSpeakers = remember(speakers, currentSortOption) {
        when (currentSortOption) {
            SortOption.DATE_NEWEST -> speakers.sortedByDescending { it.createdAt }
            SortOption.DATE_OLDEST -> speakers.sortedBy { it.createdAt }
            SortOption.NAME_ASC -> speakers.sortedBy { it.name }
            SortOption.NAME_DESC -> speakers.sortedByDescending { it.name }
            SortOption.TYPE_ASC -> speakers.sortedBy { it.speechType }
            SortOption.TYPE_DESC -> speakers.sortedByDescending { it.speechType }
        }
    }

    var showConfirmDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val navigationItems = listOf("Speeches", "Custom", "Saved Data")
    var selectedItem = remember { mutableIntStateOf(2) } // Set to 2 for "Saved Data"

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Saved Speech Data") },
                actions = {
                    // Sort button
                    IconButton(onClick = { showSortOptions = true }) {
                        Icon(
                            painter = painterResource(R.drawable.sort_24px),
                            contentDescription = "Sort Options"
                        )
                    }

                    // Share All button
                    IconButton(
                        onClick = {
                            if (sortedSpeakers.isNotEmpty()) {
                                shareSpeakerData(context, sortedSpeakers)
                                scope.launch {
                                    snackbarHostState.showSnackbar("Sharing all speech data")
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

                    // Delete All button
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
            NavigationBar {
                navigationItems.forEachIndexed { index, item ->
                    NavigationBarItem(
                        icon = {
                            when (index) {
                                0 -> Icon(
                                    painter = painterResource(R.drawable.podium_24px),
                                    contentDescription = item
                                )

                                1 -> Icon(
                                    painter = painterResource(R.drawable.timer_24px),
                                    contentDescription = item
                                )

                                2 -> Icon(Icons.Filled.Person, contentDescription = item)
                            }
                        },
                        label = { Text(item) },
                        selected = selectedItem.intValue == index,
                        onClick = {
                            selectedItem.intValue = index
                            when (index) {
                                0 -> navController.navigate(Screen.Main.route) {
                                    // Pop up to the start destination to avoid building up a stack
                                    popUpTo(Screen.Main.route) { inclusive = true }
                                }

                                1 -> navController.navigate(Screen.CustomSpeechScreen.route)
                                // We're already on index 2, no need to navigate
                            }
                        }
                    )
                }
            }
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
            } else if (error != null) {
                Text(
                    text = "Error: $error",
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp),
                    color = MaterialTheme.colorScheme.error
                )
            } else if (sortedSpeakers.isEmpty()) {
                EmptyStateContent(
                    onAddSpeech = { navController.navigate(Screen.Main.route) }
                )
            } else {
                SpeechDataList(
                    speakers = sortedSpeakers,
                    onShareClick = { speaker ->
                        shareSingleSpeaker(context, speaker)
                        scope.launch {
                            snackbarHostState.showSnackbar("Sharing ${speaker.name}'s speech data")
                        }
                    },
                    onDeleteClick = { speaker ->
                        viewModel.deleteSpeaker(speaker)
                        scope.launch {
                            snackbarHostState.showSnackbar("Deleted ${speaker.name}'s speech data")
                        }
                    }
                )
            }

            // Confirm dialog for clearing all data
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
                                scope.launch {
                                    snackbarHostState.showSnackbar("All speech data cleared")
                                }
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

            // Sort options dialog
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
        items(speakers) { speaker ->
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
    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    val formattedDate = dateFormat.format(speaker.createdAt)

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

            // Display creation date and time
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
                        contentDescription = "Share",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(onClick = onDeleteClick) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
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

// Function to share a single speaker's data
fun shareSingleSpeaker(context: Context, speaker: Speaker) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    val formattedDate = dateFormat.format(speaker.createdAt)

    val shareText = """
        Simple Speech Timer - Speaker Data
        
        Name: ${speaker.name}
        Speech Type: ${speaker.speechType}
        Duration: ${speaker.duration}
        Recorded on: $formattedDate
        
        Created with Simple Speech Timer App
    """.trimIndent()

    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, shareText)
        type = "text/plain"
    }

    context.startActivity(Intent.createChooser(sendIntent, "Share Speech Data"))
}

//todo remove duplicate code by extracting method from above
fun shareSpeakerData(context: Context, speakers: List<Speaker>) {
    val shareText = speakers.joinToString(
        prefix = "Simple Speech Timer - Speaker Data\n\n",
        separator = "\n--------------------------\n",
        postfix = "\n\nCreated with Simple Speech Timer App for Android"
    ) { speaker ->
        val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        val formattedDate = dateFormat.format(speaker.createdAt)

        """
       Name: ${speaker.name}
       Speech Type: ${speaker.speechType}
       Duration: ${speaker.duration}
       Recorded on: $formattedDate
       """.trimIndent()
    }

    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, shareText)
        type = "text/plain"
    }

    context.startActivity(Intent.createChooser(sendIntent, "Share All Speech Data"))
}


// Function to share all speakers' data as a formatted ASCII table
//fun shareSpeakerData(context: Context, speakers: List<Speaker>) {
//    val dateFormat = SimpleDateFormat("MM/dd/yyyy HH:mm", Locale.getDefault())
//
//    // Create consistent table structure with StringBuilder for better control
//    val sb = StringBuilder()
//
//    // Add header
//    sb.append("This is a record of all timed speeches from Simple Speech Timer App:\n\n")
//
//    // Define table formatting consistently
//    val separator = "+----------------------+---------------------------+------------+---------------------+"
//    val headerFormat = "| %-20s | %-25s | %-10s | %-19s |"
//    val rowFormat = "| %-20s | %-25s | %-10s | %-19s |"
//
//    // Add table structure
//    sb.append(separator).append("\n")
//    sb.append(String.format(headerFormat, "Name", "Speech Type", "Duration", "Date & Time")).append("\n")
//    sb.append(separator).append("\n")
//
//    // Add each row with consistent formatting
//    speakers.forEach { speaker ->
//        val formattedDate = dateFormat.format(speaker.createdAt)
//        sb.append(String.format(
//            rowFormat,
//            speaker.name.take(20),
//            speaker.speechType.take(25),
//            speaker.duration,
//            formattedDate
//        )).append("\n")
//    }
//
//    sb.append(separator).append("\n\n")
//
//    // Add footer
//    val currentTimestamp = dateFormat.format(Date())
//    sb.append("Created by Simple Speech Timer App for Android\n")
//    sb.append("Generated on: $currentTimestamp\n")
//    sb.append("Total Speeches: ${speakers.size}")
//
//    val shareText = sb.toString()
//
//    val sendIntent = Intent().apply {
//        action = Intent.ACTION_SEND
//        putExtra(Intent.EXTRA_TEXT, shareText)
//        type = "text/plain"
//    }
//
//    context.startActivity(Intent.createChooser(sendIntent, "Share All Speech Data"))
//}