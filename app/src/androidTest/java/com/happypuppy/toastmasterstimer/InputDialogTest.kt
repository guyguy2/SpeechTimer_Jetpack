package com.happypuppy.toastmasterstimer

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.happypuppy.toastmasterstimer.ui.components.InputDialog
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InputDialogTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun nameField_hasFocusAsSoonAsTheDialogOpens() {
        rule.setContent {
            MaterialTheme { InputDialog(onDismiss = {}, onConfirm = {}, title = "Save Time") }
        }
        rule.waitForIdle()

        rule.onNode(hasSetTextAction()).assertIsFocused()
    }
}
