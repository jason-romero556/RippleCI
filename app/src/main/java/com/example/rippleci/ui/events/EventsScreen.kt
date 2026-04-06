package com.example.rippleci.ui.events

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rippleci.data.models.PersonalEvent
import com.example.rippleci.ui.components.EventCard
import com.example.rippleci.ui.components.PersonalEventCard
import com.example.rippleci.ui.screens.CollapsibleSection
import com.example.rippleci.ui.screens.CreatePersonalEventScreen

val mockPersonalEvents =
    listOf(
        PersonalEvent(
            id = "1",
            title = "Study Group",
            description = "Review algorithms notes",
            location = "Library",
            date = "Apr 5",
            startTime = "3:00 PM",
            endTime = "4:00 PM",
        ),
    )

@Composable
fun EventsScreen(
    modifier: Modifier = Modifier,
    viewModel: EventsViewModel = viewModel(),
) {
    var isPersonalExpanded by remember { mutableStateOf(true) }
    var isSchoolExpanded by remember { mutableStateOf(true) }
    var isCreatingEvent by remember { mutableStateOf(false) }
    var personalEvents by remember { mutableStateOf(mockPersonalEvents) }

    if (isCreatingEvent) {
        CreatePersonalEventScreen(
            onSave = { newEvent ->
                personalEvents = personalEvents + newEvent
                isCreatingEvent = false
            },
            onCancel = {
                isCreatingEvent = false
            },
        )
    } else {
        val uiState by viewModel.uiState.collectAsState()

        Column(modifier = modifier.fillMaxSize()) {
            Text(
                text = "Upcoming Events",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp),
            )

            CollapsibleSection(
                title = "My Events",
                expanded = isPersonalExpanded,
                onToggle = { isPersonalExpanded = !isPersonalExpanded },
            ) {
                Button(
                    onClick = { isCreatingEvent = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Add Event")
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (personalEvents.isEmpty()) {
                    Text("No personal events yet.")
                } else {
                    personalEvents.forEach { event ->
                        PersonalEventCard(event)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }

            CollapsibleSection(
                title = "School Events",
                expanded = isSchoolExpanded,
                onToggle = { isSchoolExpanded = !isSchoolExpanded },
            ) {
                when (val state = uiState) {
                    is EventsUiState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    is EventsUiState.Error -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Error: ${state.message}",
                                    color = MaterialTheme.colorScheme.error,
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(onClick = { viewModel.fetchEvents() }) {
                                    Text("Retry")
                                }
                            }
                        }
                    }

                    is EventsUiState.Success -> {
                        if (state.events.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(text = "No upcoming events found.")
                            }
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
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
    }
}
