package com.happypuppy.toastmasterstimer.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.happypuppy.toastmasterstimer.data.database.repository.Speaker
import com.happypuppy.toastmasterstimer.data.database.repository.SpeakerRepository
import com.happypuppy.toastmasterstimer.data.database.repository.UNKNOWN_SPEECH_TYPE_ID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class MainScreenUiState(
    val speakers: List<Speaker> = emptyList(),
    val isLoading: Boolean = false,
    val error: SpeakerError? = null,
    val restoreVersions: Map<Int, Int> = emptyMap(),
    val canUndo: Boolean = false
)

sealed interface SpeakerEvent {
    data class Deleted(val speaker: Speaker) : SpeakerEvent {
        val speakerName: String get() = speaker.name
    }
    data object AllDeleted : SpeakerEvent
}

class SpeechTimerViewModel(private val repository: SpeakerRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(MainScreenUiState(isLoading = true))
    val uiState: StateFlow<MainScreenUiState> = _uiState.asStateFlow()

    private val _events = Channel<SpeakerEvent>(Channel.BUFFERED)
    val events: Flow<SpeakerEvent> = _events.receiveAsFlow()

    private val _rawSpeakers = MutableStateFlow<List<Speaker>?>(null)
    private val _error = MutableStateFlow<SpeakerError?>(null)
    private val _undoOverlay = MutableStateFlow(UndoUiOverlay())

    private val undoStack = ArrayDeque<UndoEntry>()
    private val dbMutex = Mutex()

    init {
        observeSpeakers()
        observeCombinedState()
    }

    private fun observeSpeakers() {
        viewModelScope.launch {
            repository.getAllSpeakers()
                .catch {
                    _error.value = SpeakerError.LoadFailed
                    _uiState.update { it.copy(error = SpeakerError.LoadFailed, isLoading = false) }
                }
                .collect { speakers ->
                    val presentIds = speakers.mapTo(mutableSetOf()) { it.id }
                    _undoOverlay.update { overlay ->
                        val pruned = overlay.pendingDeletedIds.intersect(presentIds)
                        if (pruned == overlay.pendingDeletedIds) {
                            overlay
                        } else {
                            overlay.copy(pendingDeletedIds = pruned)
                        }
                    }
                    // A successful load supersedes any earlier error.
                    _error.value = null
                    _rawSpeakers.value = speakers
                }
        }
    }

    private fun observeCombinedState() {
        viewModelScope.launch {
            combine(_rawSpeakers, _undoOverlay, _error) { rawSpeakers, overlay, error ->
                if (rawSpeakers == null) {
                    if (error != null) {
                        MainScreenUiState(
                            speakers = emptyList(),
                            isLoading = false,
                            error = error,
                            restoreVersions = overlay.restoreVersions,
                            canUndo = overlay.canUndo
                        )
                    } else {
                        null
                    }
                } else {
                    MainScreenUiState(
                        speakers = rawSpeakers.filterNot { it.id in overlay.pendingDeletedIds },
                        isLoading = false,
                        error = error,
                        restoreVersions = overlay.restoreVersions,
                        canUndo = overlay.canUndo
                    )
                }
            }.collect { state ->
                if (state != null) {
                    _uiState.value = state
                }
            }
        }
    }

    fun saveSpeaker(
        name: String,
        speechType: String,
        duration: String,
        speechTypeId: String = UNKNOWN_SPEECH_TYPE_ID,
        minSeconds: Int? = null,
        maxSeconds: Int? = null,
        elapsedSeconds: Int? = null,
        phaseReached: String? = null
    ) {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) return
        runMutation(SpeakerError.SaveFailed) {
            repository.insertSpeaker(
                Speaker(
                    name = trimmedName,
                    speechType = speechType,
                    duration = duration,
                    speechTypeId = speechTypeId,
                    minSeconds = minSeconds,
                    maxSeconds = maxSeconds,
                    elapsedSeconds = elapsedSeconds,
                    phaseReached = phaseReached
                )
            )
        }
    }

    fun updateSpeakerName(speaker: Speaker, newName: String) {
        val trimmedName = newName.trim()
        if (trimmedName.isEmpty() || trimmedName == speaker.name) return
        runMutation(SpeakerError.UpdateFailed) {
            repository.updateSpeaker(speaker.copy(name = trimmedName))
        }
    }

    fun restoreSpeaker(speaker: Speaker) {
        viewModelScope.launch {
            dbMutex.withLock {
                try {
                    repository.insertSpeaker(speaker)
                    _undoOverlay.update { overlay ->
                        val nextVersion = (overlay.restoreVersions[speaker.id] ?: 0) + 1
                        overlay.copy(
                            pendingDeletedIds = overlay.pendingDeletedIds - speaker.id,
                            restoreVersions = overlay.restoreVersions + (speaker.id to nextVersion)
                        )
                    }
                    _error.value = null
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    _error.value = SpeakerError.RestoreFailed
                }
            }
        }
    }

    fun deleteSpeaker(speaker: Speaker) {
        if (speaker.id in _undoOverlay.value.pendingDeletedIds) return
        if (_rawSpeakers.value?.none { it.id == speaker.id } == true) return

        val entry = UndoEntry.Single(speaker)
        synchronized(undoStack) {
            undoStack.addLast(entry)
        }
        _undoOverlay.update { overlay ->
            overlay.copy(
                pendingDeletedIds = overlay.pendingDeletedIds + speaker.id,
                canUndo = true
            )
        }
        _error.value = null

        viewModelScope.launch {
            dbMutex.withLock {
                try {
                    repository.deleteSpeaker(speaker)
                    _events.trySend(SpeakerEvent.Deleted(speaker))
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    val stackNotEmpty = synchronized(undoStack) {
                        undoStack.remove(entry)
                        undoStack.isNotEmpty()
                    }
                    _undoOverlay.update { overlay ->
                        val nextVersion = (overlay.restoreVersions[speaker.id] ?: 0) + 1
                        overlay.copy(
                            pendingDeletedIds = overlay.pendingDeletedIds - speaker.id,
                            restoreVersions = overlay.restoreVersions + (speaker.id to nextVersion),
                            canUndo = stackNotEmpty
                        )
                    }
                    _error.value = SpeakerError.DeleteFailed
                }
            }
        }
    }

    fun deleteAllSpeakers() {
        val snapshot = _rawSpeakers.value
            ?.filterNot { it.id in _undoOverlay.value.pendingDeletedIds }
            .orEmpty()
        if (snapshot.isEmpty()) return

        val entry = UndoEntry.Bulk(snapshot)
        val snapshotIds = snapshot.mapTo(mutableSetOf()) { it.id }
        synchronized(undoStack) {
            undoStack.addLast(entry)
        }
        _undoOverlay.update { overlay ->
            overlay.copy(
                pendingDeletedIds = overlay.pendingDeletedIds + snapshotIds,
                canUndo = true
            )
        }
        _error.value = null

        viewModelScope.launch {
            dbMutex.withLock {
                try {
                    repository.deleteAllSpeakers()
                    _events.trySend(SpeakerEvent.AllDeleted)
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    val stackNotEmpty = synchronized(undoStack) {
                        undoStack.remove(entry)
                        undoStack.isNotEmpty()
                    }
                    _undoOverlay.update { overlay ->
                        overlay.copy(
                            pendingDeletedIds = overlay.pendingDeletedIds - snapshotIds,
                            canUndo = stackNotEmpty
                        )
                    }
                    _error.value = SpeakerError.DeleteAllFailed
                }
            }
        }
    }

    fun undoDelete() {
        if (synchronized(undoStack) { undoStack.isEmpty() }) return

        viewModelScope.launch {
            dbMutex.withLock {
                val entry = synchronized(undoStack) { undoStack.lastOrNull() } ?: return@withLock
                _error.value = null
                try {
                    val restoredIds = when (entry) {
                        is UndoEntry.Single -> {
                            repository.insertSpeaker(entry.speaker)
                            listOf(entry.speaker.id)
                        }
                        is UndoEntry.Bulk -> {
                            repository.insertSpeakers(entry.speakers)
                            entry.speakers.map { it.id }
                        }
                    }
                    val (stackNotEmpty, previous) = synchronized(undoStack) {
                        undoStack.remove(entry)
                        undoStack.isNotEmpty() to undoStack.lastOrNull()
                    }
                    _undoOverlay.update { overlay ->
                        val updatedVersions = overlay.restoreVersions.toMutableMap()
                        for (id in restoredIds) {
                            updatedVersions[id] = (updatedVersions[id] ?: 0) + 1
                        }
                        overlay.copy(
                            pendingDeletedIds = overlay.pendingDeletedIds - restoredIds.toSet(),
                            restoreVersions = updatedVersions,
                            canUndo = stackNotEmpty
                        )
                    }
                    _error.value = null
                    when (previous) {
                        is UndoEntry.Single -> _events.trySend(SpeakerEvent.Deleted(previous.speaker))
                        is UndoEntry.Bulk -> _events.trySend(SpeakerEvent.AllDeleted)
                        null -> Unit
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    _error.value = SpeakerError.RestoreFailed
                }
            }
        }
    }

    fun dismissUndoSnackbar() {
        synchronized(undoStack) {
            undoStack.clear()
        }
        _undoOverlay.update { it.copy(canUndo = false) }
    }

    fun clearError() {
        _error.value = null
    }

    /** Runs a database write, surfacing failures in [uiState] instead of crashing the app. */
    private fun runMutation(
        error: SpeakerError,
        block: suspend () -> Unit
    ) {
        viewModelScope.launch {
            dbMutex.withLock {
                try {
                    block()
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    _error.value = error
                }
            }
        }
    }

    private sealed interface UndoEntry {
        data class Single(val speaker: Speaker) : UndoEntry
        data class Bulk(val speakers: List<Speaker>) : UndoEntry
    }

    private data class UndoUiOverlay(
        val pendingDeletedIds: Set<Int> = emptySet(),
        val restoreVersions: Map<Int, Int> = emptyMap(),
        val canUndo: Boolean = false
    )
}
