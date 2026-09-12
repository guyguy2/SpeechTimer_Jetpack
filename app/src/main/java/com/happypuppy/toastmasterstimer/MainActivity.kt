package com.happypuppy.toastmasterstimer

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.happypuppy.toastmasterstimer.navigation.Screen
import com.happypuppy.toastmasterstimer.navigation.Screen.SavedDataScreen
import com.happypuppy.toastmasterstimer.ui.components.SpeechTimerButton
import com.happypuppy.toastmasterstimer.ui.screens.SavedDataScreen
import com.happypuppy.toastmasterstimer.ui.screens.SettingsScreen
import com.happypuppy.toastmasterstimer.ui.screens.SpeechTimePicker
import com.happypuppy.toastmasterstimer.ui.screens.SplashScreen
import com.happypuppy.toastmasterstimer.ui.screens.TimerScreen
import com.happypuppy.toastmasterstimer.ui.screens.SpeechTimerViewModel
import com.happypuppy.toastmasterstimer.ui.theme.SimpleSpeechTimerTheme
import org.koin.androidx.compose.koinViewModel

class MainActivity : ComponentActivity() {
    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        //todo locked to portrait - support landscape
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

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
            Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                MainScreen(
                    modifier = Modifier.padding(innerPadding),
                    navController = navController,
                    viewModel = koinViewModel
                )
            }
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

            TimerScreen(
                navController = navController,
                title = title,
                minMinutes = minMinutes,
                maxMinutes = maxMinutes,
                viewModel = koinViewModel
            )
        }
        composable(Screen.CustomSpeechScreen.route) {
            SpeechTimePicker(
                navController = navController,
                viewModel = koinViewModel
            )
        }
        composable(SavedDataScreen.route) {
            SavedDataScreen(navController, koinViewModel)
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(modifier: Modifier = Modifier, navController: NavHostController, viewModel: SpeechTimerViewModel) {
    var selectedItem = remember { mutableIntStateOf(0) }
    val navigationItems = listOf("Speeches", "Custom", "Saved Data")
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(text = "Simple Speech Timer") },
                navigationIcon = {
                    Icon(
                        painter = painterResource(R.drawable.hourglass_top_24px),
                        contentDescription = "Menu"
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
                            if (selectedItem.intValue == 1)
                                navController.navigate(Screen.CustomSpeechScreen.route)
                            if (selectedItem.intValue == 2)
                                navController.navigate(SavedDataScreen.route)
//                            scope.launch {
//                                snackbarHostState.showSnackbar(
//                                    message = "Switched to ${navigationItems[index]}",
//                                    duration = SnackbarDuration.Short
//                                )
//                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        // Screen content goes here
        /*     Text(
                 modifier = Modifier.padding(innerPadding),
                 text = "Current screen: ${navigationItems[selectedItem.intValue]}"
             )*/

        Column(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            SpeechTimerButton(text = "TABLE TOPICS (1 TO 2 MIN)", navController)
            SpeechTimerButton(text = "EVALUATION (2 TO 3 MIN)", navController)
            SpeechTimerButton(text = "ICE BREAKER (4 TO 6 MIN)", navController)
            SpeechTimerButton(text = "SPEECH (5 TO 7 MIN)", navController)
        }
    }

}





