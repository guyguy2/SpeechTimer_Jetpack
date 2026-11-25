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
                    // A successful load supersedes a prior load failure without wiping unrelated mutation/restore errors.
                    _error.update { current ->
                        if (current == SpeakerError.LoadFailed) null else current
                    }
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
                    _error.update { current ->
                        if (current == SpeakerError.RestoreFailed) null else current
                    }
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

        viewModelScope.launch {
            dbMutex.withLock {
                try {
                    repository.deleteSpeaker(speaker)
                    _error.update { current ->
                        if (current == SpeakerError.DeleteFailed) null else current
                    }
                    val shouldEmitEvent = synchronized(undoStack) {
                        entry.committed = true
                        undoStack.any { it === entry } && !entry.undoRequested && !entry.dismissed
                    }
                    if (shouldEmitEvent) {
                        _events.trySend(SpeakerEvent.Deleted(speaker))
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    val stackNotEmpty = synchronized(undoStack) {
                        entry.dismissed = true
                        undoStack.removeAll { it === entry }
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
        viewModelScope.launch {
            dbMutex.withLock {
                var entry: UndoEntry.Bulk? = null
                var snapshotIds: Set<Int> = emptySet()
                try {
                    val snapshot = repository.getAllSpeakersOnce()
                    if (snapshot.isEmpty()) return@withLock

                    val bulkEntry = UndoEntry.Bulk(snapshot)
                    entry = bulkEntry
                    snapshotIds = snapshot.mapTo(mutableSetOf()) { it.id }
                    synchronized(undoStack) {
                        undoStack.addLast(bulkEntry)
                    }
                    _undoOverlay.update { overlay ->
                        overlay.copy(
                            pendingDeletedIds = overlay.pendingDeletedIds + snapshotIds,
                            canUndo = true
                        )
                    }

                    repository.deleteAllSpeakers()
                    _error.update { current ->
                        if (current == SpeakerError.DeleteAllFailed || current == SpeakerError.DeleteFailed) {
                            null
                        } else {
                            current
                        }
                    }
                    val shouldEmitEvent = synchronized(undoStack) {
                        bulkEntry.committed = true
                        undoStack.any { it === bulkEntry } &&
                            !bulkEntry.undoRequested &&
                            !bulkEntry.dismissed
                    }
                    if (shouldEmitEvent) {
                        _events.trySend(SpeakerEvent.AllDeleted)
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    val existingEntry = entry
                    if (existingEntry != null) {
                        val stackNotEmpty = synchronized(undoStack) {
                            existingEntry.dismissed = true
                            undoStack.removeAll { it === existingEntry }
                            undoStack.isNotEmpty()
                        }
                        _undoOverlay.update { overlay ->
                            overlay.copy(
                                pendingDeletedIds = overlay.pendingDeletedIds - snapshotIds,
                                canUndo = stackNotEmpty
                            )
                        }
                    }
                    _error.value = SpeakerError.DeleteAllFailed
                }
            }
        }
    }

    fun undoDelete() {
        val targetEntry = synchronized(undoStack) {
            val candidate = undoStack.lastOrNull { !it.undoRequested && !it.dismissed } ?: return
            candidate.undoRequested = true
            candidate
        }

        viewModelScope.launch {
            dbMutex.withLock {
                val entryStillActive = synchronized(undoStack) {
                    !targetEntry.dismissed && undoStack.any { it === targetEntry }
                }
                if (!entryStillActive) return@withLock

                try {
                    val restoredIds = when (targetEntry) {
                        is UndoEntry.Single -> {
                            repository.insertSpeaker(targetEntry.speaker)
                            listOf(targetEntry.speaker.id)
                        }
                        is UndoEntry.Bulk -> {
                            repository.insertSpeakers(targetEntry.speakers)
                            targetEntry.speakers.map { it.id }
                        }
                    }
                    val (stackNotEmpty, previous) = synchronized(undoStack) {
                        undoStack.removeAll { it === targetEntry }
                        undoStack.isNotEmpty() to undoStack.lastOrNull {
                            it.committed && !it.undoRequested && !it.dismissed
                        }
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
                    _error.update { current ->
                        if (current == SpeakerError.RestoreFailed) null else current
                    }
                    when (previous) {
                        is UndoEntry.Single -> _events.trySend(SpeakerEvent.Deleted(previous.speaker))
                        is UndoEntry.Bulk -> _events.trySend(SpeakerEvent.AllDeleted)
                        null -> Unit
                    }
                } catch (e: CancellationException) {
                    synchronized(undoStack) {
                        targetEntry.undoRequested = false
                    }
                    throw e
                } catch (_: Exception) {
                    synchronized(undoStack) {
                        targetEntry.undoRequested = false
                    }
                    _error.value = SpeakerError.RestoreFailed
                }
            }
        }
    }

    fun dismissUndoSnackbar() {
        synchronized(undoStack) {
            undoStack.forEach { it.dismissed = true }
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
                    _error.update { current ->
                        if (current == error) null else current
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    _error.value = error
                }
            }
        }
    }

    private sealed interface UndoEntry {
        var committed: Boolean
        var undoRequested: Boolean
        var dismissed: Boolean

        class Single(
            val speaker: Speaker,
            override var committed: Boolean = false,
            override var undoRequested: Boolean = false,
            override var dismissed: Boolean = false
        ) : UndoEntry

        class Bulk(
            val speakers: List<Speaker>,
            override var committed: Boolean = false,
            override var undoRequested: Boolean = false,
            override var dismissed: Boolean = false
        ) : UndoEntry
    }

    private data class UndoUiOverlay(
        val pendingDeletedIds: Set<Int> = emptySet(),
        val restoreVersions: Map<Int, Int> = emptyMap(),
        val canUndo: Boolean = false
    )
}
