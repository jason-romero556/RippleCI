// File: app/src/main/java/com/example/rippleci/screens/EventsScreen.kt
package com.example.rippleci.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rippleci.ui.components.EventCard // Assumes your EventCard is here!

@Composable
fun EventsScreen(
    modifier: Modifier = Modifier,
    // Inject the ViewModel here
    viewModel: EventsViewModel = viewModel()
) {
    // This line "listens" to the ViewModel. Every time the state changes (Loading -> Success),
    // the screen automatically redraws itself!
    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = "Upcoming Events",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp)
        )

        // Handle the states defined in our ViewModel
        when (val state = uiState) {
            is EventsUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is EventsUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Error: ${state.message}", color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(8.dp))
                        // A button to try again if the network failed
                        Button(onClick = { viewModel.fetchEvents() }) {
                            Text("Retry")
                        }
                    }
                }
            }
            is EventsUiState.Success -> {
                if (state.events.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = "No upcoming events found.")
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.events) { event ->
                            EventCard(event = event)
                        }
                    }
                }
            }
        }
    }
}