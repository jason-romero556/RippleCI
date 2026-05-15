package com.example.rippleci.ui.events

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import com.example.rippleci.data.eventSortMillis
import com.example.rippleci.data.isPastEvent
import com.example.rippleci.data.models.EventInvite
import com.example.rippleci.data.models.PersonalEvent
import com.example.rippleci.data.toEventInvite
import com.example.rippleci.data.toPersonalEvent
import com.example.rippleci.ui.components.EventCard
import com.example.rippleci.ui.components.PersonalEventCard
import com.example.rippleci.ui.components.helpfulLinksMenuTitleStartPadding
import com.example.rippleci.ui.screens.CollapsibleSection
import com.example.rippleci.ui.screens.CreatePersonalEventScreen
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.firestore

@Composable
fun EventsScreen(
    modifier: Modifier = Modifier,
    viewModel: EventsViewModel = viewModel(),
    onOpenEventProfile: (String, String, String) -> Unit = { _, _, _ -> },
) {
    val db = Firebase.firestore
    val auth = Firebase.auth
    val userId = auth.currentUser?.uid

    var personalEvents by remember { mutableStateOf<List<PersonalEvent>>(emptyList()) }
    var isPersonalExpanded by remember { mutableStateOf(true) }
    var isPastPersonalExpanded by remember { mutableStateOf(false) }
    var isSchoolExpanded by remember { mutableStateOf(true) }
    var isCreatingEvent by remember { mutableStateOf(false) }
    var pendingInvites by remember { mutableStateOf<List<EventInvite>>(emptyList()) }
    var attendingEvents by remember { mutableStateOf<List<PersonalEvent>>(emptyList()) }
    var isInvitesExpanded by remember { mutableStateOf(true) }
    var isAttendingExpanded by remember { mutableStateOf(true) }
    var isPastAttendingExpanded by remember { mutableStateOf(false) }

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
        userId?.let { uid ->
            db
                .collection("eventInvites")
                .whereEqualTo("toUserId", uid)
                .whereEqualTo("status", "pending")
                .addSnapshotListener { snapshot, _ ->
                    pendingInvites = snapshot?.documents?.map { it.toEventInvite() } ?: emptyList()
                }

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

                        eventRef.get()
                            .addOnSuccessListener { eventDoc ->
                                if (eventDoc.exists()) {
                                    val event =
                                        eventDoc.toPersonalEvent().copy(
                                            id = eventDoc.id,
                                            ownerUserId = invite.ownerUserId,
                                            groupId = invite.groupId,
                                        )

                                    attendingEvents =
                                        attendingEvents
                                            .filterNot { it.id == event.id && it.ownerUserId == event.ownerUserId } + event
                                }
                            }
                    }
                }
        }
    }

    fun acceptInvite(invite: EventInvite) {
        val uid = userId ?: return

        val inviteRef = db.collection("eventInvites").document(invite.id)
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

        val batch = db.batch()
        batch.update(inviteRef, "status", "accepted")
        batch.update(eventRef, "attendeeIds", FieldValue.arrayUnion(uid))
        batch.update(eventRef, "invitedUserIds", FieldValue.arrayRemove(uid))
        batch.commit()
    }

    fun declineInvite(invite: EventInvite) {
        val uid = userId ?: return

        val inviteRef = db.collection("eventInvites").document(invite.id)
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

        val batch = db.batch()
        batch.update(inviteRef, "status", "declined")
        batch.update(eventRef, "invitedUserIds", FieldValue.arrayRemove(uid))
        batch.commit()
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
                            "startAtMillis" to newEvent.startAtMillis,
                            "endAtMillis" to newEvent.endAtMillis,
                            "ownerUserId" to uid,
                            "attendeeIds" to listOf(uid),
                            "invitedUserIds" to emptyList<String>(),
                            "blockedUserIds" to emptyList<String>(),
                            "imageUrl" to newEvent.imageUrl,
                            "visibility" to newEvent.visibility,
                        )

                    db
                        .collection("users")
                        .document(uid)
                        .collection("personalEvents")
                        .add(eventData)
                        .addOnSuccessListener {
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
        val nowMillis = System.currentTimeMillis()
        val upcomingPersonalEvents =
            personalEvents
                .filterNot { it.isPastEvent(nowMillis) }
                .sortedBy { it.eventSortMillis() }
        val pastPersonalEvents =
            personalEvents
                .filter { it.isPastEvent(nowMillis) }
                .sortedByDescending { it.eventSortMillis() }
        val upcomingAttendingEvents =
            attendingEvents
                .filterNot { it.isPastEvent(nowMillis) }
                .sortedBy { it.eventSortMillis() }
        val pastAttendingEvents =
            attendingEvents
                .filter { it.isPastEvent(nowMillis) }
                .sortedByDescending { it.eventSortMillis() }

        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            item {
                Text(
                    text = "Upcoming Events",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    modifier =
                        Modifier.padding(
                            start = helpfulLinksMenuTitleStartPadding,
                            top = 16.dp,
                            end = 16.dp,
                            bottom = 16.dp,
                        ),
                )
            }

            item {
                if (pendingInvites.isEmpty()) {
                    Text("No pending event invites.", modifier = Modifier.padding(horizontal = 16.dp))
                } else {
                    CollapsibleSection(
                        title = "Event Invites (${pendingInvites.size})",
                        expanded = isInvitesExpanded,
                        onToggle = { isInvitesExpanded = !isInvitesExpanded },
                    ) {
                        pendingInvites.forEach { invite ->
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(invite.eventTitle.ifBlank { "Untitled Event" })

                                    Row {
                                        Button(onClick = { acceptInvite(invite) }) {
                                            Text("Accept")
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        OutlinedButton(onClick = { declineInvite(invite) }) {
                                            Text("Decline")
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = { isCreatingEvent = true },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                ) {
                    Text("Add Event")
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                if (upcomingPersonalEvents.isEmpty()) {
                    Text("No upcoming personal events.", modifier = Modifier.padding(horizontal = 16.dp))
                } else {
                    CollapsibleSection(
                        title = "My Events (${upcomingPersonalEvents.size})",
                        expanded = isPersonalExpanded,
                        onToggle = { isPersonalExpanded = !isPersonalExpanded },
                    ) {
                        upcomingPersonalEvents.forEach { event ->
                            PersonalEventCard(
                                event = event,
                                onClick = { onOpenEventProfile(userId.orEmpty(), event.id, "") },
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }

            item {
                if (pastPersonalEvents.isEmpty()) {
                    Text("No past personal events.", modifier = Modifier.padding(horizontal = 16.dp))
                } else {
                    CollapsibleSection(
                        title = "Past My Events (${pastPersonalEvents.size})",
                        expanded = isPastPersonalExpanded,
                        onToggle = { isPastPersonalExpanded = !isPastPersonalExpanded },
                    ) {
                        pastPersonalEvents.forEach { event ->
                            PersonalEventCard(
                                event = event,
                                onClick = { onOpenEventProfile(userId.orEmpty(), event.id, "") },
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }

            item {
                if (upcomingAttendingEvents.isEmpty()) {
                    Text("No upcoming accepted event invites.", modifier = Modifier.padding(horizontal = 16.dp))
                } else {
                    CollapsibleSection(
                        title = "Events I'm Attending (${upcomingAttendingEvents.size})",
                        expanded = isAttendingExpanded,
                        onToggle = { isAttendingExpanded = !isAttendingExpanded },
                    ) {
                        upcomingAttendingEvents.forEach { event ->
                            PersonalEventCard(
                                event = event,
                                onClick = { onOpenEventProfile(event.ownerUserId, event.id, event.groupId) },
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }

            item {
                if (pastAttendingEvents.isEmpty()) {
                    Text("No past attended events.", modifier = Modifier.padding(horizontal = 16.dp))
                } else {
                    CollapsibleSection(
                        title = "Past Events I Attended (${pastAttendingEvents.size})",
                        expanded = isPastAttendingExpanded,
                        onToggle = { isPastAttendingExpanded = !isPastAttendingExpanded },
                    ) {
                        pastAttendingEvents.forEach { event ->
                            PersonalEventCard(
                                event = event,
                                onClick = { onOpenEventProfile(event.ownerUserId, event.id, event.groupId) },
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }

            item {
                when (val state = uiState) {
                    is EventsUiState.Success -> {
                        if (state.events.isEmpty()) {
                            Text(text = "No upcoming events found.", modifier = Modifier.padding(horizontal = 16.dp))
                        } else {
                            CollapsibleSection(
                                title = "School Events",
                                expanded = isSchoolExpanded,
                                onToggle = { isSchoolExpanded = !isSchoolExpanded },
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    state.events.forEach { event ->
                                        EventCard(event = event)
                                    }
                                }
                            }
                        }
                    }

                    else -> {
                        CollapsibleSection(
                            title = "School Events",
                            expanded = isSchoolExpanded,
                            onToggle = { isSchoolExpanded = !isSchoolExpanded },
                        ) {
                            when (state) {
                                is EventsUiState.Loading -> {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        CircularProgressIndicator()
                                    }
                                }

                                is EventsUiState.Error -> {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(24.dp),
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

                                is EventsUiState.Success -> Unit
                            }
                        }
                    }
                }
            }
        }
    }
}
