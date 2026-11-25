package com.happypuppy.toastmasterstimer.ui.screens

/**
 * Pure viewport-sizing helpers for [TimerFace], separated so phone, landscape, tablet, and font
 * scale behavior can be verified directly in JVM unit tests.
 */
internal object TimerLayoutMetrics {
    const val SIDE_BY_SIDE_MIN_WIDTH_DP = 500f
    const val COMPACT_HEIGHT_THRESHOLD_DP = 460f
    const val MIN_RENDERED_DIGIT_DP = 48f
    const val MAX_RENDERED_DIGIT_DP = 156f
    private const val MIN_DIGIT_FONT_SP = 24f
    private const val HORIZONTAL_PADDING_DP = 32f
    private const val MIN_PANE_WIDTH_DP = 160f
    private const val SIDE_BY_SIDE_LEFT_WIDTH_FRACTION = 0.56f
    private const val FIVE_CHAR_WIDTH_RATIO = 3.5f
    private const val PORTRAIT_HEIGHT_FRACTION = 0.20f
    private const val SIDE_BY_SIDE_HEIGHT_FRACTION = 0.32f

    /**
     * Returns true when the timer viewport is wide enough and landscape-oriented for the
     * side-by-side readout + controls layout.
     */
    fun isSideBySide(maxWidthDp: Float, maxHeightDp: Float): Boolean =
        maxWidthDp > maxHeightDp && maxWidthDp >= SIDE_BY_SIDE_MIN_WIDTH_DP

    /**
     * Computes a font size in `sp` for the `"MM:SS"` timer digits so they scale smoothly across
     * compact phones, landscape viewports, and tablets without wrapping or clipping even at large
     * system font scales (1.3x, 2.0x).
     */
    fun digitFontSizeSp(
        maxWidthDp: Float,
        maxHeightDp: Float,
        fontScale: Float,
        isSideBySide: Boolean
    ): Float {
        val safeFontScale = fontScale.coerceAtLeast(1f)
        val paneWidthDp = if (isSideBySide) {
            (maxWidthDp * SIDE_BY_SIDE_LEFT_WIDTH_FRACTION - HORIZONTAL_PADDING_DP)
                .coerceAtLeast(MIN_PANE_WIDTH_DP)
        } else {
            (maxWidthDp - HORIZONTAL_PADDING_DP).coerceAtLeast(MIN_PANE_WIDTH_DP)
        }
        val widthDrivenDp = paneWidthDp / FIVE_CHAR_WIDTH_RATIO
        val heightFraction = if (isSideBySide) SIDE_BY_SIDE_HEIGHT_FRACTION else PORTRAIT_HEIGHT_FRACTION
        val heightDrivenDp = (maxHeightDp * heightFraction).coerceAtLeast(MIN_RENDERED_DIGIT_DP)
        val targetRenderedDp = minOf(widthDrivenDp, heightDrivenDp)
            .coerceIn(MIN_RENDERED_DIGIT_DP, MAX_RENDERED_DIGIT_DP)
        return (targetRenderedDp / safeFontScale).coerceAtLeast(MIN_DIGIT_FONT_SP)
    }
}
