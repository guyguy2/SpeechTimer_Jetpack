package com.happypuppy.toastmasterstimer.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ToggleSetting(
    title: String,
    options: List<String>,
    onOptionSelected: (String) -> Unit
) {
    var selectedOption by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            options.forEachIndexed { index, option ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = selectedOption == index,
                        onClick = {
                            selectedOption = index
                            onOptionSelected(option)
                        }
                    )
                    Text(text = option)
                }
            }
        }
    }
}