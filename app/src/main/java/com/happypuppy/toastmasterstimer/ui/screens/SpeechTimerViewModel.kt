package com.happypuppy.toastmasterstimer.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.happypuppy.toastmasterstimer.data.database.repository.Speaker
import com.happypuppy.toastmasterstimer.data.database.repository.SpeakerRepository
import com.happypuppy.toastmasterstimer.data.database.repository.UNKNOWN_SPEECH_TYPE_ID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MainScreenUiState(
    val speakers: List<Speaker> = emptyList(),
    val isLoading: Boolean = false,
    val error: SpeakerError? = null
)

sealed interface SpeakerEvent {
    data class Deleted(val speaker: Speaker) : SpeakerEvent {
        val speakerName: String get() = speaker.name
    }
    data object AllDeleted : SpeakerEvent
}

class SpeechTimerViewModel(private val repository: SpeakerRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(MainScreenUiState(isLoading = true))
    val uiState: StateFlow<MainScreenUiState> get() = _uiState

    private val _events = MutableSharedFlow<SpeakerEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<SpeakerEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            repository.getAllSpeakers()
                .catch { _ ->
                    _uiState.update { it.copy(error = SpeakerError.LoadFailed, isLoading = false) }
                }
                .collect { speakers ->
                    // A successful load supersedes any earlier error
                    _uiState.update { it.copy(speakers = speakers, isLoading = false, error = null) }
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
        runMutation(SpeakerError.RestoreFailed) {
            repository.insertSpeaker(speaker)
        }
    }

    fun deleteSpeaker(speaker: Speaker) {
        runMutation(SpeakerError.DeleteFailed, onSuccess = { _events.tryEmit(SpeakerEvent.Deleted(speaker)) }) {
            repository.deleteSpeaker(speaker)
        }
    }

    fun deleteAllSpeakers() {
        runMutation(SpeakerError.DeleteAllFailed, onSuccess = { _events.tryEmit(SpeakerEvent.AllDeleted) }) {
            repository.deleteAllSpeakers()
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /** Runs a database write, surfacing failures in [uiState] instead of crashing the app. */
    private fun runMutation(
        error: SpeakerError,
        onSuccess: () -> Unit = {},
        block: suspend () -> Unit
    ) {
        viewModelScope.launch {
            try {
                block()
                onSuccess()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _uiState.update { it.copy(error = error) }
            }
        }
    }
}
