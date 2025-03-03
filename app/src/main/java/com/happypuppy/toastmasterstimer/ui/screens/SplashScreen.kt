package com.happypuppy.toastmasterstimer.ui.screens

import android.view.animation.OvershootInterpolator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.happypuppy.toastmasterstimer.R
import com.happypuppy.toastmasterstimer.navigation.Screen
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(navController: NavHostController) {
    // Create animatable values for our animations
    val scale = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }

    // Background color gradient from primary to surface
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surface

    // Launch effect for animations
    LaunchedEffect(key1 = true) {
        // Animate logo scale with overshoot for a bounce effect
        scale.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 600,
                easing = {
                    OvershootInterpolator(2f).getInterpolation(it)
                }
            )
        )

        // Delay before text appears
        delay(150)

        // Animate text fade in
        textAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 600)
        )

        // Delay before navigating away from splash screen
        delay(1500)

        // Navigate to main screen
        navController.navigate(Screen.Main.route) {
            // Remove splash from back stack
            popUpTo(Screen.Main.route) {
                inclusive = true
            }
        }
    }

    // Splash screen UI
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(16.dp)
        ) {
            // App logo with scale animation
            Image(
                painter = painterResource(id = R.drawable.hour_glass_svg),
                contentDescription = "App Logo",
                modifier = Modifier
                    .size(480.dp)
                    .scale(scale.value)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // App name with fade-in animation
            Text(
                text = "Simple Speech Timer",
                color = Color.White,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.scale(textAlpha.value)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Tagline with fade-in animation
            Text(
                text = "Perfect timing for every speech",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 18.sp,
                modifier = Modifier.scale(textAlpha.value)
            )
        }
    }
}