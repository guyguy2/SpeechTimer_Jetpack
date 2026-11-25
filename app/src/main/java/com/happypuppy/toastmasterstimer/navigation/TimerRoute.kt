package com.happypuppy.toastmasterstimer.navigation

import com.happypuppy.toastmasterstimer.model.SpeechType
import kotlinx.serialization.Serializable

/**
 * Type-safe route to the timer screen. [speechTypeId] is a [SpeechType.id] (a fixed token, never
 * display text), [customLabel] is optional free text and travels as a query argument, so no part
 * of the title is ever parsed back out of a string.
 */
@Serializable
data class TimerRoute(
    val speechTypeId: String,
    val minMinutes: Int,
    val maxMinutes: Int,
    val customLabel: String? = null
) {
    /** The type for [speechTypeId]; an id this version does not know is treated as a custom speech. */
    val speechType: SpeechType
        get() = SpeechType.fromId(speechTypeId) ?: SpeechType.CUSTOM

    val title: String
        get() = speechType.timerTitle(minMinutes, maxMinutes, customLabel)
}
