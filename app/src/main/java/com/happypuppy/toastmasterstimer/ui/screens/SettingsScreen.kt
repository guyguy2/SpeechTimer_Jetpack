package com.happypuppy.toastmasterstimer.ui.screens

import android.content.ActivityNotFoundException
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.navigation.NavHostController
import com.happypuppy.toastmasterstimer.R
import com.happypuppy.toastmasterstimer.alert.AlertVibration
import com.happypuppy.toastmasterstimer.alert.AndroidAlertPlayer
import com.happypuppy.toastmasterstimer.data.preferences.AppTheme
import com.happypuppy.toastmasterstimer.data.preferences.DataStoreManager
import com.happypuppy.toastmasterstimer.ui.layout.ADAPTIVE_CONTENT_MAX_WIDTH
import com.happypuppy.toastmasterstimer.util.AppVersionInfo
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavHostController) {
    val dataStoreManager = koinInject<DataStoreManager>()
    var showAboutDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back)
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
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ThemeSelector(dataStoreManager)

                DynamicColorSetting(dataStoreManager)

                HorizontalDivider()

                VibrationSetting(dataStoreManager)

                SoundSetting(dataStoreManager)

                Text(
                    text = stringResource(R.string.settings_foreground_only_note),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HorizontalDivider()

                RateAppSetting()

                AboutSetting(onClick = { showAboutDialog = true })
            }
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
            text = stringResource(R.string.settings_theme),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        ThemeOption(
            title = stringResource(R.string.settings_theme_system),
            icon = painterResource(R.drawable.smartphone_24px),
            selected = currentTheme == AppTheme.SYSTEM_DEFAULT,
            onClick = {
                scope.launch {
                    dataStoreManager.setTheme(AppTheme.SYSTEM_DEFAULT)
                }
            }
        )

        ThemeOption(
            title = stringResource(R.string.settings_theme_light),
            icon = painterResource(R.drawable.light_mode_24px),
            selected = currentTheme == AppTheme.LIGHT,
            onClick = {
                scope.launch {
                    dataStoreManager.setTheme(AppTheme.LIGHT)
                }
            }
        )

        ThemeOption(
            title = stringResource(R.string.settings_theme_dark),
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
fun DynamicColorSetting(dataStoreManager: DataStoreManager) {
    val scope = rememberCoroutineScope()
    val isEnabled by dataStoreManager.dynamicColorEnabledFlow.collectAsState(initial = false)

    SettingSwitch(
        title = stringResource(R.string.settings_dynamic_color),
        description = stringResource(R.string.settings_dynamic_color_description),
        checked = isEnabled,
        onCheckedChange = { enabled ->
            scope.launch {
                dataStoreManager.setDynamicColorEnabled(enabled)
            }
        }
    )
}

@Composable
fun VibrationSetting(dataStoreManager: DataStoreManager) {
    val context = LocalContext.current
    val alertPlayer = remember(context) { AndroidAlertPlayer(context) }
    val scope = rememberCoroutineScope()
    val isEnabled by dataStoreManager.vibrateEnabledFlow.collectAsState(initial = true)

    SettingSwitch(
        title = stringResource(R.string.settings_vibration),
        description = stringResource(R.string.settings_vibration_description),
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
        title = stringResource(R.string.settings_sound),
        description = stringResource(R.string.settings_sound_description),
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
            .heightIn(min = 48.dp)
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = onCheckedChange
            )
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
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = null,
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
            text = stringResource(R.string.settings_rate_app),
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
            text = stringResource(R.string.settings_about),
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
        title = { Text(stringResource(R.string.settings_about_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.settings_about_description))

                Spacer(modifier = Modifier.height(8.dp))

                Row {
                    Text(
                        text = stringResource(R.string.settings_about_version),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(versionInfo)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(stringResource(R.string.settings_about_copyright))
                Text(stringResource(R.string.settings_about_rights))
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_ok))
            }
        }
    )
}
