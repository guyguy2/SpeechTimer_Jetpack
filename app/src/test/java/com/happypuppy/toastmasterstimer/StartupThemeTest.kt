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
        assertNull(startup.state.value)
    }

    @Test
    fun resolvesOnFirstCombinedEmission() = runTest(StandardTestDispatcher()) {
        val startup = StartupTheme()
        val job = launch {
            startup.collectFrom(
                themeSource = flowOf(AppTheme.DARK),
                dynamicColorSource = flowOf(true)
            )
        }
        runCurrent()

        assertTrue(startup.isResolved)
        assertEquals(
            StartupThemeState(theme = AppTheme.DARK, dynamicColorEnabled = true),
            startup.state.value
        )
        job.cancel()
    }

    @Test
    fun fallsBackToDefaultsAfterTimeout_andLaterEmissionsUpdate() = runTest(StandardTestDispatcher()) {
        val themeSource = MutableSharedFlow<AppTheme>()
        val dynamicSource = MutableSharedFlow<Boolean>()
        val startup = StartupTheme()
        val job = launch { startup.collectFrom(themeSource, dynamicSource) }

        advanceTimeBy(StartupTheme.FIRST_READ_TIMEOUT_MS - 1L)
        runCurrent()
        assertFalse(startup.isResolved)

        advanceTimeBy(1L)
        runCurrent()
        assertTrue(startup.isResolved)
        assertEquals(
            StartupThemeState(theme = AppTheme.SYSTEM_DEFAULT, dynamicColorEnabled = false),
            startup.state.value
        )

        themeSource.emit(AppTheme.LIGHT)
        dynamicSource.emit(true)
        runCurrent()
        assertEquals(
            StartupThemeState(theme = AppTheme.LIGHT, dynamicColorEnabled = true),
            startup.state.value
        )
        job.cancel()
    }

    @Test
    fun stallsIfOnlyOneSourceEmits_thenFallsBackOnTimeout() = runTest(StandardTestDispatcher()) {
        val themeSource = MutableSharedFlow<AppTheme>()
        val dynamicSource = MutableSharedFlow<Boolean>()
        val startup = StartupTheme()
        val job = launch { startup.collectFrom(themeSource, dynamicSource) }
        runCurrent()

        themeSource.emit(AppTheme.DARK)
        runCurrent()
        assertFalse(startup.isResolved)

        advanceTimeBy(StartupTheme.FIRST_READ_TIMEOUT_MS)
        runCurrent()
        assertTrue(startup.isResolved)
        assertEquals(StartupThemeState(), startup.state.value)

        dynamicSource.emit(true)
        runCurrent()
        assertEquals(
            StartupThemeState(theme = AppTheme.DARK, dynamicColorEnabled = true),
            startup.state.value
        )
        job.cancel()
    }

    @Test
    fun ioExceptionMappedToDefaults_resolvesWithoutWaiting() = runTest(StandardTestDispatcher()) {
        val startup = StartupTheme()
        val failingThemeFlow = flow<AppTheme> { throw IOException("disk error") }
            .catch { e -> if (e is IOException) emit(AppTheme.SYSTEM_DEFAULT) else throw e }
        val failingDynamicFlow = flow<Boolean> { throw IOException("disk error") }
            .catch { e -> if (e is IOException) emit(false) else throw e }

        val job = launch { startup.collectFrom(failingThemeFlow, failingDynamicFlow) }
        runCurrent()

        assertTrue(startup.isResolved)
        assertEquals(
            StartupThemeState(theme = AppTheme.SYSTEM_DEFAULT, dynamicColorEnabled = false),
            startup.state.value
        )
        job.cancel()
    }
}
