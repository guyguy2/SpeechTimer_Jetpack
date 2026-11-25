package com.happypuppy.toastmasterstimer.ui.layout

import androidx.window.core.layout.WindowSizeClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for Wave 7 adaptive layout breakpoint mapping (P1-3).
 */
class AdaptiveLayoutTest {

    @Test
    fun compactPortrait_usesSingleColumnListWithSwipeEnabled() {
        for (width in listOf(320, 360, 412, 599)) {
            val spec = adaptiveLayoutSpecFor(WindowSizeClass(width, 800))
            assertFalse("width=$width should use single-column presets", spec.useTwoColumnPresets)
            assertFalse("width=$width should use LazyColumn for Saved Data", spec.useSavedDataGrid)
            assertTrue("width=$width should keep swipe enabled", spec.enableSwipeToDelete)
        }
    }

    @Test
    fun compactWidthLandscape_usesTwoColumnPresetsWithListAndSwipe() {
        val spec = adaptiveLayoutSpecFor(WindowSizeClass(540, 360))
        assertTrue(spec.useTwoColumnPresets)
        assertFalse(spec.useSavedDataGrid)
        assertTrue(spec.enableSwipeToDelete)
    }

    @Test
    fun veryNarrowShortWindow_staysSingleColumnPresets() {
        val spec = adaptiveLayoutSpecFor(WindowSizeClass(320, 400))
        assertFalse(spec.useTwoColumnPresets)
        assertFalse(spec.useSavedDataGrid)
        assertTrue(spec.enableSwipeToDelete)
    }

    @Test
    fun mediumWidth_usesTwoColumnPresetsAndSavedDataGridWithoutSwipe() {
        for ((width, height) in listOf(600 to 800, 640 to 360, 720 to 900, 839 to 600)) {
            val spec = adaptiveLayoutSpecFor(WindowSizeClass(width, height))
            assertEquals(
                AdaptiveLayoutSpec(
                    useTwoColumnPresets = true,
                    useSavedDataGrid = true,
                    enableSwipeToDelete = false
                ),
                spec
            )
        }
    }

    @Test
    fun expandedWidth_usesTwoColumnPresetsAndSavedDataGridWithoutSwipe() {
        for ((width, height) in listOf(840 to 900, 1024 to 768, 1200 to 800)) {
            val spec = adaptiveLayoutSpecFor(WindowSizeClass(width, height))
            assertTrue("width=$width should use 2x2 presets", spec.useTwoColumnPresets)
            assertTrue("width=$width should use Saved Data grid", spec.useSavedDataGrid)
            assertFalse("width=$width should disable swipe", spec.enableSwipeToDelete)
        }
    }
}
