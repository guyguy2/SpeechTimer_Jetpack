package com.happypuppy.toastmasterstimer.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController

@Composable
fun SpeechTimerButton(
    text: String,
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val regex = "\\((\\d+) TO (\\d+) MIN\\)".toRegex()
    val matchResult = regex.find(text)

    val minMinutes = matchResult?.groupValues?.get(1)?.toIntOrNull() ?: 1
    val maxMinutes = matchResult?.groupValues?.get(2)?.toIntOrNull() ?: 2

    Button(
        onClick = {
            navController.navigate(
                "timer_screen/${text.replace("/", "-")}/$minMinutes/$maxMinutes"
            )
        },
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 76.dp)
            .padding(horizontal = 8.dp)
    ) {
        Text(
            text = text,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            textAlign = TextAlign.Center
        )
    }
}
