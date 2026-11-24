package com.happypuppy.toastmasterstimer.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.navigation.NavHostController
import com.happypuppy.toastmasterstimer.R
import com.happypuppy.toastmasterstimer.alert.AlertVibration
import com.happypuppy.toastmasterstimer.alert.AndroidAlertPlayer
import com.happypuppy.toastmasterstimer.data.preferences.AppTheme
import com.happypuppy.toastmasterstimer.data.preferences.DataStoreManager
import com.happypuppy.toastmasterstimer.util.AppVersionInfo
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

const val FOREGROUND_ONLY_NOTE =
    "Alerts only play while this app is open. Keep the timer screen open and the screen on while timing."

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavHostController) {
    val dataStoreManager = koinInject<DataStoreManager>()
    var showAboutDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ThemeSelector(dataStoreManager)

            HorizontalDivider()

            VibrationSetting(dataStoreManager)

            SoundSetting(dataStoreManager)

            Text(
                text = FOREGROUND_ONLY_NOTE,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider()

            RateAppSetting()

            AboutSetting(onClick = { showAboutDialog = true })
        }

        if (showAboutDialog) {
            AboutDialog(onDismiss = { showAboutDialog = false })
        }
    }
}

@Composable
fun ThemeSelector(dataStoreManager: DataStoreManager) {
    val scope = rememberCoroutineScope()
    val currentTheme by dataStoreManager.themeFlow.collectAsState(initial = AppTheme.SYSTEM_DEFAULT)

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Theme",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        ThemeOption(
            title = "System Default",
            icon = painterResource(R.drawable.smartphone_24px),
            selected = currentTheme == AppTheme.SYSTEM_DEFAULT,
            onClick = {
                scope.launch {
                    dataStoreManager.setTheme(AppTheme.SYSTEM_DEFAULT)
                }
            }
        )

        ThemeOption(
            title = "Light",
            icon = painterResource(R.drawable.light_mode_24px),
            selected = currentTheme == AppTheme.LIGHT,
            onClick = {
                scope.launch {
                    dataStoreManager.setTheme(AppTheme.LIGHT)
                }
            }
        )

        ThemeOption(
            title = "Dark",
            icon = painterResource(R.drawable.dark_mode_24px),
            selected = currentTheme == AppTheme.DARK,
            onClick = {
                scope.launch {
                    dataStoreManager.setTheme(AppTheme.DARK)
                }
            }
        )
    }
}

@Composable
fun ThemeOption(
    title: String,
    icon: Painter,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )
        Icon(
            painter = icon,
            contentDescription = null,
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .size(24.dp)
        )
        Text(title, modifier = Modifier.weight(1f))
    }
}

@Composable
fun VibrationSetting(dataStoreManager: DataStoreManager) {
    val context = LocalContext.current
    val alertPlayer = remember(context) { AndroidAlertPlayer(context) }
    val scope = rememberCoroutineScope()
    val isEnabled by dataStoreManager.vibrateEnabledFlow.collectAsState(initial = true)

    SettingSwitch(
        title = "Vibration",
        description = "Vibrate when display changes color",
        checked = isEnabled,
        onCheckedChange = { enabled ->
            scope.launch {
                dataStoreManager.setVibrateEnabled(enabled)

                if (enabled) {
                    alertPlayer.vibrate(AlertVibration.PREVIEW)
                }
            }
        }
    )
}

@Composable
fun SoundSetting(dataStoreManager: DataStoreManager) {
    val context = LocalContext.current
    val alertPlayer = remember(context) { AndroidAlertPlayer(context) }
    DisposableEffect(alertPlayer) {
        onDispose { alertPlayer.stopSound() }
    }
    val scope = rememberCoroutineScope()
    val isEnabled by dataStoreManager.soundEnabledFlow.collectAsState(initial = false)

    SettingSwitch(
        title = "Sound",
        description = "Play sound when display changes color",
        checked = isEnabled,
        onCheckedChange = { enabled ->
            scope.launch {
                dataStoreManager.setSoundEnabled(enabled)

                if (enabled) {
                    alertPlayer.playSound()
                }
            }
        }
    )
}

@Composable
fun SettingSwitch(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            thumbContent = if (checked) {
                {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        modifier = Modifier.size(SwitchDefaults.IconSize),
                    )
                }
            } else {
                null
            }
        )
    }
}

@Composable
fun RateAppSetting() {
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                val marketIntent = Intent(Intent.ACTION_VIEW).apply {
                    data = "market://details?id=${context.packageName}".toUri()
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                try {
                    context.startActivity(marketIntent)
                } catch (_: ActivityNotFoundException) {
                    val webIntent = Intent(Intent.ACTION_VIEW).apply {
                        data = "https://play.google.com/store/apps/details?id=${context.packageName}".toUri()
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    try {
                        context.startActivity(webIntent)
                    } catch (_: ActivityNotFoundException) {
                        // No store or browser installed to handle the intent
                    }
                }
            }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = null,
            modifier = Modifier
                .padding(end = 16.dp)
                .size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Rate App",
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@Composable
fun AboutSetting(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            modifier = Modifier
                .padding(end = 16.dp)
                .size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "About",
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val versionInfo = AppVersionInfo.getFullVersionInfo(context)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("About Simple Speech Timer") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("A simple yet effective timer app for speeches and presentations.")

                Spacer(modifier = Modifier.height(8.dp))

                Row {
                    Text(
                        text = "Version:",
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(versionInfo)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text("©2013 - 2026 Simple Speech Timer")
                Text("All rights reserved")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}
