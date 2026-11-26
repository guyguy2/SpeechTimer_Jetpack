package com.happypuppy.toastmasterstimer.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.happypuppy.toastmasterstimer.R
import com.happypuppy.toastmasterstimer.alert.AlertVibration
import com.happypuppy.toastmasterstimer.alert.AndroidAlertPlayer
import com.happypuppy.toastmasterstimer.data.preferences.AppTheme
import com.happypuppy.toastmasterstimer.data.preferences.DataStoreManager
import com.happypuppy.toastmasterstimer.ui.components.ExpressiveTonalToggleAction
import com.happypuppy.toastmasterstimer.ui.layout.ADAPTIVE_CONTENT_MAX_WIDTH
import com.happypuppy.toastmasterstimer.ui.theme.SimpleSpeechTimerTheme
import com.happypuppy.toastmasterstimer.util.AppVersionInfo
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun SettingsScreen(navController: NavHostController) {
    val dataStoreManager = koinInject<DataStoreManager>()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val alertPlayer = remember(context) { AndroidAlertPlayer(context) }
    DisposableEffect(alertPlayer) {
        onDispose { alertPlayer.stopSound() }
    }

    val currentTheme by dataStoreManager.themeFlow.collectAsStateWithLifecycle(
        initialValue = AppTheme.SYSTEM_DEFAULT
    )
    val dynamicColorEnabled by dataStoreManager.dynamicColorEnabledFlow.collectAsStateWithLifecycle(
        initialValue = false
    )
    val keepScreenOnEnabled by dataStoreManager.keepScreenOnEnabledFlow.collectAsStateWithLifecycle(
        initialValue = true
    )
    val vibrationEnabled by dataStoreManager.vibrateEnabledFlow.collectAsStateWithLifecycle(
        initialValue = true
    )
    val soundEnabled by dataStoreManager.soundEnabledFlow.collectAsStateWithLifecycle(
        initialValue = false
    )
    val isSoundPlaying by alertPlayer.isSoundPlaying.collectAsStateWithLifecycle()

    var showAboutDialog by rememberSaveable { mutableStateOf(false) }

    SettingsScreenContent(
        currentTheme = currentTheme,
        dynamicColorEnabled = dynamicColorEnabled,
        keepScreenOnEnabled = keepScreenOnEnabled,
        vibrationEnabled = vibrationEnabled,
        soundEnabled = soundEnabled,
        isSoundPlaying = isSoundPlaying,
        onNavigateBack = { navController.navigateUp() },
        onThemeSelect = { theme ->
            scope.launch { dataStoreManager.setTheme(theme) }
        },
        onDynamicColorChange = { enabled ->
            scope.launch { dataStoreManager.setDynamicColorEnabled(enabled) }
        },
        onKeepScreenOnChange = { enabled ->
            scope.launch { dataStoreManager.setKeepScreenOnEnabled(enabled) }
        },
        onVibrationChange = { enabled ->
            scope.launch {
                dataStoreManager.setVibrateEnabled(enabled)
                if (enabled) {
                    alertPlayer.vibrate(AlertVibration.PREVIEW)
                }
            }
        },
        onSoundChange = { enabled ->
            scope.launch {
                dataStoreManager.setSoundEnabled(enabled)
                if (enabled) {
                    alertPlayer.playSound()
                } else {
                    alertPlayer.stopSound()
                }
            }
        },
        onTestVibration = { alertPlayer.vibrate(AlertVibration.PREVIEW) },
        onTestSound = { alertPlayer.playSound() },
        onStopSound = { alertPlayer.stopSound() },
        onRateApp = {
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
        },
        onOpenAbout = { showAboutDialog = true }
    )

    if (showAboutDialog) {
        AboutDialog(onDismiss = { showAboutDialog = false })
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreenContent(
    currentTheme: AppTheme,
    dynamicColorEnabled: Boolean,
    keepScreenOnEnabled: Boolean,
    vibrationEnabled: Boolean,
    soundEnabled: Boolean,
    isSoundPlaying: Boolean,
    onNavigateBack: () -> Unit,
    onThemeSelect: (AppTheme) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onKeepScreenOnChange: (Boolean) -> Unit,
    onVibrationChange: (Boolean) -> Unit,
    onSoundChange: (Boolean) -> Unit,
    onTestVibration: () -> Unit,
    onTestSound: () -> Unit,
    onStopSound: () -> Unit,
    onRateApp: () -> Unit,
    onOpenAbout: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
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
                ThemeSelector(
                    currentTheme = currentTheme,
                    onThemeSelect = onThemeSelect
                )

                SettingSwitch(
                    title = stringResource(R.string.settings_dynamic_color),
                    description = stringResource(R.string.settings_dynamic_color_description),
                    checked = dynamicColorEnabled,
                    onCheckedChange = onDynamicColorChange
                )

                SettingSwitch(
                    title = stringResource(R.string.settings_keep_screen_on),
                    description = stringResource(R.string.settings_keep_screen_on_description),
                    checked = keepScreenOnEnabled,
                    onCheckedChange = onKeepScreenOnChange
                )

                HorizontalDivider()

                SettingSwitch(
                    title = stringResource(R.string.settings_vibration),
                    description = stringResource(R.string.settings_vibration_description),
                    checked = vibrationEnabled,
                    onCheckedChange = onVibrationChange
                )

                SettingSwitch(
                    title = stringResource(R.string.settings_sound),
                    description = stringResource(R.string.settings_sound_description),
                    checked = soundEnabled,
                    onCheckedChange = onSoundChange
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onTestVibration,
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text(stringResource(R.string.settings_test_vibration))
                    }

                    ExpressiveTonalToggleAction(
                        checked = isSoundPlaying,
                        onCheckedChange = { shouldPlay ->
                            if (shouldPlay) onTestSound() else onStopSound()
                        }
                    ) {
                        if (isSoundPlaying) {
                            Icon(
                                painter = painterResource(R.drawable.stop_24px),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.settings_stop_sound))
                        } else {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.settings_test_sound))
                        }
                    }
                }

                Text(
                    text = stringResource(R.string.settings_foreground_only_note),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HorizontalDivider()

                RateAppSetting(onClick = onRateApp)

                AboutSetting(onClick = onOpenAbout)
            }
        }
    }
}

