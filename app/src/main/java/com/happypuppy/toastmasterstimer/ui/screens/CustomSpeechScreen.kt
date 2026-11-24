package com.happypuppy.toastmasterstimer.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.happypuppy.toastmasterstimer.R
import com.happypuppy.toastmasterstimer.model.SpeechType
import com.happypuppy.toastmasterstimer.navigation.Screen
import com.happypuppy.toastmasterstimer.navigation.TimerRoute
import com.happypuppy.toastmasterstimer.ui.components.MinutePicker
import com.happypuppy.toastmasterstimer.ui.components.SpeechTimerBottomBar
import com.happypuppy.toastmasterstimer.util.CustomSpeechRange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeechTimePicker(navController: NavHostController) {
    var startMinutes by rememberSaveable { mutableIntStateOf(8) }
    var endMinutes by rememberSaveable { mutableIntStateOf(10) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Set Speech Duration") }
            )
        },
        bottomBar = {
            SpeechTimerBottomBar(
                currentRoute = Screen.CustomSpeechScreen.route,
                navController = navController
            )
        }
    ) { paddingValues ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MinutePicker(
                            label = "From",
                            value = startMinutes,
                            onValueChange = { newValue ->
                                val (clampedStart, clampedEnd) = CustomSpeechRange.updateStart(
                                    newStart = newValue,
                                    currentEnd = endMinutes
                                )
                                startMinutes = clampedStart
                                endMinutes = clampedEnd
                            },
                            range = CustomSpeechRange.startRange,
                            increaseDescription = stringResource(R.string.stepper_increase_minimum),
                            decreaseDescription = stringResource(R.string.stepper_decrease_minimum)
                        )

                        Text(
                            text = "to",
                            fontSize = 18.sp,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        MinutePicker(
                            label = "To",
                            value = endMinutes,
                            onValueChange = { newValue ->
                                val (clampedStart, clampedEnd) = CustomSpeechRange.updateEnd(
                                    currentStart = startMinutes,
                                    newEnd = newValue
                                )
                                startMinutes = clampedStart
                                endMinutes = clampedEnd
                            },
                            range = CustomSpeechRange.endRange(startMinutes),
                            increaseDescription = stringResource(R.string.stepper_increase_maximum),
                            decreaseDescription = stringResource(R.string.stepper_decrease_maximum)
                        )
                    }

                    Text(
                        text = "Speech will last between $startMinutes and $endMinutes minutes",
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        navController.navigate(TimerRoute(SpeechType.CUSTOM.id, startMinutes, endMinutes))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = "Start Timer",
                        fontSize = 18.sp
                    )
                }
            }
        }
    }
}
