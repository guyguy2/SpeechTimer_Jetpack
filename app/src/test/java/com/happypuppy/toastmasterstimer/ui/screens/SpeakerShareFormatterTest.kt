package com.happypuppy.toastmasterstimer.ui.screens

import com.happypuppy.toastmasterstimer.data.database.repository.Speaker
import com.happypuppy.toastmasterstimer.util.TimerPhase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Date

class SpeakerShareFormatterTest {

    private val labels = SpeakerShareLabels(
        header = "Simple Speech Timer - Speaker Data",
        footerSingle = "Created with Simple Speech Timer App",
        footerAll = "Created with Simple Speech Timer App for Android",
        nameFormat = "Name: %1\$s",
        speechTypeFormat = "Speech Type: %1\$s",
        targetRangeFormat = "Target Range: %1\$s min",
        durationFormat = "Duration: %1\$s",
        phaseFormat = "Phase Reached: %1\$s",
        recordedOnFormat = "Recorded on: %1\$s",
        phaseLabels = mapOf(
            TimerPhase.BEFORE_GREEN to "Before green",
            TimerPhase.GREEN to "Green: minimum reached",
            TimerPhase.YELLOW to "Yellow: wrap up soon",
            TimerPhase.RED to "Red: maximum reached"
        )
    )

    @Test
    fun formatTargetRangeMinutes_formatsWholeMinutesWithEnDash() {
        assertEquals("1\u20132", formatTargetRangeMinutes(60, 120))
        assertEquals("5\u20137", formatTargetRangeMinutes(300, 420))
    }

    @Test
    fun formatTargetRangeMinutes_fallsBackToMmSsForNonMinuteBounds() {
        assertEquals("01:30\u201302:30", formatTargetRangeMinutes(90, 150))
        assertEquals("01:00\u201301:45", formatTargetRangeMinutes(60, 105))
    }

    @Test
    fun formatTargetRangeMinutes_returnsNullForMissingOrInvalidBounds() {
        assertNull(formatTargetRangeMinutes(null, 120))
        assertNull(formatTargetRangeMinutes(60, null))
        assertNull(formatTargetRangeMinutes(0, 120))
        assertNull(formatTargetRangeMinutes(-60, 120))
        assertNull(formatTargetRangeMinutes(180, 120))
    }

    @Test
    fun parseTimerPhase_parsesKnownValuesTolerantlyAndRejectsUnknown() {
        assertEquals(TimerPhase.BEFORE_GREEN, parseTimerPhase("BEFORE_GREEN"))
        assertEquals(TimerPhase.GREEN, parseTimerPhase(" green "))
        assertEquals(TimerPhase.YELLOW, parseTimerPhase("Yellow"))
        assertEquals(TimerPhase.RED, parseTimerPhase("RED"))
        assertNull(parseTimerPhase(null))
        assertNull(parseTimerPhase("   "))
        assertNull(parseTimerPhase("BLUE"))
    }

    @Test
    fun formatSpeakerSummary_includesRangeAndPhaseForV3Row() {
        val v3Speaker = Speaker(
            id = 1,
            name = "Alex Morgan",
            speechType = "TABLE TOPICS",
            duration = "01:35",
            createdAt = Date(1_000L),
            speechTypeId = "table_topics",
            minSeconds = 60,
            maxSeconds = 120,
            elapsedSeconds = 95,
            phaseReached = "YELLOW"
        )

        val summary = formatSpeakerSummary(v3Speaker, "Nov 24, 2026 20:00", labels)

        assertEquals(
            """
            Name: Alex Morgan
            Speech Type: TABLE TOPICS
            Target Range: 1–2 min
            Duration: 01:35
            Phase Reached: Yellow: wrap up soon
            Recorded on: Nov 24, 2026 20:00
            """.trimIndent(),
            summary
        )
    }

    @Test
    fun formatSpeakerSummary_allowsExplicitPhaseLabelOverride() {
        val v3Speaker = Speaker(
            id = 1,
            name = "Sam Lee",
            speechType = "SPEECH",
            duration = "06:00",
            minSeconds = 300,
            maxSeconds = 420,
            phaseReached = "GREEN"
        )

        val summary = formatSpeakerSummary(
            speaker = v3Speaker,
            formattedDate = "Nov 24, 2026 20:05",
            labels = labels,
            phaseLabel = "Custom Green Label"
        )

        assertEquals(
            """
            Name: Sam Lee
            Speech Type: SPEECH
            Target Range: 5–7 min
            Duration: 06:00
            Phase Reached: Custom Green Label
            Recorded on: Nov 24, 2026 20:05
            """.trimIndent(),
            summary
        )
    }

