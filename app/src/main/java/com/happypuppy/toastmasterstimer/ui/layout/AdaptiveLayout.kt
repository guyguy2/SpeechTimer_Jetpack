package com.happypuppy.toastmasterstimer.ui.layout

import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass

val ADAPTIVE_CONTENT_MAX_WIDTH = 840.dp
val ADAPTIVE_GRID_MIN_CELL_WIDTH = 320.dp
val ADAPTIVE_COMPACT_FORM_MAX_WIDTH = 560.dp

private const val LANDSCAPE_TWO_COLUMN_MIN_WIDTH_DP = 480

/**
 * Window-size-driven layout decisions for Simple Speech Timer:
 * - [useTwoColumnPresets]: renders the 4 speech presets in a 2x2 grid on medium/expanded widths
 *   or landscape phones wide enough for two columns.
 * - [useSavedDataGrid]: switches Saved Data from a single-column list to an adaptive multi-column
 *   grid capped at [ADAPTIVE_CONTENT_MAX_WIDTH] when width >= 600.dp.
 * - [enableSwipeToDelete]: enables horizontal EndToStart swipe-to-delete in single-column list mode;
 *   disabled in multi-column grid mode where explicit buttons, TalkBack custom actions, and the
 *   hardware Delete key remain active.
 */
@Immutable
data class AdaptiveLayoutSpec(
    val useTwoColumnPresets: Boolean,
    val useSavedDataGrid: Boolean,
    val enableSwipeToDelete: Boolean
)

/** Pure mapping from Material 3 [WindowSizeClass] to [AdaptiveLayoutSpec] for JVM unit testing. */
fun adaptiveLayoutSpecFor(windowSizeClass: WindowSizeClass): AdaptiveLayoutSpec {
    val isMediumOrWider = windowSizeClass.isWidthAtLeastBreakpoint(
        WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND
    )
    val isCompactHeight = !windowSizeClass.isHeightAtLeastBreakpoint(
        WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND
    )
    val isWideEnoughForTwoColumns = windowSizeClass.isWidthAtLeastBreakpoint(
        LANDSCAPE_TWO_COLUMN_MIN_WIDTH_DP
    )

    return AdaptiveLayoutSpec(
        useTwoColumnPresets = isMediumOrWider || (isCompactHeight && isWideEnoughForTwoColumns),
        useSavedDataGrid = isMediumOrWider,
        enableSwipeToDelete = !isMediumOrWider
    )
}

@Composable
fun rememberAdaptiveLayoutSpec(): AdaptiveLayoutSpec {
    val windowSizeClass = currentWindowAdaptiveInfoV2().windowSizeClass
    return adaptiveLayoutSpecFor(windowSizeClass)
}
