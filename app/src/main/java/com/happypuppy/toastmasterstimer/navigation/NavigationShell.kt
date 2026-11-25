package com.happypuppy.toastmasterstimer.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.happypuppy.toastmasterstimer.R

private data class TopLevelTab(
    val route: String,
    @StringRes val labelRes: Int
)

private val topLevelTabs = listOf(
    TopLevelTab(Screen.Main.route, R.string.nav_speeches),
    TopLevelTab(Screen.CustomSpeechScreen.route, R.string.nav_custom),
    TopLevelTab(Screen.SavedDataScreen.route, R.string.nav_saved_data)
)

private val topLevelRoutes = topLevelTabs.mapTo(mutableSetOf()) { it.route }

@Composable
fun SpeechTimerNavigationShell(
    navController: NavHostController,
    content: @Composable () -> Unit
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Main.route
    val showNavigationSuite = currentRoute in topLevelRoutes
    val adaptiveInfo = currentWindowAdaptiveInfoV2()
    val layoutType = if (showNavigationSuite) {
        NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(adaptiveInfo)
    } else {
        NavigationSuiteType.None
    }

    val tabItems = topLevelTabs.map { tab -> tab to stringResource(tab.labelRes) }

    NavigationSuiteScaffold(
        layoutType = layoutType,
        navigationSuiteItems = {
            tabItems.forEach { (tab, label) ->
                item(
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
                    },
                    icon = {
                        when (tab.route) {
                            Screen.Main.route -> Icon(
                                painter = painterResource(R.drawable.podium_24px),
                                contentDescription = null
                            )
                            Screen.CustomSpeechScreen.route -> Icon(
                                painter = painterResource(R.drawable.timer_24px),
                                contentDescription = null
                            )
                            else -> Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = null
                            )
                        }
                    },
                    label = { Text(label) }
                )
            }
        }
    ) {
        content()
    }
}
