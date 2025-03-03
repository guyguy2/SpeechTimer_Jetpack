package com.happypuppy.toastmasterstimer.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.happypuppy.toastmasterstimer.data.database.repository.Speaker
import com.happypuppy.toastmasterstimer.data.database.repository.SpeakerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class MainScreenUiState(
    val speakers: List<Speaker> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class ViewModel(private val repository: SpeakerRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(MainScreenUiState())
    val uiState: StateFlow<MainScreenUiState> get() = _uiState

    init {
        getAllSpeakers()
    }

    fun saveSpeaker(name: String, speechType: String, duration: String) {
        viewModelScope.launch {
            repository.insertSpeaker(
                Speaker(
                    name = name,
                    speechType = speechType,
                    duration = duration
                )
            )
            getAllSpeakers() // Refresh the list after adding a speaker
        }
    }

    fun getAllSpeakers() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val speakers = repository.getAllSpeakers()
                _uiState.value = _uiState.value.copy(speakers = speakers, isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message, isLoading = false)
            }
        }
    }

    fun deleteSpeaker(speaker: Speaker) {
        viewModelScope.launch {
            repository.deleteSpeaker(speaker)
            getAllSpeakers() // Refresh the list after deleting a speaker
        }
    }

    fun deleteAllSpeakers() {
        viewModelScope.launch {
            repository.deleteAllSpeakers()
            getAllSpeakers() // Refresh the list after deleting all speakers
        }
    }
}