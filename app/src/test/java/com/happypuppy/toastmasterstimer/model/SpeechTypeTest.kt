package com.happypuppy.toastmasterstimer.model

import com.happypuppy.toastmasterstimer.util.TimerThresholds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins the stable ids (Wave 4 maps stored title strings to these ids, so they must never change),
 * the preset ranges and thresholds (same values as [com.happypuppy.toastmasterstimer.util.TimerContractTest]),
 * and the exact display strings the app shows and stores today.
 */
class SpeechTypeTest {

    private data class Expected(
        val type: SpeechType,
        val id: String,
        val label: String,
        val title: String,
        val min: Int,
        val max: Int,
        val green: Int,
        val yellow: Int,
        val red: Int
    )

    private val expected = listOf(
        Expected(SpeechType.TABLE_TOPICS, "table_topics", "TABLE TOPICS", "TABLE TOPICS (1 TO 2 MIN)", 1, 2, 60, 90, 120),
        Expected(SpeechType.EVALUATION, "evaluation", "EVALUATION", "EVALUATION (2 TO 3 MIN)", 2, 3, 120, 150, 180),
        Expected(SpeechType.ICE_BREAKER, "ice_breaker", "ICE BREAKER", "ICE BREAKER (4 TO 6 MIN)", 4, 6, 240, 300, 360),
        Expected(SpeechType.SPEECH, "speech", "SPEECH", "SPEECH (5 TO 7 MIN)", 5, 7, 300, 360, 420)
    )

    @Test
    fun presets_areInMainScreenOrder() {
        assertEquals(expected.map { it.type }, SpeechType.presets)
    }

    @Test
    fun presets_haveStableIdsAndStoredLabels() {
        expected.forEach { e ->
            assertEquals(e.id, e.type.id)
            assertEquals(e.label, e.type.label)
        }
        assertEquals("custom", SpeechType.CUSTOM.id)
        assertEquals("Custom Speech", SpeechType.CUSTOM.label)
    }

    @Test
    fun presets_haveTheContractRangeAndThresholds() {
        expected.forEach { e ->
            assertEquals(e.min, e.type.presetMinMinutes)
            assertEquals(e.max, e.type.presetMaxMinutes)
            val t = TimerThresholds(e.type.presetMinMinutes!!, e.type.presetMaxMinutes!!)
            assertEquals(e.label, e.green, t.greenSeconds)
            assertEquals(e.label, e.yellow, t.yellowSeconds)
            assertEquals(e.label, e.red, t.redSeconds)
        }
    }

    @Test
    fun custom_hasNoPresetRangeAndIsNotAPreset() {
        assertNull(SpeechType.CUSTOM.presetMinMinutes)
        assertNull(SpeechType.CUSTOM.presetMaxMinutes)
        assertTrue(SpeechType.CUSTOM !in SpeechType.presets)
    }

    @Test
    fun ids_areUnique() {
        val ids = SpeechType.entries.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun fromId_roundTripsEveryTypeAndRejectsUnknownIds() {
        SpeechType.entries.forEach { assertEquals(it, SpeechType.fromId(it.id)) }
        assertNull(SpeechType.fromId(""))
        assertNull(SpeechType.fromId("TABLE TOPICS"))
        assertNull(SpeechType.fromId("table_topics "))
        assertNull(SpeechType.fromId("unknown"))
    }

    @Test
    fun timerTitle_matchesTheTextTheMainButtonsShowToday() {
        expected.forEach { e ->
            assertEquals(e.title, e.type.timerTitle(e.min, e.max))
        }
    }

    @Test
    fun timerTitle_forCustomMatchesTheTitleTheCustomScreenBuiltToday() {
        assertEquals("Custom Speech (3 - 8 min)", SpeechType.CUSTOM.timerTitle(3, 8))
        assertEquals("Custom Speech (59 - 60 min)", SpeechType.CUSTOM.timerTitle(59, 60))
    }

    @Test
    fun timerTitle_customLabelReplacesTheNameOnlyForCustom() {
        assertEquals("Club contest (3 - 8 min)", SpeechType.CUSTOM.timerTitle(3, 8, customLabel = "Club contest"))
        assertEquals(
            "SPEECH (5 TO 7 MIN)",
            SpeechType.SPEECH.timerTitle(5, 7, customLabel = "ignored for presets")
        )
    }

    @Test
    fun timerTitle_blankCustomLabelFallsBackToTheTypeName() {
        assertEquals("Custom Speech (3 - 8 min)", SpeechType.CUSTOM.timerTitle(3, 8, customLabel = ""))
        assertEquals("Custom Speech (3 - 8 min)", SpeechType.CUSTOM.timerTitle(3, 8, customLabel = "   "))
    }
}
