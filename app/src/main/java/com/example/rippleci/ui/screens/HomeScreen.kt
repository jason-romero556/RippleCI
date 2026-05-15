package com.example.rippleci.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import com.example.rippleci.data.isOnDay
import com.example.rippleci.data.isPastEvent
import com.example.rippleci.data.models.SchoolEvent
import com.example.rippleci.data.models.PersonalEvent
import com.example.rippleci.data.schoolEventSortMillis
import com.example.rippleci.data.stableSchoolEventId
import com.example.rippleci.data.toEventInvite
import com.example.rippleci.data.toFirestoreMap
import com.example.rippleci.data.toPersonalEvent
import com.example.rippleci.data.toSchoolEvent
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
    onOpenEventProfile: (String, String, String) -> Unit = { _, _, _ -> },
    onAddEvent: () -> Unit = {},
) {
    val db = Firebase.firestore
    val auth = Firebase.auth
    val userId = auth.currentUser?.uid
    var personalEvents by remember { mutableStateOf<List<PersonalEvent>>(emptyList()) }
    var attendingEvents by remember { mutableStateOf<List<PersonalEvent>>(emptyList()) }
    var markedSchoolEvents by remember { mutableStateOf<Map<String, SchoolEvent>>(emptyMap()) }
    val uiState by eventsViewModel.uiState.collectAsState()
    val nowMillis = System.currentTimeMillis()
    val todayLabel = remember(nowMillis) { SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(Date(nowMillis)) }

    var schoolEventsExpanded by remember { mutableStateOf(true) }
    var myEventsExpanded by remember { mutableStateOf(true) }

    val todaysPersonalEvents =
        (personalEvents + attendingEvents)
            .distinctBy { event -> "${event.groupId}|${event.ownerUserId}|${event.id}" }
            .filter { it.isOnDay(nowMillis) }
            .sortedBy { it.eventSortMillis() }

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

    LaunchedEffect(userId) {
        val uid = userId ?: return@LaunchedEffect

        db
            .collection("eventInvites")
            .whereEqualTo("toUserId", uid)
            .whereEqualTo("status", "accepted")
            .addSnapshotListener { snapshot, _ ->
                attendingEvents = emptyList()

                val invites = snapshot?.documents?.map { it.toEventInvite() } ?: emptyList()

                invites.forEach { invite ->
                    val eventRef =
                        if (invite.groupId.isNotBlank()) {
                            db
                                .collection("userGroups")
                                .document(invite.groupId)
                                .collection("events")
                                .document(invite.eventId)
                        } else {
                            db
                                .collection("users")
                                .document(invite.ownerUserId)
                                .collection("personalEvents")
                                .document(invite.eventId)
                        }

                    eventRef.get().addOnSuccessListener { eventDoc ->
                        if (!eventDoc.exists()) return@addOnSuccessListener

                        val event =
                            eventDoc.toPersonalEvent().copy(
                                id = eventDoc.id,
                                ownerUserId =
                                    eventDoc
                                        .getString("ownerUserId")
                                        .orEmpty()
                                        .ifBlank { invite.ownerUserId },
                                groupId = invite.groupId.ifBlank { eventDoc.getString("groupId").orEmpty() },
                            )

                        attendingEvents =
                            attendingEvents
                                .filterNot { it.id == event.id && it.ownerUserId == event.ownerUserId && it.groupId == event.groupId } + event
                    }
                }
            }

        db
            .collection("users")
            .document(uid)
            .collection("schoolEventMarks")
            .addSnapshotListener { snapshot, _ ->
                markedSchoolEvents =
                    snapshot
                        ?.documents
                        ?.associate { doc -> doc.id to doc.toSchoolEvent() }
                        ?: emptyMap()
            }
    }

    fun toggleSchoolEventAttendance(event: SchoolEvent) {
        val uid = userId ?: return
        val eventKey = event.stableSchoolEventId()
        val markRef =
            db
                .collection("users")
                .document(uid)
                .collection("schoolEventMarks")
                .document(eventKey)

        if (markedSchoolEvents.containsKey(eventKey)) {
            markRef.delete()
        } else {
            markRef.set(event.toFirestoreMap())
        }
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
            Row(
                modifier =
                    Modifier
                        .weight(1f)
                        .clickable { myEventsExpanded = !myEventsExpanded }
                        .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Today's Events - $todayLabel",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = if (myEventsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (myEventsExpanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            OutlinedButton(onClick = onAddEvent) {
                Text("My Events")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        if (myEventsExpanded) {
            if (todaysPersonalEvents.isEmpty()) {
                Text("No personal events today.", style = MaterialTheme.typography.bodyMedium)
            } else {
                todaysPersonalEvents.forEach { event ->
                    PersonalEventCard(
                        event = event,
                        onClick = {
                            onOpenEventProfile(
                                event.ownerUserId.ifBlank { userId.orEmpty() },
                                event.id,
                                event.groupId,
                            )
                        },
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
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
                        state.events
                            .filter { event -> event.isOnDay(nowMillis) }
                            .sortedWith(
                                compareByDescending<SchoolEvent> {
                                    markedSchoolEvents.containsKey(it.stableSchoolEventId())
                                }.thenBy { it.schoolEventSortMillis() },
                            )

                    if (todaysEvents.isEmpty()) {
                        Text("No school events scheduled for today.", style = MaterialTheme.typography.bodyMedium)
                    } else {
                        todaysEvents.forEach { event ->
                            val eventKey = event.stableSchoolEventId()

                            EventCard(
                                event = event,
                                isMarkedAttending = markedSchoolEvents.containsKey(eventKey),
                                onToggleAttendance = { toggleSchoolEventAttendance(event) },
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
