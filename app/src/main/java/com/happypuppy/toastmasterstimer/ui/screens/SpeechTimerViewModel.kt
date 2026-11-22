package com.happypuppy.toastmasterstimer.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.happypuppy.toastmasterstimer.data.database.repository.Speaker
import com.happypuppy.toastmasterstimer.data.database.repository.SpeakerRepository
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
    val error: String? = null
)

sealed interface SpeakerEvent {
    data class Deleted(val speakerName: String) : SpeakerEvent
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
                .catch { e ->
                    _uiState.update { it.copy(error = e.message ?: "Failed to load speeches", isLoading = false) }
                }
                .collect { speakers ->
                    // A successful load supersedes any earlier error
                    _uiState.update { it.copy(speakers = speakers, isLoading = false, error = null) }
                }
        }
    }

    fun saveSpeaker(name: String, speechType: String, duration: String) {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) return
        runMutation {
            repository.insertSpeaker(
                Speaker(
                    name = trimmedName,
                    speechType = speechType,
                    duration = duration
                )
            )
        }
    }

    fun deleteSpeaker(speaker: Speaker) {
        runMutation(onSuccess = { _events.tryEmit(SpeakerEvent.Deleted(speaker.name)) }) {
            repository.deleteSpeaker(speaker)
        }
    }

    fun deleteAllSpeakers() {
        runMutation(onSuccess = { _events.tryEmit(SpeakerEvent.AllDeleted) }) {
            repository.deleteAllSpeakers()
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /** Runs a database write, surfacing failures in [uiState] instead of crashing the app. */
    private fun runMutation(
        onSuccess: () -> Unit = {},
        block: suspend () -> Unit
    ) {
        viewModelScope.launch {
            try {
                block()
                onSuccess()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Database operation failed") }
            }
        }
    }
}
