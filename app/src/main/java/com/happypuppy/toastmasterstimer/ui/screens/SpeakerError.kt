package com.happypuppy.toastmasterstimer.ui.screens

import com.happypuppy.toastmasterstimer.R
import com.happypuppy.toastmasterstimer.util.UiText

/**
 * Typed, localizable failures emitted by [SpeechTimerViewModel] instead of raw exception messages.
 */
sealed interface SpeakerError {
    val message: UiText

    data object LoadFailed : SpeakerError {
        override val message: UiText = UiText.StringResource(R.string.saved_error_load_failed)
    }

    data object SaveFailed : SpeakerError {
        override val message: UiText = UiText.StringResource(R.string.saved_error_save_failed)
    }

    data object UpdateFailed : SpeakerError {
        override val message: UiText = UiText.StringResource(R.string.saved_error_update_failed)
    }

    data object DeleteFailed : SpeakerError {
        override val message: UiText = UiText.StringResource(R.string.saved_error_delete_failed)
    }

    data object DeleteAllFailed : SpeakerError {
        override val message: UiText = UiText.StringResource(R.string.saved_error_delete_all_failed)
    }

    data object RestoreFailed : SpeakerError {
        override val message: UiText = UiText.StringResource(R.string.saved_error_restore_failed)
    }
}
