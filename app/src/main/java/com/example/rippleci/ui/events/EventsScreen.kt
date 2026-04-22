package com.example.rippleci.ui.events

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.rippleci.ui.components.helpfulLinksMenuTitleStartPadding
import com.example.rippleci.ui.components.PersonalEventCard
import com.example.rippleci.ui.screens.CollapsibleSection
import com.example.rippleci.ui.screens.CreatePersonalEventScreen
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore

@Composable
fun EventsScreen(
    modifier: Modifier = Modifier,
    viewModel: EventsViewModel = viewModel(),
) {
    val db = Firebase.firestore
    val auth = Firebase.auth
    val userId = auth.currentUser?.uid
    var personalEvents by remember { mutableStateOf<List<PersonalEvent>>(emptyList()) }
    var isPersonalExpanded by remember { mutableStateOf(true) }
    var isSchoolExpanded by remember { mutableStateOf(true) }
    var isCreatingEvent by remember { mutableStateOf(false) }

    LaunchedEffect(userId) {
        userId?.let { uid ->
            db
                .collection("users")
                .document(uid)
                .collection("personalEvents")
                .get()
                .addOnSuccessListener { result ->
                    personalEvents =
                        result.documents.map { doc ->
                            PersonalEvent(
                                id = doc.id,
                                title = doc.getString("title").orEmpty(),
                                description = doc.getString("description").orEmpty(),
                                location = doc.getString("location").orEmpty(),
                                date = doc.getString("date").orEmpty(),
                                startTime = doc.getString("startTime").orEmpty(),
                                endTime = doc.getString("endTime").orEmpty(),
                            )
                        }
                }
        }
    }

    if (isCreatingEvent) {
        CreatePersonalEventScreen(
            onSave = { newEvent ->
                userId?.let { uid ->
                    val eventData =
                        mapOf(
                            "title" to newEvent.title,
                            "description" to newEvent.description,
                            "location" to newEvent.location,
                            "date" to newEvent.date,
                            "startTime" to newEvent.startTime,
                            "endTime" to newEvent.endTime,
                        )

                    db
                        .collection("users")
                        .document(uid)
                        .collection("personalEvents")
                        .add(eventData)
                        .addOnSuccessListener { docRef ->
                            personalEvents = personalEvents + newEvent.copy(id = docRef.id)
                            isCreatingEvent = false
                        }
                }
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
                modifier = Modifier.padding(
                    start = helpfulLinksMenuTitleStartPadding,
                    top = 16.dp,
                    end = 16.dp,
                    bottom = 16.dp,
                ),
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
