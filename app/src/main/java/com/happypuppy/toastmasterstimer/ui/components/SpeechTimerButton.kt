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
import com.happypuppy.toastmasterstimer.model.SpeechType
import com.happypuppy.toastmasterstimer.navigation.TimerRoute

@Composable
fun SpeechTimerButton(
    speechType: SpeechType,
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val minMinutes = requireNotNull(speechType.presetMinMinutes) { "${speechType.id} has no preset range" }
    val maxMinutes = requireNotNull(speechType.presetMaxMinutes) { "${speechType.id} has no preset range" }
    val text = speechType.timerTitle(minMinutes, maxMinutes)

    Button(
        onClick = {
            navController.navigate(TimerRoute(speechType.id, minMinutes, maxMinutes))
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
