// File: app/src/main/java/com/example/rippleci/screens/EventsViewModel.kt
package com.example.rippleci.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rippleci.data.RetrofitInstance
import com.example.rippleci.data.SchoolEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// 1. Define the possible states for the screen
sealed class EventsUiState {
    object Loading : EventsUiState()
    data class Success(val events: List<SchoolEvent>) : EventsUiState()
    data class Error(val message: String) : EventsUiState()
}

class EventsViewModel : ViewModel() {

    // 2. Create a stream of data that the UI can "listen" to
    private val _uiState = MutableStateFlow<EventsUiState>(EventsUiState.Loading)
    val uiState: StateFlow<EventsUiState> = _uiState.asStateFlow()

    init {
        // Automatically fetch data when the ViewModel is created
        fetchEvents()
    }

    fun fetchEvents() {
        // Run this in the background
        viewModelScope.launch {
            _uiState.value = EventsUiState.Loading
            try {
                // Make the network call
                val events = RetrofitInstance.api.getEvents()

                // If it works, update the state to Success and pass the events
                _uiState.value = EventsUiState.Success(events)
            } catch (e: Exception) {
                // If the internet is down or it fails, pass the error
                _uiState.value = EventsUiState.Error(e.message ?: "An unknown error occurred")
            }
        }
    }
}