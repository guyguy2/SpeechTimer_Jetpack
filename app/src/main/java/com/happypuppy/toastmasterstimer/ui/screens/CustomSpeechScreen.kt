package com.happypuppy.toastmasterstimer.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.happypuppy.toastmasterstimer.R
import com.happypuppy.toastmasterstimer.navigation.Screen
import com.happypuppy.toastmasterstimer.ui.components.MinutePicker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeechTimePicker(navController: NavHostController) {
    var startMinutes by remember { mutableIntStateOf(8) }
    var endMinutes by remember { mutableIntStateOf(10) }
    val navigationItems = listOf("Speeches", "Custom", "Saved Data")
    var selectedItem = remember { mutableIntStateOf(1) }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Set Speech Duration") }
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
                            if (selectedItem.intValue == 0)
                                navController.navigate(Screen.Main.route)
                            if (selectedItem.intValue == 2)
                                navController.navigate(Screen.SavedDataScreen.route)
                        }
                    )
                }
            }
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
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MinutePicker(
                            label = "From",
                            value = startMinutes,
                            onValueChange = { newValue ->
                                startMinutes = newValue
                                if (startMinutes > endMinutes) {
                                    endMinutes = startMinutes
                                }
                            },
                            range = 1..60
                        )

                        Text(
                            text = "to",
                            fontSize = 18.sp,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        MinutePicker(
                            label = "To",
                            value = endMinutes,
                            onValueChange = { newValue ->
                                endMinutes = newValue
                                if (endMinutes < startMinutes) {
                                    startMinutes = endMinutes
                                }
                            },
                            range = startMinutes + 1..60
                        )
                    }

                    Text(
                        text = "Speech will last ${if (startMinutes == endMinutes) "$startMinutes minutes" else "between $startMinutes and $endMinutes minutes"}",
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }

                Button(
                    onClick = {
                        // Navigate to TimerScreen with parameters
                        navController.navigate(
                            "timer_screen/Custom Speech (${startMinutes} - ${endMinutes} min)/${startMinutes}/${endMinutes}"
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
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

