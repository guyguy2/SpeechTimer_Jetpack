package com.happypuppy.toastmasterstimer.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.happypuppy.toastmasterstimer.R
import com.happypuppy.toastmasterstimer.data.database.repository.Speaker
import com.happypuppy.toastmasterstimer.model.SpeechType
import com.happypuppy.toastmasterstimer.navigation.TimerRoute

/**
 * Localized title shown in UI buttons and the timer top bar for a [SpeechType] and range.
 */
@Composable
fun SpeechType.displayTitle(minMinutes: Int, maxMinutes: Int, customLabel: String? = null): String =
    if (this == SpeechType.CUSTOM) {
        val name = customLabel?.takeIf { it.isNotBlank() } ?: stringResource(labelRes)
        stringResource(R.string.timer_title_custom, name, minMinutes, maxMinutes)
    } else {
        stringResource(R.string.timer_title_preset, stringResource(labelRes), minMinutes, maxMinutes)
    }

/** Localized title for [TimerRoute] shown in the timer top bar. */
@Composable
fun TimerRoute.displayTitle(): String = speechType.displayTitle(minMinutes, maxMinutes, customLabel)

/**
 * Localized display name for a saved [Speaker]'s speech type, resolved via [Speaker.speechTypeId]
 * and falling back to the stored [Speaker.speechType] string for legacy or unknown entries.
 */
@Composable
fun speakerSpeechTypeDisplayName(speaker: Speaker): String =
    SpeechType.fromId(speaker.speechTypeId)?.let { stringResource(it.labelRes) } ?: speaker.speechType