@Composable
fun ThemeSelector(
    currentTheme: AppTheme,
    onThemeSelect: (AppTheme) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .selectableGroup()
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
            onClick = { onThemeSelect(AppTheme.SYSTEM_DEFAULT) }
        )

        ThemeOption(
            title = stringResource(R.string.settings_theme_light),
            icon = painterResource(R.drawable.light_mode_24px),
            selected = currentTheme == AppTheme.LIGHT,
            onClick = { onThemeSelect(AppTheme.LIGHT) }
        )

        ThemeOption(
            title = stringResource(R.string.settings_theme_dark),
            icon = painterResource(R.drawable.dark_mode_24px),
            selected = currentTheme == AppTheme.DARK,
            onClick = { onThemeSelect(AppTheme.DARK) }
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
            .heightIn(min = 48.dp)
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onClick
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            modifier = Modifier.padding(horizontal = 8.dp)
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
                        modifier = Modifier.size(SwitchDefaults.IconSize)
                    )
                }
            } else {
                null
            }
        )
    }
}

@Composable
fun RateAppSetting(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(onClick = onClick)
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
            .heightIn(min = 48.dp)
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

@Preview(showBackground = true)
@Composable
private fun SettingsScreenLightPreview() {
    SimpleSpeechTimerTheme(themePreference = AppTheme.LIGHT, dynamicColor = false) {
        SettingsScreenContent(
            currentTheme = AppTheme.SYSTEM_DEFAULT,
            dynamicColorEnabled = false,
            keepScreenOnEnabled = true,
            vibrationEnabled = true,
            soundEnabled = false,
            isSoundPlaying = false,
            onNavigateBack = {},
            onThemeSelect = {},
            onDynamicColorChange = {},
            onKeepScreenOnChange = {},
            onVibrationChange = {},
            onSoundChange = {},
            onTestVibration = {},
            onTestSound = {},
            onStopSound = {},
            onRateApp = {},
            onOpenAbout = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenDarkSoundPlayingPreview() {
    SimpleSpeechTimerTheme(themePreference = AppTheme.DARK, dynamicColor = false) {
        SettingsScreenContent(
            currentTheme = AppTheme.DARK,
            dynamicColorEnabled = true,
            keepScreenOnEnabled = false,
            vibrationEnabled = true,
            soundEnabled = true,
            isSoundPlaying = true,
            onNavigateBack = {},
            onThemeSelect = {},
            onDynamicColorChange = {},
            onKeepScreenOnChange = {},
            onVibrationChange = {},
            onSoundChange = {},
            onTestVibration = {},
            onTestSound = {},
            onStopSound = {},
            onRateApp = {},
            onOpenAbout = {}
        )
    }
}
