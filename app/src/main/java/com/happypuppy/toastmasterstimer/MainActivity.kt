package com.happypuppy.toastmasterstimer

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.happypuppy.toastmasterstimer.data.preferences.AppTheme
import com.happypuppy.toastmasterstimer.data.preferences.DataStoreManager
import com.happypuppy.toastmasterstimer.model.SpeechType
import com.happypuppy.toastmasterstimer.navigation.Screen
import com.happypuppy.toastmasterstimer.navigation.Screen.SavedDataScreen
import com.happypuppy.toastmasterstimer.navigation.TimerRoute
import com.happypuppy.toastmasterstimer.ui.components.SpeechTimerBottomBar
import com.happypuppy.toastmasterstimer.ui.components.SpeechTimerButton
import com.happypuppy.toastmasterstimer.ui.screens.SavedDataScreen
import com.happypuppy.toastmasterstimer.ui.screens.SettingsScreen
import com.happypuppy.toastmasterstimer.ui.screens.SpeechTimePicker
import com.happypuppy.toastmasterstimer.ui.screens.SpeechTimerViewModel
import com.happypuppy.toastmasterstimer.ui.screens.TimerScreen
import com.happypuppy.toastmasterstimer.ui.screens.TimerViewModel
import com.happypuppy.toastmasterstimer.ui.theme.SimpleSpeechTimerTheme
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.compose.koinViewModel

class MainActivity : ComponentActivity() {
    private val dataStoreManager: DataStoreManager by inject()
    private val startup = StartupTheme()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Must run before super.onCreate. Keeps the system splash on screen until DataStore
        // resolves the stored theme (or the 2s safety timeout fires), so the first frame uses it.
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        splashScreen.setKeepOnScreenCondition { !startup.isResolved }
        enableEdgeToEdge()

        lifecycleScope.launch { startup.collectFrom(dataStoreManager.themeFlow) }

        setContent {
            val themePref by startup.theme.collectAsState()
            val resolvedTheme = themePref ?: return@setContent
            val isSystemDark = isSystemInDarkTheme()
            val useDarkTheme = when (resolvedTheme) {
                AppTheme.SYSTEM_DEFAULT -> isSystemDark
                AppTheme.LIGHT -> false
                AppTheme.DARK -> true
            }

            DisposableEffect(useDarkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(
                        Color.TRANSPARENT,
                        Color.TRANSPARENT
                    ) { useDarkTheme },
                    navigationBarStyle = SystemBarStyle.auto(
                        LIGHT_SCRIM,
                        DARK_SCRIM
                    ) { useDarkTheme }
                )
                onDispose {}
            }

            val koinViewModel = koinViewModel<SpeechTimerViewModel>()
            SimpleSpeechTimerTheme(themePreference = resolvedTheme) {
                val navController = rememberNavController()
                NavGraph(navController = navController, koinViewModel = koinViewModel)
            }
        }
    }

    companion object {
        // Default scrims recommended by AndroidX Activity for edge-to-edge 3-button nav bars.
        private val LIGHT_SCRIM = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
        private val DARK_SCRIM = Color.argb(0x80, 0x1b, 0x1b, 0x1b)
    }
}

@Composable
fun NavGraph(navController: NavHostController, koinViewModel: SpeechTimerViewModel) {
    NavHost(navController = navController, startDestination = Screen.Main.route) {
        composable(Screen.Main.route) {
            MainScreen(navController = navController)
        }
        composable(Screen.Settings.route) {
            SettingsScreen(navController)
        }
        composable<TimerRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<TimerRoute>()
            val timerViewModel = koinViewModel<TimerViewModel>(viewModelStoreOwner = backStackEntry)

            TimerScreen(
                navController = navController,
                route = route,
                viewModel = koinViewModel,
                timerViewModel = timerViewModel
            )
        }
        composable(Screen.CustomSpeechScreen.route) {
            SpeechTimePicker(navController = navController)
        }
        composable(SavedDataScreen.route) {
            SavedDataScreen(navController, koinViewModel)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(navController: NavHostController, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = "Simple Speech Timer") },
                navigationIcon = {
                    Icon(
                        painter = painterResource(R.drawable.hourglass_top_24px),
                        contentDescription = null,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                },
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.Settings.route) }) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        bottomBar = {
            SpeechTimerBottomBar(
                currentRoute = Screen.Main.route,
                navController = navController
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SpeechType.presets.forEach { type ->
                SpeechTimerButton(speechType = type, navController = navController)
            }
        }
    }
}
