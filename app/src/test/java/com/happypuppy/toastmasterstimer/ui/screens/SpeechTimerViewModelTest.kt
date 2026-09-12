package com.happypuppy.toastmasterstimer.ui.screens

import com.happypuppy.toastmasterstimer.MainDispatcherRule
import com.happypuppy.toastmasterstimer.data.database.FakeSpeakerDao
import com.happypuppy.toastmasterstimer.data.database.repository.Speaker
import com.happypuppy.toastmasterstimer.data.database.repository.SpeakerRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class SpeechTimerViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun speaker(id: Int, name: String) =
        Speaker(id = id, name = name, speechType = "SPEECH", duration = "05:30")

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
    fun deleteSpeaker_removesOnlyThatRow() = runTest {
        val ann = speaker(1, "Ann")
        val dao = FakeSpeakerDao(listOf(ann, speaker(2, "Bob")))
        val vm = viewModel(dao)

        vm.deleteSpeaker(ann)

        assertEquals(listOf("Bob"), vm.uiState.value.speakers.map { it.name })
    }

    @Test
    fun deleteAllSpeakers_emptiesList() = runTest {
        val dao = FakeSpeakerDao(listOf(speaker(1, "Ann"), speaker(2, "Bob")))
        val vm = viewModel(dao)

        vm.deleteAllSpeakers()

        assertTrue(vm.uiState.value.speakers.isEmpty())
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
    fun error_isClearedByNextSuccessfulLoad() = runTest {
        val dao = FakeSpeakerDao().apply { insertError = IOException("readonly database") }
        val vm = viewModel(dao)
        vm.saveSpeaker(name = "Dan", speechType = "SPEECH", duration = "06:00")
        assertEquals("readonly database", vm.uiState.value.error)

        vm.saveSpeaker(name = "Eve", speechType = "SPEECH", duration = "06:30")

        assertNull(vm.uiState.value.error)
        assertEquals(listOf("Eve"), vm.uiState.value.speakers.map { it.name })
    }
}
