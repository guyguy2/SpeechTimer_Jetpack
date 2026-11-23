package com.happypuppy.toastmasterstimer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.happypuppy.toastmasterstimer.navigation.Screen
import com.happypuppy.toastmasterstimer.navigation.Screen.SavedDataScreen
import com.happypuppy.toastmasterstimer.ui.components.SpeechTimerBottomBar
import com.happypuppy.toastmasterstimer.ui.components.SpeechTimerButton
import com.happypuppy.toastmasterstimer.ui.screens.SavedDataScreen
import com.happypuppy.toastmasterstimer.ui.screens.SettingsScreen
import com.happypuppy.toastmasterstimer.ui.screens.SpeechTimePicker
import com.happypuppy.toastmasterstimer.ui.screens.SpeechTimerViewModel
import com.happypuppy.toastmasterstimer.ui.screens.SplashScreen
import com.happypuppy.toastmasterstimer.ui.screens.TimerScreen
import com.happypuppy.toastmasterstimer.ui.screens.TimerViewModel
import com.happypuppy.toastmasterstimer.ui.theme.SimpleSpeechTimerTheme
import org.koin.androidx.compose.koinViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val koinViewModel = koinViewModel<SpeechTimerViewModel>()
            SimpleSpeechTimerTheme {
                val navController = rememberNavController()
                NavGraph(navController = navController, koinViewModel = koinViewModel)
            }
        }
    }
}

@Composable
fun NavGraph(navController: NavHostController, koinViewModel: SpeechTimerViewModel) {
    NavHost(navController = navController, startDestination = Screen.Splash.route) {
        composable(Screen.Splash.route) {
            SplashScreen(navController)
        }
        composable(Screen.Main.route) {
            MainScreen(navController = navController)
        }
        composable(Screen.Settings.route) {
            SettingsScreen(navController)
        }
        composable(
            route = Screen.TimerScreen.route,
            arguments = listOf(
                navArgument("title") { type = NavType.StringType },
                navArgument("minMinutes") { type = NavType.IntType },
                navArgument("maxMinutes") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val title = backStackEntry.arguments?.getString("title") ?: "Timer Screen"
            val minMinutes = backStackEntry.arguments?.getInt("minMinutes") ?: 1
            val maxMinutes = backStackEntry.arguments?.getInt("maxMinutes") ?: 2
            val timerViewModel = koinViewModel<TimerViewModel>(viewModelStoreOwner = backStackEntry)

            TimerScreen(
                navController = navController,
                title = title,
                minMinutes = minMinutes,
                maxMinutes = maxMinutes,
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
            SpeechTimerButton(text = "TABLE TOPICS (1 TO 2 MIN)", navController)
            SpeechTimerButton(text = "EVALUATION (2 TO 3 MIN)", navController)
            SpeechTimerButton(text = "ICE BREAKER (4 TO 6 MIN)", navController)
            SpeechTimerButton(text = "SPEECH (5 TO 7 MIN)", navController)
        }
    }
}
