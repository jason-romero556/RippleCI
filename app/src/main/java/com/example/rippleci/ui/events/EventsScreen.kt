package com.example.rippleci.ui.events

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.rippleci.data.isPastSchoolEvent
import com.example.rippleci.data.models.EventInvite
import com.example.rippleci.data.models.PersonalEvent
import com.example.rippleci.data.models.SchoolEvent
import com.example.rippleci.data.schoolEventSortMillis
import com.example.rippleci.data.stableSchoolEventId
import com.example.rippleci.data.toEventInvite
import com.example.rippleci.data.toFirestoreMap
import com.example.rippleci.data.toPersonalEvent
import com.example.rippleci.data.toSchoolEvent
import com.example.rippleci.ui.components.EventCard
import com.example.rippleci.ui.components.PersonalEventCard
import com.example.rippleci.ui.components.RippleButton
import com.example.rippleci.ui.components.RippleOutlinedButton
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
    var isPastEventsExpanded by remember { mutableStateOf(false) }
    var isSchoolExpanded by remember { mutableStateOf(false) }
    var isCreatingEvent by remember { mutableStateOf(false) }
    var pendingInvites by remember { mutableStateOf<List<EventInvite>>(emptyList()) }
    var attendingEvents by remember { mutableStateOf<List<PersonalEvent>>(emptyList()) }
    var markedSchoolEvents by remember { mutableStateOf<Map<String, SchoolEvent>>(emptyMap()) }
    var clearedPastEventKeys by remember { mutableStateOf<Set<String>>(emptySet()) }
    var isInvitesExpanded by remember { mutableStateOf(true) }
    var isAttendingExpanded by remember { mutableStateOf(true) }

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

                        eventRef
                            .get()
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

            db
                .collection("users")
                .document(uid)
                .collection("clearedPastEvents")
                .addSnapshotListener { snapshot, _ ->
                    clearedPastEventKeys =
                        snapshot
                            ?.documents
                            ?.map { it.id }
                            ?.toSet()
                            ?: emptySet()
                }
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

    fun clearPastEvents(
        pastCustomEvents: List<PersonalEvent>,
        pastSchoolEvents: List<SchoolEvent>,
    ) {
        val uid = userId ?: return
        val batch = db.batch()
        val clearedRef =
            db
                .collection("users")
                .document(uid)
                .collection("clearedPastEvents")
        val schoolMarksRef =
            db
                .collection("users")
                .document(uid)
                .collection("schoolEventMarks")

        pastCustomEvents.forEach { event ->
            val key = pastPersonalEventKey(event)
            batch.set(
                clearedRef.document(key),
                mapOf(
                    "type" to "custom",
                    "eventId" to event.id,
                    "ownerUserId" to event.ownerUserId,
                    "groupId" to event.groupId,
                    "clearedAt" to System.currentTimeMillis(),
                ),
            )
        }

        pastSchoolEvents.forEach { event ->
            val eventKey = event.stableSchoolEventId()
            batch.delete(schoolMarksRef.document(eventKey))
            batch.set(
                clearedRef.document(pastSchoolEventKey(event)),
                mapOf(
                    "type" to "school",
                    "eventId" to eventKey,
                    "clearedAt" to System.currentTimeMillis(),
                ),
            )
        }

        batch.commit()
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
                            "inviteesCanInvite" to newEvent.inviteesCanInvite,
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
        val pastCustomEvents =
            (pastPersonalEvents + pastAttendingEvents)
                .distinctBy { event -> "${event.groupId}|${event.ownerUserId}|${event.id}" }
                .filterNot { event -> clearedPastEventKeys.contains(pastPersonalEventKey(event)) }
                .sortedByDescending { it.eventSortMillis() }
        val pastMarkedSchoolEvents =
            markedSchoolEvents
                .values
                .filter { event -> event.isPastSchoolEvent(nowMillis) }
                .filterNot { event -> clearedPastEventKeys.contains(pastSchoolEventKey(event)) }
                .sortedByDescending { it.schoolEventSortMillis() }
        val pastEventsCount = pastCustomEvents.size + pastMarkedSchoolEvents.size

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
                        Modifier.fillMaxWidth().padding(16.dp),
                )
            }

            item {
                if (pendingInvites.isEmpty()) {
                    Text(
                        text = "No pending event invites.",
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary,
                    )
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
                                        RippleButton(
                                            text = "Accept",
                                            onClick = { acceptInvite(invite) },
                                        )

                                        Spacer(modifier = Modifier.width(8.dp))

                                        RippleOutlinedButton(
                                            text = "Decline",
                                            onClick = { declineInvite(invite) },
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }

            item {
                RippleButton(
                    text = "Add Event",
                    onClick = { isCreatingEvent = true },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                )
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                if (upcomingPersonalEvents.isEmpty()) {
                    Text(
                        text = "No upcoming personal events.",
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary,
                    )
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
                if (upcomingAttendingEvents.isEmpty()) {
                    Text(
                        text = "No upcoming accepted event invites.",
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary,
                    )
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
                if (pastEventsCount == 0) {
                    Text(
                        text = "No past events.",
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                } else {
                    CollapsibleSection(
                        title = "Past Events ($pastEventsCount)",
                        expanded = isPastEventsExpanded,
                        onToggle = { isPastEventsExpanded = !isPastEventsExpanded },
                    ) {
                        RippleOutlinedButton(
                            text = "Clear Past Events",
                            onClick = { clearPastEvents(pastCustomEvents, pastMarkedSchoolEvents) },
                            modifier = Modifier.fillMaxWidth(),
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        pastCustomEvents.forEach { event ->
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

                        pastMarkedSchoolEvents.forEach { event ->
                            EventCard(
                                event = event,
                                isMarkedAttending = true,
                                onToggleAttendance = { toggleSchoolEventAttendance(event) },
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }

            item {
                when (val state = uiState) {
                    is EventsUiState.Success -> {
                        val schoolEvents =
                            state.events
                                .filterNot { event -> event.isPastSchoolEvent(nowMillis) }
                                .sortedWith(
                                    compareByDescending<SchoolEvent> {
                                        markedSchoolEvents.containsKey(it.stableSchoolEventId())
                                    }.thenBy { it.schoolEventSortMillis() },
                                )

                        if (schoolEvents.isEmpty()) {
                            Text(
                                text = "No upcoming events found.",
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.secondary,
                            )
                        } else {
                            CollapsibleSection(
                                title = "School Events",
                                expanded = isSchoolExpanded,
                                onToggle = { isSchoolExpanded = !isSchoolExpanded },
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    schoolEvents.forEach { event ->
                                        val eventKey = event.stableSchoolEventId()

                                        EventCard(
                                            event = event,
                                            isMarkedAttending = markedSchoolEvents.containsKey(eventKey),
                                            onToggleAttendance = { toggleSchoolEventAttendance(event) },
                                        )
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
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                        ) {
                                            Text(
                                                text = "Error: ${state.message}",
                                                color = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.fillMaxWidth(),
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            RippleButton(
                                                text = "Retry",
                                                onClick = { viewModel.fetchEvents() },
                                                modifier = Modifier.wrapContentWidth(),
                                            )
                                        }
                                    }
                                }

                                is EventsUiState.Success -> {
                                    Unit
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun pastPersonalEventKey(event: PersonalEvent): String =
    "custom_${event.ownerUserId}_${event.groupId}_${event.id}".replace("/", "_")

private fun pastSchoolEventKey(event: SchoolEvent): String = "school_${event.stableSchoolEventId()}".replace("/", "_")
