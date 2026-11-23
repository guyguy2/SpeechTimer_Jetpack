package com.happypuppy.toastmasterstimer.ui.screens

import com.happypuppy.toastmasterstimer.MainDispatcherRule
import com.happypuppy.toastmasterstimer.data.database.FakeSpeakerDao
import com.happypuppy.toastmasterstimer.data.database.repository.Speaker
import com.happypuppy.toastmasterstimer.data.database.repository.SpeakerRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.Date

/**
 * Failure-mode and fallback behaviour of [SpeechTimerViewModel] that the main test does not
 * cover: null error messages, cancellation, retry after failure, and stored field fidelity.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SpeechTimerViewModelFailureModesTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun viewModel(dao: FakeSpeakerDao) = SpeechTimerViewModel(SpeakerRepository(dao))

    private fun speaker(id: Int, name: String) = Speaker(
        id = id,
        name = name,
        speechType = "SPEECH",
        duration = "05:30",
        createdAt = Date(id * 1_000L)
    )

    @Test
    fun queryFailureWithoutMessage_usesLoadFallbackText() = runTest {
        val dao = FakeSpeakerDao().apply { queryError = RuntimeException() }

        val state = viewModel(dao).uiState.value

        assertEquals("Failed to load speeches", state.error)
        assertTrue(state.speakers.isEmpty())
    }

    @Test
    fun insertFailureWithoutMessage_usesDatabaseFallbackText() = runTest {
        val dao = FakeSpeakerDao().apply { insertError = RuntimeException() }
        val vm = viewModel(dao)

        vm.saveSpeaker("Dana", "SPEECH", "05:30")

        assertEquals("Database operation failed", vm.uiState.value.error)
        assertTrue(dao.current.isEmpty())
    }

    @Test
    fun cancellationDuringWrite_isNotSurfacedAsAnErrorOrEvent() = runTest {
        val dao = FakeSpeakerDao(listOf(speaker(1, "Ann")))
        dao.deleteError = CancellationException("scope cancelled")
        val vm = viewModel(dao)
        val events = mutableListOf<SpeakerEvent>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.events.collect { events.add(it) }
        }

        vm.deleteSpeaker(vm.uiState.value.speakers.single())

        assertNull(vm.uiState.value.error)
        assertTrue(events.isEmpty())
        assertEquals(1, dao.current.size)
        job.cancel()
    }

    @Test
    fun successfulRetryAfterFailedInsert_clearsTheErrorViaTheListEmission() = runTest {
        val dao = FakeSpeakerDao().apply { insertError = java.io.IOException("disk full") }
        val vm = viewModel(dao)

        vm.saveSpeaker("Eli", "SPEECH", "05:30")
        assertEquals("disk full", vm.uiState.value.error)

        vm.saveSpeaker("Eli", "SPEECH", "05:30")

        assertEquals(listOf("Eli"), vm.uiState.value.speakers.map { it.name })
        // Characterization: the next successful list emission supersedes the earlier error.
        assertNull(vm.uiState.value.error)
    }

    @Test
    fun saveSpeaker_storesTypeAndDurationVerbatimWithFreshTimestamp() = runTest {
        val dao = FakeSpeakerDao()
        val vm = viewModel(dao)
        val before = System.currentTimeMillis()

        vm.saveSpeaker("  Fay  ", "ICE BREAKER", "04:59")

        val saved = dao.current.single()
        assertEquals("Fay", saved.name)
        assertEquals("ICE BREAKER", saved.speechType)
        assertEquals("04:59", saved.duration)
        assertNotNull(saved.createdAt)
        assertTrue(saved.createdAt.time >= before)
        assertTrue(saved.id > 0)
    }

    @Test
    fun whitespaceOnlyName_neverReachesTheDao() = runTest {
        val dao = FakeSpeakerDao()
        val vm = viewModel(dao)

        vm.saveSpeaker("   \t ", "SPEECH", "05:30")

        assertTrue(dao.current.isEmpty())
        assertNull(vm.uiState.value.error)
    }

    @Test
    fun deleteAll_thenSave_listRecoversWithoutStaleRows() = runTest {
        val dao = FakeSpeakerDao(listOf(speaker(1, "Ann"), speaker(2, "Bob")))
        val vm = viewModel(dao)

        vm.deleteAllSpeakers()
        assertTrue(vm.uiState.value.speakers.isEmpty())

        vm.saveSpeaker("Cy", "SPEECH", "05:30")
        assertEquals(listOf("Cy"), vm.uiState.value.speakers.map { it.name })
    }

    @Test
    fun clearError_whenNoErrorIsHarmless() = runTest {
        val vm = viewModel(FakeSpeakerDao())

        vm.clearError()
        vm.clearError()

        assertNull(vm.uiState.value.error)
    }
}
