package com.happypuppy.toastmasterstimer.ui.screens

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.happypuppy.toastmasterstimer.util.TimerPhase
import com.happypuppy.toastmasterstimer.util.TimerThresholds
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class TimerUiState(
    val elapsedSeconds: Int = 0,
    val isRunning: Boolean = false,
    val phase: TimerPhase = TimerPhase.BEFORE_GREEN,
    val isUiVisible: Boolean = true,
    val thresholds: TimerThresholds = TimerThresholds(minMinutes = 1, maxMinutes = 2)
)

/**
 * Holds timer state across configuration changes and tracks elapsed time from a
 * monotonic clock so the timer does not drift, preserves sub-second progress across
 * pause/resume, and survives Doze/background delays.
 */
class TimerViewModel(
    private val elapsedRealtimeMs: () -> Long = { SystemClock.elapsedRealtime() }
) : ViewModel() {

    private val _uiState = MutableStateFlow(TimerUiState())
    val uiState: StateFlow<TimerUiState> = _uiState.asStateFlow()

    private val _phaseAlerts = MutableSharedFlow<TimerPhase>(extraBufferCapacity = 4)
    val phaseAlerts: SharedFlow<TimerPhase> = _phaseAlerts.asSharedFlow()

    private val _catchUpCues = MutableSharedFlow<TimerPhase>(extraBufferCapacity = 1)

    /**
     * One-shot cue emitted when the app returns to the foreground and the timer moved into a later
     * phase while the app was away, so a cue that was missed with the screen off is repeated once.
     */
    val catchUpCues: SharedFlow<TimerPhase> = _catchUpCues.asSharedFlow()

    private var isConfigured = false
    private var accumulatedMs: Long = 0L
    private var startedAtMs: Long? = null
    private var lastAlertedPhase: TimerPhase = TimerPhase.BEFORE_GREEN
    private var tickJob: Job? = null
    private var phaseWhenLeft: TimerPhase? = null
    private var lastLiveAlertAtMs: Long? = null

    fun configure(minMinutes: Int, maxMinutes: Int) {
        val safeMin = minMinutes.coerceAtLeast(0)
        val safeMax = maxMinutes.coerceAtLeast(safeMin)
        val newThresholds = TimerThresholds(minMinutes = safeMin, maxMinutes = safeMax)
        if (isConfigured && _uiState.value.thresholds == newThresholds) {
            return
        }
        isConfigured = true
        _uiState.update { it.copy(thresholds = newThresholds) }
        reset()
    }

    fun start() {
        if (_uiState.value.isRunning) return
        startedAtMs = elapsedRealtimeMs()
        _uiState.update { it.copy(isRunning = true) }
        syncElapsed()

        tickJob?.cancel()
        tickJob = viewModelScope.launch {
            while (isActive) {
                val msUntilNextSecond = 1000L - (currentElapsedMs() % 1000L)
                delay(msUntilNextSecond.coerceIn(1L, 1000L))
                syncElapsed()
            }
        }
    }

    fun pause() {
        if (!_uiState.value.isRunning) return
        accumulatedMs = currentElapsedMs()
        startedAtMs = null
        tickJob?.cancel()
        tickJob = null
        _uiState.update { it.copy(isRunning = false) }
        syncElapsed()
    }

    fun toggleRunning() {
        if (_uiState.value.isRunning) pause() else start()
    }

    fun reset() {
        tickJob?.cancel()
        tickJob = null
        accumulatedMs = 0L
        startedAtMs = null
        lastAlertedPhase = TimerPhase.BEFORE_GREEN
        phaseWhenLeft = null
        lastLiveAlertAtMs = null
        _uiState.update {
            it.copy(
                elapsedSeconds = 0,
                isRunning = false,
                phase = TimerPhase.BEFORE_GREEN
            )
        }
    }

    fun toggleUiVisibility() {
        _uiState.update { it.copy(isUiVisible = !it.isUiVisible) }
    }

    /** Called when the timer screen stops being visible (home, screen off, another app). */
    fun onAppBackgrounded() {
        if (phaseWhenLeft == null) phaseWhenLeft = _uiState.value.phase
    }

    /** Called when the timer screen is visible again; emits a catch-up cue if a phase was crossed. */
    fun onAppForegrounded() {
        val left = phaseWhenLeft ?: return
        phaseWhenLeft = null
        syncElapsed()
        val now = _uiState.value.phase
        // A live alert for this phase fired a moment ago (a frozen process thawing on return, or this
        // sync itself): the user is getting that cue now, so do not repeat it.
        val liveAlertAt = lastLiveAlertAtMs
        val justAlerted = lastAlertedPhase == now && liveAlertAt != null &&
            elapsedRealtimeMs() - liveAlertAt < RECENT_ALERT_WINDOW_MS
        if (!justAlerted && now.ordinal > left.ordinal) _catchUpCues.tryEmit(now)
    }

    fun syncElapsed() {
        val elapsedSec = (currentElapsedMs() / 1000L).toInt()
        val newPhase = _uiState.value.thresholds.phaseAt(elapsedSec)

        if (newPhase != lastAlertedPhase && newPhase != TimerPhase.BEFORE_GREEN) {
            lastAlertedPhase = newPhase
            lastLiveAlertAtMs = elapsedRealtimeMs()
            _phaseAlerts.tryEmit(newPhase)
        }

        _uiState.update {
            it.copy(
                elapsedSeconds = elapsedSec,
                phase = newPhase
            )
        }
    }

    private fun currentElapsedMs(): Long {
        val start = startedAtMs
        return if (start != null) {
            accumulatedMs + (elapsedRealtimeMs() - start).coerceAtLeast(0L)
        } else {
            accumulatedMs
        }
    }

    private companion object {
        const val RECENT_ALERT_WINDOW_MS = 2_000L
    }
}
