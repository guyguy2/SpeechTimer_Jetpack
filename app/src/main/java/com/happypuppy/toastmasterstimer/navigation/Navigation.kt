package com.happypuppy.toastmasterstimer.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash_screen")
    object Main : Screen("main_screen")
    object Settings : Screen("settings_screen")
    object CustomSpeechScreen : Screen("custom_speech_screen")
    object SavedDataScreen : Screen("saved_data_screen")
}