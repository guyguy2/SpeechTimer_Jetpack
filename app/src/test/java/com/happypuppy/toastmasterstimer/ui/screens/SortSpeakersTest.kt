package com.happypuppy.toastmasterstimer.ui.screens

import com.happypuppy.toastmasterstimer.data.database.repository.Speaker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

/** Complements the sorting assertions in SpeechTimerViewModelTest with empty/tie/immutability cases. */
class SortSpeakersTest {

    private fun speaker(id: Int, name: String, type: String = "SPEECH", at: Long = id * 1_000L) =
        Speaker(id = id, name = name, speechType = type, duration = "01:00", createdAt = Date(at))

    @Test
    fun emptyList_isEmptyForEveryOption() {
        SortOption.entries.forEach { option ->
            assertTrue("$option", sortSpeakers(emptyList(), option).isEmpty())
        }
    }

    @Test
    fun caseInsensitiveNameTies_keepOriginalOrder() {
        val input = listOf(speaker(1, "bob"), speaker(2, "Bob"), speaker(3, "BOB"))

        assertEquals(listOf(1, 2, 3), sortSpeakers(input, SortOption.NAME_ASC).map { it.id })
        assertEquals(listOf(1, 2, 3), sortSpeakers(input, SortOption.NAME_DESC).map { it.id })
    }

    @Test
    fun sameTimestamp_keepsOriginalOrder() {
        val input = listOf(speaker(1, "A", at = 5), speaker(2, "B", at = 5))

        assertEquals(listOf(1, 2), sortSpeakers(input, SortOption.DATE_NEWEST).map { it.id })
        assertEquals(listOf(1, 2), sortSpeakers(input, SortOption.DATE_OLDEST).map { it.id })
    }

    @Test
    fun sorting_doesNotMutateTheInput() {
        val input = listOf(speaker(1, "Zed"), speaker(2, "Amy"))
        val snapshot = input.toList()

        sortSpeakers(input, SortOption.NAME_ASC)

        assertEquals(snapshot, input)
    }

    @Test
    fun everyOption_hasADistinctNonBlankDisplayName() {
        val names = SortOption.entries.map { it.displayName }
        assertTrue(names.all { it.isNotBlank() })
        assertEquals(names.size, names.toSet().size)
    }
}
