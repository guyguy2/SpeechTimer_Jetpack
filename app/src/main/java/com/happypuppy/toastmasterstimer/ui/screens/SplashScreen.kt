package com.happypuppy.toastmasterstimer.ui.screens

import android.view.animation.OvershootInterpolator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.happypuppy.toastmasterstimer.R
import com.happypuppy.toastmasterstimer.navigation.Screen
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(navController: NavHostController) {
    val scale = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }

    LaunchedEffect(key1 = true) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 600,
                easing = { OvershootInterpolator(2f).getInterpolation(it) }
            )
        )

        delay(150)

        textAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 600)
        )

        delay(1500)

        navController.navigate(Screen.Main.route) {
            popUpTo(Screen.Splash.route) {
                inclusive = true
            }
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary)
            .safeDrawingPadding(),
        contentAlignment = Alignment.Center
    ) {
        val logoSize = (minOf(maxWidth, maxHeight) * 0.45f).coerceIn(112.dp, 260.dp)
        val contentColor = MaterialTheme.colorScheme.onPrimary

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(16.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.hour_glass_svg),
                contentDescription = "App Logo",
                modifier = Modifier
                    .size(logoSize)
                    .scale(scale.value)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Simple Speech Timer",
                color = contentColor,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(textAlpha.value)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Perfect timing for every speech",
                color = contentColor.copy(alpha = 0.85f),
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(textAlpha.value)
            )
        }
    }
}
