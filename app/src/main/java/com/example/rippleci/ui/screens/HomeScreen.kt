package com.example.rippleci.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rippleci.data.eventSortMillis
import com.example.rippleci.data.isPastEvent
import com.example.rippleci.data.models.PersonalEvent
import com.example.rippleci.data.toPersonalEvent
import com.example.rippleci.ui.components.EventCard
import com.example.rippleci.ui.components.PersonalEventCard
import com.example.rippleci.ui.events.EventsUiState
import com.example.rippleci.ui.events.EventsViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(
    eventsViewModel: EventsViewModel = viewModel(),
    onOpenEventProfile: (String, String) -> Unit = { _, _ -> },
    onAddEvent: () -> Unit = {},
) {
    val db = Firebase.firestore
    val auth = Firebase.auth
    val userId = auth.currentUser?.uid
    var personalEvents by remember { mutableStateOf<List<PersonalEvent>>(emptyList()) }
    val uiState by eventsViewModel.uiState.collectAsState()
    val nowMillis = System.currentTimeMillis()

    var schoolEventsExpanded by remember { mutableStateOf(true) }

    val upcomingPersonalEvents =
        personalEvents
            .filterNot { it.isPastEvent(nowMillis) }
            .sortedBy { it.eventSortMillis() }
    val pastPersonalEvents =
        personalEvents
            .filter { it.isPastEvent(nowMillis) }
            .sortedByDescending { it.eventSortMillis() }

    // 1. Fetch Personal Events (Custom Events)
    DisposableEffect(userId) {
        val uid = userId
        if (uid.isNullOrBlank()) {
            personalEvents = emptyList()
            onDispose { }
        } else {
            val registration =
                db
                    .collection("users")
                    .document(uid)
                    .collection("personalEvents")
                    .addSnapshotListener { snapshot, _ ->
                        personalEvents =
                            snapshot
                                ?.documents
                                ?.map { doc ->
                                    doc.toPersonalEvent().copy(
                                        id = doc.id,
                                        ownerUserId = doc.getString("ownerUserId").orEmpty().ifBlank { uid },
                                    )
                                }
                                ?: emptyList()
                    }

            onDispose { registration.remove() }
        }
    }

    // 2. Helper to filter "Today's" School Events
    val todayDateString =
        remember {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "My Custom Events",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            IconButton(onClick = onAddEvent) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Event",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        if (upcomingPersonalEvents.isEmpty()) {
            Text("No upcoming custom events.", style = MaterialTheme.typography.bodyMedium)
        } else {
            upcomingPersonalEvents.forEach { event ->
                PersonalEventCard(
                    event = event,
                    onClick = { onOpenEventProfile(event.ownerUserId.ifBlank { userId.orEmpty() }, event.id) },
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Past Custom Events",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (pastPersonalEvents.isEmpty()) {
            Text("No past custom events.", style = MaterialTheme.typography.bodyMedium)
        } else {
            pastPersonalEvents.forEach { event ->
                PersonalEventCard(
                    event = event,
                    onClick = { onOpenEventProfile(event.ownerUserId.ifBlank { userId.orEmpty() }, event.id) },
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable { schoolEventsExpanded = !schoolEventsExpanded }
                    .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Today's School Events",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Icon(
                imageVector = if (schoolEventsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = if (schoolEventsExpanded) "Collapse" else "Expand",
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        if (schoolEventsExpanded) {
            when (val state = uiState) {
                is EventsUiState.Loading -> {
                    CircularProgressIndicator()
                }

                is EventsUiState.Error -> {
                    Text("Could not load school events.")
                }

                is EventsUiState.Success -> {
                    val todaysEvents =
                        state.events.filter { event ->
                            event.startDateTime.contains(todayDateString)
                        }

                    if (todaysEvents.isEmpty()) {
                        Text("No school events scheduled for today.", style = MaterialTheme.typography.bodyMedium)
                    } else {
                        todaysEvents.forEach { event ->
                            EventCard(event = event)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
