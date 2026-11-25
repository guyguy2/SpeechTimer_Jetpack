package com.happypuppy.toastmasterstimer.util

import android.content.Context
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource

/**
 * Text that a ViewModel or other non-UI code can hand to the UI without needing a Context.
 * The UI resolves it with [asString].
 */
sealed interface UiText {
    data class DynamicString(val value: String) : UiText

    data class StringResource(
        @StringRes val id: Int,
        val args: List<Any> = emptyList()
    ) : UiText

    data class PluralResource(
        @PluralsRes val id: Int,
        val count: Int,
        val args: List<Any> = emptyList()
    ) : UiText

    @Composable
    fun asString(): String = when (this) {
        is DynamicString -> value
        is StringResource -> stringResource(id, *args.toTypedArray())
        is PluralResource -> pluralStringResource(id, count, *args.toTypedArray())
    }

    fun asString(context: Context): String = when (this) {
        is DynamicString -> value
        is StringResource -> context.getString(id, *args.toTypedArray())
        is PluralResource -> context.resources.getQuantityString(id, count, *args.toTypedArray())
    }
}
