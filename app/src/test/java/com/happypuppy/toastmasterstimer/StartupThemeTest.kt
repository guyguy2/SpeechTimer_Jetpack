package com.happypuppy.toastmasterstimer

import com.happypuppy.toastmasterstimer.data.preferences.AppTheme
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class StartupThemeTest {

    @Test
    fun initiallyUnresolved() {
        val startup = StartupTheme()
        assertFalse(startup.isResolved)
        assertNull(startup.theme.value)
    }

    @Test
    fun resolvesOnFirstEmission() = runTest(StandardTestDispatcher()) {
        val startup = StartupTheme()
        val job = launch { startup.collectFrom(flowOf(AppTheme.DARK)) }
        runCurrent()

        assertTrue(startup.isResolved)
        assertEquals(AppTheme.DARK, startup.theme.value)
        job.cancel()
    }

    @Test
    fun fallsBackToSystemDefaultAfterTimeout_andLaterEmissionUpdates() = runTest(StandardTestDispatcher()) {
        val source = MutableSharedFlow<AppTheme>()
        val startup = StartupTheme()
        val job = launch { startup.collectFrom(source) }

        advanceTimeBy(StartupTheme.FIRST_READ_TIMEOUT_MS - 1L)
        runCurrent()
        assertFalse(startup.isResolved)

        advanceTimeBy(1L)
        runCurrent()
        assertTrue(startup.isResolved)
        assertEquals(AppTheme.SYSTEM_DEFAULT, startup.theme.value)

        source.emit(AppTheme.LIGHT)
        runCurrent()
        assertEquals(AppTheme.LIGHT, startup.theme.value)
        job.cancel()
    }

    @Test
    fun ioExceptionMappedToDefault_resolvesWithoutWaiting() = runTest(StandardTestDispatcher()) {
        val startup = StartupTheme()
        val failingFlow = flow<AppTheme> { throw IOException("disk error") }
            .catch { e -> if (e is IOException) emit(AppTheme.SYSTEM_DEFAULT) else throw e }

        val job = launch { startup.collectFrom(failingFlow) }
        runCurrent()

        assertTrue(startup.isResolved)
        assertEquals(AppTheme.SYSTEM_DEFAULT, startup.theme.value)
        job.cancel()
    }
}
