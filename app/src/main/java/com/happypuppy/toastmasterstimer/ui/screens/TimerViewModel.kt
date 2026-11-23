package com.happypuppy.toastmasterstimer.ui.screens

import android.os.SystemClock
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.happypuppy.toastmasterstimer.util.TimerPhase
import com.happypuppy.toastmasterstimer.util.TimerThresholds
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class TimerUiState(
    val elapsedSeconds: Int = 0,
    val isRunning: Boolean = false,
    val phase: TimerPhase = TimerPhase.BEFORE_GREEN,
    val isUiVisible: Boolean = true,
    val thresholds: TimerThresholds = TimerThresholds(minMinutes = 1, maxMinutes = 2),
    /** True when this state was rebuilt after process death and the user has not acted on it yet. */
    val isRestored: Boolean = false
)

/**
 * Holds timer state across configuration changes and tracks elapsed time from a
 * monotonic clock so the timer does not drift, preserves sub-second progress across
 * pause/resume, and survives Doze/background delays. Clock state is mirrored into
 * [savedStateHandle] so the timer is rebuilt after process death (not after a reboot or swipe-away).
 */
class TimerViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val elapsedRealtimeMs: () -> Long = { SystemClock.elapsedRealtime() }
) : ViewModel() {

    private val _uiState = MutableStateFlow(TimerUiState())
    val uiState: StateFlow<TimerUiState> = _uiState.asStateFlow()

    private val _phaseAlerts = MutableSharedFlow<TimerPhase>(extraBufferCapacity = 4)
    val phaseAlerts: SharedFlow<TimerPhase> = _phaseAlerts.asSharedFlow()

    // A channel (not a SharedFlow) so a cue raised while restoring waits for the screen to collect it.
    private val _catchUpCues = Channel<TimerPhase>(Channel.CONFLATED)

    /**
     * One-shot cue emitted when the app returns to the foreground and the timer moved into a later
     * phase while the app was away, so a cue that was missed with the screen off is repeated once.
     */
    val catchUpCues: Flow<TimerPhase> = _catchUpCues.receiveAsFlow()

    private var isConfigured = false
    private var accumulatedMs: Long = 0L
    private var startedAtMs: Long? = null
    private var lastAlertedPhase: TimerPhase = TimerPhase.BEFORE_GREEN
    private var tickJob: Job? = null
    private var phaseWhenLeft: TimerPhase? = null
    private var lastLiveAlertAtMs: Long? = null

    init {
        restoreFromSavedState()
    }

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
        _uiState.update { it.copy(isRunning = true, isRestored = false) }
        syncElapsed()
        persist()
        launchTickLoop()
    }

    private fun launchTickLoop() {
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
        _uiState.update { it.copy(isRunning = false, isRestored = false) }
        syncElapsed()
        persist()
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
                phase = TimerPhase.BEFORE_GREEN,
                isRestored = false
            )
        }
        persist()
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
        if (!justAlerted && now.ordinal > left.ordinal) _catchUpCues.trySend(now)
    }

    fun syncElapsed() {
        val elapsedSec = (currentElapsedMs() / 1000L).toInt()
        val newPhase = _uiState.value.thresholds.phaseAt(elapsedSec)

        if (newPhase != lastAlertedPhase && newPhase != TimerPhase.BEFORE_GREEN) {
            lastAlertedPhase = newPhase
            lastLiveAlertAtMs = elapsedRealtimeMs()
            _phaseAlerts.tryEmit(newPhase)
        }

        val phaseChanged = newPhase != _uiState.value.phase
        _uiState.update {
            it.copy(
                elapsedSeconds = elapsedSec,
                phase = newPhase
            )
        }
        if (phaseChanged) persist()
    }

    private fun persist() {
        savedStateHandle[KEY_MIN_MINUTES] = _uiState.value.thresholds.minMinutes
        savedStateHandle[KEY_MAX_MINUTES] = _uiState.value.thresholds.maxMinutes
        savedStateHandle[KEY_ACCUMULATED_MS] = accumulatedMs
        savedStateHandle[KEY_STARTED_AT_MS] = startedAtMs ?: NOT_RUNNING
        savedStateHandle[KEY_PHASE] = _uiState.value.phase.name
    }

    private fun restoreFromSavedState() {
        val minMinutes = savedStateHandle.get<Int>(KEY_MIN_MINUTES) ?: return
        val maxMinutes = savedStateHandle.get<Int>(KEY_MAX_MINUTES) ?: return
        val accumulated = (savedStateHandle.get<Long>(KEY_ACCUMULATED_MS) ?: 0L).coerceAtLeast(0L)
        // elapsedRealtime restarts at a reboot, so a saved start in the future means the clock is stale.
        val startedAt = savedStateHandle.get<Long>(KEY_STARTED_AT_MS)
            ?.takeIf { it >= 0L && it <= elapsedRealtimeMs() }
        if (accumulated == 0L && startedAt == null) return

        val thresholds = TimerThresholds(minMinutes = minMinutes, maxMinutes = maxMinutes)
        isConfigured = true
        accumulatedMs = accumulated
        startedAtMs = startedAt
        phaseWhenLeft = savedStateHandle.get<String>(KEY_PHASE)
            ?.let { name -> TimerPhase.entries.firstOrNull { it.name == name } }
        val elapsedSec = (currentElapsedMs() / 1000L).toInt()
        val phase = thresholds.phaseAt(elapsedSec)
        // The catch-up cue (not a live alert) reports any phase crossed while the process was gone.
        lastAlertedPhase = phase
        _uiState.value = TimerUiState(
            elapsedSeconds = elapsedSec,
            isRunning = startedAt != null,
            phase = phase,
            thresholds = thresholds,
            isRestored = true
        )
        if (startedAt != null) launchTickLoop()
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
        const val KEY_MIN_MINUTES = "timer_min_minutes"
        const val KEY_MAX_MINUTES = "timer_max_minutes"
        const val KEY_ACCUMULATED_MS = "timer_accumulated_ms"
        const val KEY_STARTED_AT_MS = "timer_started_at_ms"
        const val KEY_PHASE = "timer_phase"
        const val NOT_RUNNING = -1L
        const val RECENT_ALERT_WINDOW_MS = 2_000L
    }
}
