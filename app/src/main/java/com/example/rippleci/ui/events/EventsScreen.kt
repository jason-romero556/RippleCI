package com.example.rippleci.ui.events

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
    var isSchoolExpanded by remember { mutableStateOf(false) }
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

    Box(modifier = modifier.fillMaxSize()) {
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

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                CollapsibleSection(
                    title = "My Events",
                    expanded = isPersonalExpanded,
                    onToggle = { isPersonalExpanded = !isPersonalExpanded },
                ) {
                    if (personalEvents.isEmpty()) {
                        Text(
                            text = "No personal events yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
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
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator()
                            }
                        }

                        is EventsUiState.Error -> {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
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
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(text = "No upcoming events found.")
                                }
                            } else {
                                state.events.forEach { event ->
                                    EventCard(event = event)
                                    Spacer(modifier = Modifier.height(12.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        if (!isCreatingEvent) {
            LargeFloatingActionButton(
                onClick = { isCreatingEvent = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Event")
            }
        }
    }
}
