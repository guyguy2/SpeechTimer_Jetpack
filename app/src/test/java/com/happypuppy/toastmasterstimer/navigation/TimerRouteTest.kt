package com.happypuppy.toastmasterstimer.navigation

import com.happypuppy.toastmasterstimer.model.SpeechType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The pure parts of [TimerRoute]. Reading a route back out of a Bundle or SavedStateHandle cannot be
 * tested on the JVM (the unit-test android.jar returns default values, so `toRoute()` yields blanks:
 * verified, see AGENTS.md); the real round trip with awkward text is TimerRouteNavigationTest on a device.
 */
class TimerRouteTest {

    @Test
    fun customLabelDefaultsToNull() {
        assertNull(TimerRoute(SpeechType.CUSTOM.id, 3, 8).customLabel)
    }

    @Test
    fun resolvedType_isTheTypeWithThatId() {
        SpeechType.entries.forEach {
            assertEquals(it, TimerRoute(it.id, 1, 2).speechType)
        }
    }

    @Test
    fun resolvedType_fallsBackToCustomForAnUnknownId() {
        assertEquals(SpeechType.CUSTOM, TimerRoute("renamed_in_a_later_version", 2, 3).speechType)
        assertEquals(SpeechType.CUSTOM, TimerRoute("", 2, 3).speechType)
    }

    @Test
    fun title_isBuiltFromTheTypeAndRange() {
        assertEquals("SPEECH (5 TO 7 MIN)", TimerRoute(SpeechType.SPEECH.id, 5, 7).title)
        assertEquals("Custom Speech (3 - 8 min)", TimerRoute(SpeechType.CUSTOM.id, 3, 8).title)
        assertEquals("Club (3 - 8 min)", TimerRoute(SpeechType.CUSTOM.id, 3, 8, "Club").title)
    }
}
