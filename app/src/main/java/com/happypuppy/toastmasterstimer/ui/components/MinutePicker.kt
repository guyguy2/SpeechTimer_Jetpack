package com.happypuppy.toastmasterstimer.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MinutePicker(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange,
    increaseDescription: String,
    decreaseDescription: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            fontSize = 16.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Card(
            modifier = Modifier
                .width(120.dp)
                .padding(8.dp),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            // This Column needs to have fillMaxWidth and center alignment
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                IconButton(
                    onClick = {
                        if (value < range.last) {
                            onValueChange(value + 1)
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .align(Alignment.CenterHorizontally)
                        .semantics { contentDescription = increaseDescription }
                ) {
                    // The glyph is decoration; the button is named by its description.
                    Text(
                        text = "▲",
                        fontSize = 24.sp,
                        modifier = Modifier.clearAndSetSemantics { }
                    )
                }

                Text(
                    text = "$value",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                IconButton(
                    onClick = {
                        if (value > range.first) {
                            onValueChange(value - 1)
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .align(Alignment.CenterHorizontally)
                        .semantics { contentDescription = decreaseDescription }
                ) {
                    Text(
                        text = "▼",
                        fontSize = 24.sp,
                        modifier = Modifier.clearAndSetSemantics { }
                    )
                }
            }
        }

        Text(
            text = "minutes",
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}