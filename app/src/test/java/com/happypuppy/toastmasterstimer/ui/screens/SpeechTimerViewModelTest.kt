package com.happypuppy.toastmasterstimer.ui.screens

import com.happypuppy.toastmasterstimer.MainDispatcherRule
import com.happypuppy.toastmasterstimer.data.database.FakeSpeakerDao
import com.happypuppy.toastmasterstimer.data.database.repository.Speaker
import com.happypuppy.toastmasterstimer.data.database.repository.SpeakerRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
class SpeechTimerViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun speaker(
        id: Int,
        name: String,
        speechType: String = "SPEECH",
        createdAt: Date = Date(id * 1_000L)
    ) = Speaker(
        id = id,
        name = name,
        speechType = speechType,
        duration = "05:30",
        createdAt = createdAt
    )

    private fun viewModel(dao: FakeSpeakerDao) = SpeechTimerViewModel(SpeakerRepository(dao))

    @Test
    fun initialState_exposesStoredSpeakersAndStopsLoading() = runTest {
        val dao = FakeSpeakerDao(listOf(speaker(1, "Ann"), speaker(2, "Bob")))

        val state = viewModel(dao).uiState.value

        assertEquals(listOf("Ann", "Bob"), state.speakers.map { it.name })
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun saveSpeaker_insertsRowAndListUpdatesWithoutManualRefresh() = runTest {
        val dao = FakeSpeakerDao()
        val vm = viewModel(dao)

        vm.saveSpeaker(name = "Cara", speechType = "TABLE TOPICS", duration = "01:45")

        assertEquals(1, dao.current.size)
        val saved = vm.uiState.value.speakers.single()
        assertEquals("Cara", saved.name)
        assertEquals("TABLE TOPICS", saved.speechType)
        assertEquals("01:45", saved.duration)
        assertTrue("id should be auto-generated", saved.id > 0)
    }

    @Test
    fun saveSpeaker_ignoresBlankNameAndTrimsWhitespace() = runTest {
        val dao = FakeSpeakerDao()
        val vm = viewModel(dao)

        vm.saveSpeaker(name = "   ", speechType = "SPEECH", duration = "05:00")
        assertTrue(dao.current.isEmpty())

        vm.saveSpeaker(name = "  Alice  ", speechType = "SPEECH", duration = "05:00")
        assertEquals("Alice", dao.current.single().name)
    }

    @Test
    fun deleteSpeaker_removesOnlyThatRowAndEmitsEvent() = runTest {
        val ann = speaker(1, "Ann")
        val dao = FakeSpeakerDao(listOf(ann, speaker(2, "Bob")))
        val vm = viewModel(dao)

        val events = mutableListOf<SpeakerEvent>()
        val collectJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.events.collect { events.add(it) }
        }

        vm.deleteSpeaker(ann)

        assertEquals(listOf("Bob"), vm.uiState.value.speakers.map { it.name })
        assertEquals(listOf(SpeakerEvent.Deleted("Ann")), events)
        collectJob.cancel()
    }

    @Test
    fun deleteSpeaker_failureSurfacesErrorWithoutEmittingDeletedEvent() = runTest {
        val ann = speaker(1, "Ann")
        val dao = FakeSpeakerDao(listOf(ann)).apply { deleteError = IOException("locked") }
        val vm = viewModel(dao)

        val events = mutableListOf<SpeakerEvent>()
        val collectJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.events.collect { events.add(it) }
        }

        vm.deleteSpeaker(ann)

        assertEquals("locked", vm.uiState.value.error)
        assertEquals(listOf("Ann"), vm.uiState.value.speakers.map { it.name })
        assertTrue(events.isEmpty())
        collectJob.cancel()
    }

    @Test
    fun deleteAllSpeakers_emptiesListAndEmitsEvent() = runTest {
        val dao = FakeSpeakerDao(listOf(speaker(1, "Ann"), speaker(2, "Bob")))
        val vm = viewModel(dao)

        val events = mutableListOf<SpeakerEvent>()
        val collectJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.events.collect { events.add(it) }
        }

        vm.deleteAllSpeakers()

        assertTrue(vm.uiState.value.speakers.isEmpty())
        assertEquals(listOf(SpeakerEvent.AllDeleted), events)
        collectJob.cancel()
    }

    @Test
    fun deleteAllSpeakers_failureSurfacesErrorWithoutEmittingEvent() = runTest {
        val dao = FakeSpeakerDao(listOf(speaker(1, "Ann"))).apply {
            deleteAllError = IOException("disk error")
        }
        val vm = viewModel(dao)

        val events = mutableListOf<SpeakerEvent>()
        val collectJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.events.collect { events.add(it) }
        }

        vm.deleteAllSpeakers()

        assertEquals("disk error", vm.uiState.value.error)
        assertTrue(events.isEmpty())
        collectJob.cancel()
    }

    @Test
    fun queryFailure_surfacesErrorAndStopsLoading() = runTest {
        val dao = FakeSpeakerDao().apply { queryError = IOException("disk full") }

        val state = viewModel(dao).uiState.value

        assertEquals("disk full", state.error)
        assertFalse(state.isLoading)
        assertTrue(state.speakers.isEmpty())
    }

    @Test
    fun insertFailure_surfacesErrorInsteadOfCrashing() = runTest {
        val dao = FakeSpeakerDao().apply { insertError = IOException("readonly database") }
        val vm = viewModel(dao)

        vm.saveSpeaker(name = "Dan", speechType = "SPEECH", duration = "06:00")

        assertEquals("readonly database", vm.uiState.value.error)
    }

    @Test
    fun clearError_clearsExistingError() = runTest {
        val dao = FakeSpeakerDao().apply { insertError = IOException("readonly database") }
        val vm = viewModel(dao)
        vm.saveSpeaker(name = "Dan", speechType = "SPEECH", duration = "06:00")
        assertEquals("readonly database", vm.uiState.value.error)

        vm.clearError()

        assertNull(vm.uiState.value.error)
    }

    @Test
    fun error_isClearedByNextSuccessfulLoad() = runTest {
        val dao = FakeSpeakerDao().apply { insertError = IOException("readonly database") }
        val vm = viewModel(dao)
        vm.saveSpeaker(name = "Dan", speechType = "SPEECH", duration = "06:00")
        assertEquals("readonly database", vm.uiState.value.error)

        vm.saveSpeaker(name = "Eve", speechType = "SPEECH", duration = "06:30")

        assertNull(vm.uiState.value.error)
        assertEquals(listOf("Eve"), vm.uiState.value.speakers.map { it.name })
    }

    @Test
    fun sortSpeakers_sortsCaseInsensitivelyAndByDate() {
        val speakers = listOf(
            speaker(1, "charlie", speechType = "table topics", createdAt = Date(2_000L)),
            speaker(2, "Alice", speechType = "Evaluation", createdAt = Date(3_000L)),
            speaker(3, "bob", speechType = "SPEECH", createdAt = Date(1_000L))
        )

        assertEquals(
            listOf("Alice", "bob", "charlie"),
            sortSpeakers(speakers, SortOption.NAME_ASC).map { it.name }
        )
        assertEquals(
            listOf("charlie", "bob", "Alice"),
            sortSpeakers(speakers, SortOption.NAME_DESC).map { it.name }
        )
        assertEquals(
            listOf("Evaluation", "SPEECH", "table topics"),
            sortSpeakers(speakers, SortOption.TYPE_ASC).map { it.speechType }
        )
        assertEquals(
            listOf("table topics", "SPEECH", "Evaluation"),
            sortSpeakers(speakers, SortOption.TYPE_DESC).map { it.speechType }
        )
        assertEquals(
            listOf("Alice", "charlie", "bob"),
            sortSpeakers(speakers, SortOption.DATE_NEWEST).map { it.name }
        )
        assertEquals(
            listOf("bob", "charlie", "Alice"),
            sortSpeakers(speakers, SortOption.DATE_OLDEST).map { it.name }
        )
    }
}
