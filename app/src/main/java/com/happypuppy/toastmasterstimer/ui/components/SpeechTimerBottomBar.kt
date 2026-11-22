package com.happypuppy.toastmasterstimer.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavHostController
import com.happypuppy.toastmasterstimer.R
import com.happypuppy.toastmasterstimer.navigation.Screen

private data class BottomNavTab(
    val route: String,
    val label: String
)

private val bottomNavTabs = listOf(
    BottomNavTab(Screen.Main.route, "Speeches"),
    BottomNavTab(Screen.CustomSpeechScreen.route, "Custom"),
    BottomNavTab(Screen.SavedDataScreen.route, "Saved Data")
)

@Composable
fun SpeechTimerBottomBar(
    currentRoute: String,
    navController: NavHostController
) {
    NavigationBar {
        bottomNavTabs.forEachIndexed { index, tab ->
            NavigationBarItem(
                icon = {
                    when (index) {
                        0 -> Icon(
                            painter = painterResource(R.drawable.podium_24px),
                            contentDescription = tab.label
                        )

                        1 -> Icon(
                            painter = painterResource(R.drawable.timer_24px),
                            contentDescription = tab.label
                        )

                        else -> Icon(Icons.Filled.Person, contentDescription = tab.label)
                    }
                },
                label = { Text(tab.label) },
                selected = currentRoute == tab.route,
                onClick = {
                    if (currentRoute != tab.route) {
                        navController.navigate(tab.route) {
                            popUpTo(Screen.Main.route) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        }
    }
}
