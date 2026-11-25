package com.happypuppy.toastmasterstimer.ui.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import com.happypuppy.toastmasterstimer.R

@Composable
fun InputDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    title: String = stringResource(R.string.dialog_enter_name),
    initialText: String = ""
) {
    var text by rememberSaveable(initialText) { mutableStateOf(initialText) }
    val trimmed = text.trim()
    val focusRequester = remember { FocusRequester() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            // Focus the name field as soon as the dialog opens, which also brings up the keyboard. The
            // request runs here, in the dialog content next to the field, so the FocusRequester is
            // attached to a composed field when it fires (requesting from the caller's composition could
            // run before the dialog window has composed its content and throw).
            LaunchedEffect(Unit) { focusRequester.requestFocus() }
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                modifier = Modifier.focusRequester(focusRequester),
                label = { Text(stringResource(R.string.dialog_speaker_name_label)) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    capitalization = KeyboardCapitalization.Words
                )
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(trimmed)
                    onDismiss()
                },
                enabled = trimmed.isNotEmpty()
            ) {
                Text(stringResource(R.string.common_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        }
    )
}
