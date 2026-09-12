package com.happypuppy.toastmasterstimer.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.happypuppy.toastmasterstimer.data.database.repository.Speaker
import com.happypuppy.toastmasterstimer.data.database.repository.SpeakerRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MainScreenUiState(
    val speakers: List<Speaker> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class SpeechTimerViewModel(private val repository: SpeakerRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(MainScreenUiState(isLoading = true))
    val uiState: StateFlow<MainScreenUiState> get() = _uiState

    init {
        viewModelScope.launch {
            repository.getAllSpeakers()
                .catch { e ->
                    _uiState.update { it.copy(error = e.message, isLoading = false) }
                }
                .collect { speakers ->
                    // A successful load supersedes any earlier error
                    _uiState.update { it.copy(speakers = speakers, isLoading = false, error = null) }
                }
        }
    }

    fun saveSpeaker(name: String, speechType: String, duration: String) {
        runMutation {
            repository.insertSpeaker(
                Speaker(
                    name = name,
                    speechType = speechType,
                    duration = duration
                )
            )
        }
    }

    fun deleteSpeaker(speaker: Speaker) {
        runMutation { repository.deleteSpeaker(speaker) }
    }

    fun deleteAllSpeakers() {
        runMutation { repository.deleteAllSpeakers() }
    }

    /** Runs a database write, surfacing failures in [uiState] instead of crashing the app. */
    private fun runMutation(block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }
}