    @Test
    fun formatSpeakerSummary_omitsRangeAndPhaseForLegacyV2Row() {
        val legacySpeaker = Speaker(
            id = 2,
            name = "Blair",
            speechType = "Legacy Custom",
            duration = "03:10",
            createdAt = Date(2_000L)
        )

        val summary = formatSpeakerSummary(legacySpeaker, "Nov 24, 2026 19:00", labels)

        assertEquals(
            """
            Name: Blair
            Speech Type: Legacy Custom
            Duration: 03:10
            Recorded on: Nov 24, 2026 19:00
            """.trimIndent(),
            summary
        )
    }

    @Test
    fun formatSingleSpeakerShareText_wrapsSummaryWithHeaderAndFooter() {
        val speaker = Speaker(
            id = 1,
            name = "Alex",
            speechType = "TABLE TOPICS",
            duration = "01:20",
            minSeconds = 60,
            maxSeconds = 120,
            phaseReached = "GREEN"
        )

        val text = formatSingleSpeakerShareText(speaker, "Nov 24, 2026 20:00", labels)

        assertEquals(
            """
            Simple Speech Timer - Speaker Data

            Name: Alex
            Speech Type: TABLE TOPICS
            Target Range: 1–2 min
            Duration: 01:20
            Phase Reached: Green: minimum reached
            Recorded on: Nov 24, 2026 20:00

            Created with Simple Speech Timer App
            """.trimIndent(),
            text
        )
    }

    @Test
    fun formatAllSpeakersShareText_joinsMultipleSpeakersAndHandlesEmptyList() {
        assertEquals("", formatAllSpeakersShareText(emptyList(), { "unused" }, labels))

        val first = Speaker(
            id = 1,
            name = "Alex",
            speechType = "TABLE TOPICS",
            duration = "01:20",
            createdAt = Date(1_000L),
            minSeconds = 60,
            maxSeconds = 120,
            phaseReached = "GREEN"
        )
        val second = Speaker(
            id = 2,
            name = "Blair",
            speechType = "Legacy",
            duration = "03:00",
            createdAt = Date(2_000L)
        )

        val text = formatAllSpeakersShareText(
            speakers = listOf(first, second),
            formatDate = { date -> if (date.time == 1_000L) "Date 1" else "Date 2" },
            labels = labels
        )

        assertEquals(
            """
            Simple Speech Timer - Speaker Data

            Name: Alex
            Speech Type: TABLE TOPICS
            Target Range: 1–2 min
            Duration: 01:20
            Phase Reached: Green: minimum reached
            Recorded on: Date 1
            --------------------------
            Name: Blair
            Speech Type: Legacy
            Duration: 03:00
            Recorded on: Date 2

            Created with Simple Speech Timer App for Android
            """.trimIndent(),
            text
        )
    }

    @Test
    fun formatSpeakerSummary_resolvesLocalizedSpeechTypeViaIdAndFallsBackToStoredLabel() {
        val localizedLabels = labels.copy(
            speechTypeLabels = mapOf("table_topics" to "Temas de Mesa")
        )
        val known = Speaker(
            id = 1,
            name = "Alex",
            speechType = "TABLE TOPICS",
            duration = "01:20",
            speechTypeId = "table_topics"
        )
        val unknown = Speaker(
            id = 2,
            name = "Blair",
            speechType = "Legacy Custom",
            duration = "03:00",
            speechTypeId = "unknown"
        )

        val knownSummary = formatSpeakerSummary(known, "Date 1", localizedLabels)
        val unknownSummary = formatSpeakerSummary(unknown, "Date 2", localizedLabels)

        assertEquals(
            """
            Name: Alex
            Speech Type: Temas de Mesa
            Duration: 01:20
            Recorded on: Date 1
            """.trimIndent(),
            knownSummary
        )
        assertEquals(
            """
            Name: Blair
            Speech Type: Legacy Custom
            Duration: 03:00
            Recorded on: Date 2
            """.trimIndent(),
            unknownSummary
        )
    }
}
