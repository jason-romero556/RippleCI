package com.example.rippleci.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.rippleci.data.canViewEvent
import com.example.rippleci.data.isPastEvent
import com.example.rippleci.data.models.PersonalEvent
import com.example.rippleci.data.models.UserProfile
import com.example.rippleci.data.toUserProfile
import com.example.rippleci.ui.components.ClubLinkRow
import com.example.rippleci.ui.components.EventVisibilityOptions
import com.example.rippleci.ui.components.ProfileHeader
import com.example.rippleci.ui.components.ProfileInfoRow
import com.example.rippleci.ui.components.UserActionMenuButton
import com.example.rippleci.ui.components.UserActionMenuItem
import com.example.rippleci.ui.components.UserLinkRow
import com.example.rippleci.ui.components.VisibilitySelector
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.firestore

@Composable
fun EventProfileScreen(
    eventId: String,
    ownerUserId: String,
    groupId: String = "",
    onBack: () -> Unit,
    onOpenUserProfile: (String) -> Unit,
    onOpenClubProfile: (String) -> Unit,
    onOpenEventProfile: (String) -> Unit,
) {
    val db = Firebase.firestore
    val currentUserId =
        Firebase.auth.currentUser
            ?.uid
            .orEmpty()
    val eventOwnerUserId = ownerUserId.ifBlank { currentUserId }

    var ownerUserId by remember { mutableStateOf("") }
    var attendeeIds by remember { mutableStateOf(emptyList<String>()) }
    var invitedUserIds by remember { mutableStateOf(emptyList<String>()) }
    var friendProfiles by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var showEditEventScreen by remember { mutableStateOf(false) }
    var groupOwnerUserId by remember { mutableStateOf("") }
    var groupAdminIds by remember { mutableStateOf<List<String>>(emptyList()) }
    var showInviteDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showLeaveDialog by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var startTime by remember { mutableStateOf("") }
    var endTime by remember { mutableStateOf("") }
    var startAtMillis by remember { mutableStateOf(0L) }
    var endAtMillis by remember { mutableStateOf(0L) }
    var clubId by remember { mutableStateOf("") }
    var createdByUserId by remember { mutableStateOf("") }
    var creatorProfile by remember { mutableStateOf<UserProfile?>(null) }
    var visibility by remember { mutableStateOf("public") }
    var eventLoaded by remember { mutableStateOf(false) }
    var currentUserFriendIds by remember { mutableStateOf<List<String>?>(null) }
    var attendeeProfiles by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var attendeesExpanded by remember { mutableStateOf(false) }
    var blockedUserIds by remember { mutableStateOf(emptyList<String>()) }

    val eventRef =
        if (groupId.isNotBlank()) {
            db
                .collection("userGroups")
                .document(groupId)
                .collection("events")
                .document(eventId)
        } else {
            db
                .collection("users")
                .document(eventOwnerUserId)
                .collection("personalEvents")
                .document(eventId)
        }

    LaunchedEffect(eventId, currentUserId, eventOwnerUserId, groupId) {
        if (currentUserId.isBlank()) return@LaunchedEffect

        eventRef
            .get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) return@addOnSuccessListener

                title = doc.getString("title").orEmpty()
                description = doc.getString("description").orEmpty()
                location = doc.getString("location").orEmpty()
                imageUrl = doc.getString("imageUrl").orEmpty().ifBlank { doc.getString("profilePictureUrl").orEmpty() }
                date = doc.getString("date").orEmpty()
                startTime = doc.getString("startTime").orEmpty()
                endTime = doc.getString("endTime").orEmpty()
                startAtMillis = doc.getLong("startAtMillis") ?: 0L
                endAtMillis = doc.getLong("endAtMillis") ?: 0L
                clubId = doc.getString("clubId").orEmpty()
                visibility = doc.getString("visibility") ?: "public"
                eventLoaded = true

                val loadedOwnerUserId =
                    doc
                        .getString("ownerUserId")
                        .orEmpty()
                        .ifBlank { eventOwnerUserId }
                val loadedCreatedByUserId =
                    doc
                        .getString("createdByUserId")
                        .orEmpty()
                        .ifBlank { loadedOwnerUserId }

                ownerUserId = loadedOwnerUserId
                createdByUserId = loadedCreatedByUserId

                attendeeIds =
                    (doc.get("attendeeIds") as? List<*>)
                        ?.mapNotNull { it as? String }
                        ?: emptyList()

                invitedUserIds =
                    (doc.get("invitedUserIds") as? List<*>)
                        ?.mapNotNull { it as? String }
                        ?: emptyList()

                blockedUserIds =
                    (doc.get("blockedUserIds") as? List<*>)
                        ?.mapNotNull { it as? String }
                        ?: emptyList()

                db
                    .collection("users")
                    .document(loadedCreatedByUserId.ifBlank { loadedOwnerUserId })
                    .get()
                    .addOnSuccessListener { userDoc ->
                        creatorProfile = userDoc.toUserProfile()
                    }
            }
    }

    LaunchedEffect(groupId) {
        if (groupId.isBlank()) return@LaunchedEffect

        db
            .collection("userGroups")
            .document(groupId)
            .get()
            .addOnSuccessListener { doc ->
                groupOwnerUserId = doc.getString("ownerUserId").orEmpty()
                groupAdminIds = (doc.get("adminIds") as? List<*>)
                    ?.mapNotNull { it as? String }
                    ?: emptyList()
            }
    }

    LaunchedEffect(currentUserId) {
        if (currentUserId.isBlank()) return@LaunchedEffect

        db
            .collection("users")
            .document(currentUserId)
            .get()
            .addOnSuccessListener { userDoc ->
                val friendIds =
                    (userDoc.get("friends") as? List<*>)
                        ?.mapNotNull { it as? String }
                        ?: emptyList()

                currentUserFriendIds = friendIds

                if (friendIds.isEmpty()) {
                    friendProfiles = emptyList()
                } else {
                    db
                        .collection("users")
                        .whereIn(FieldPath.documentId(), friendIds.take(10))
                        .get()
                        .addOnSuccessListener { result ->
                            friendProfiles = result.documents.map { it.toUserProfile() }
                        }
                }
            }
    }

    LaunchedEffect(attendeeIds) {
        attendeeProfiles = emptyList()

        attendeeIds.chunked(10).forEach { chunk ->
            db
                .collection("users")
                .whereIn(FieldPath.documentId(), chunk)
                .get()
                .addOnSuccessListener { result ->
                    attendeeProfiles =
                        (attendeeProfiles + result.documents.map { it.toUserProfile() })
                            .distinctBy { it.id }
                }
        }
    }

    fun inviteUser(user: UserProfile) {
        if (blockedUserIds.contains(user.id)) return

        val eventDocumentOwnerUserId = ownerUserId.ifBlank { eventOwnerUserId }
        val inviteId = "${eventDocumentOwnerUserId}_${eventId}_${user.id}"

        val inviteRef =
            db
                .collection("eventInvites")
                .document(inviteId)

        val batch = db.batch()

        batch.set(
            inviteRef,
            mapOf(
                "eventId" to eventId,
                "ownerUserId" to eventDocumentOwnerUserId,
                "groupId" to groupId,
                "fromUserId" to currentUserId,
                "toUserId" to user.id,
                "eventTitle" to title,
                "status" to "pending",
                "createdAt" to System.currentTimeMillis(),
            ),
        )

        batch.update(eventRef, "invitedUserIds", FieldValue.arrayUnion(user.id))

        batch.commit().addOnSuccessListener {
            invitedUserIds = invitedUserIds + user.id
        }
    }

    fun canManageLoadedEvent(): Boolean {
        val eventDocumentOwnerUserId = ownerUserId.ifBlank { eventOwnerUserId }
        val canManageGroupEvent =
            groupId.isNotBlank() &&
                (currentUserId == groupOwnerUserId || groupAdminIds.contains(currentUserId))

        return currentUserId.isNotBlank() &&
            (
                currentUserId == eventDocumentOwnerUserId ||
                    currentUserId == createdByUserId ||
                    canManageGroupEvent
            )
    }

    fun updateEvent(updatedEvent: PersonalEvent) {
        if (!canManageLoadedEvent() || eventId.isBlank()) return

        eventRef
            .update(
                mapOf(
                    "title" to updatedEvent.title,
                    "description" to updatedEvent.description,
                    "location" to updatedEvent.location,
                    "date" to updatedEvent.date,
                    "startTime" to updatedEvent.startTime,
                    "endTime" to updatedEvent.endTime,
                    "startAtMillis" to updatedEvent.startAtMillis,
                    "endAtMillis" to updatedEvent.endAtMillis,
                    "imageUrl" to updatedEvent.imageUrl,
                    "visibility" to updatedEvent.visibility,
                ),
            ).addOnSuccessListener {
                title = updatedEvent.title
                description = updatedEvent.description
                location = updatedEvent.location
                date = updatedEvent.date
                startTime = updatedEvent.startTime
                endTime = updatedEvent.endTime
                startAtMillis = updatedEvent.startAtMillis
                endAtMillis = updatedEvent.endAtMillis
                imageUrl = updatedEvent.imageUrl
                visibility = updatedEvent.visibility
                showEditEventScreen = false
                statusMessage = "Event updated."
            }
    }

    fun updateEventVisibility(newVisibility: String) {
        if (!canManageLoadedEvent() || eventId.isBlank()) return
        if (newVisibility == visibility) return

        eventRef
            .update("visibility", newVisibility)
            .addOnSuccessListener {
                visibility = newVisibility
                statusMessage = "Visibility updated."
            }.addOnFailureListener { error ->
                statusMessage = error.message ?: "Could not update visibility."
            }
    }

    fun deleteEvent() {
        if (!canManageLoadedEvent() || eventId.isBlank()) return

        db
            .collection("eventInvites")
            .whereEqualTo("eventId", eventId)
            .get()
            .addOnSuccessListener { result ->
                val batch = db.batch()
                result.documents.forEach { inviteDoc ->
                    batch.delete(inviteDoc.reference)
                }
                batch.delete(eventRef)
                batch.commit().addOnSuccessListener { onBack() }
            }.addOnFailureListener { error ->
                statusMessage = error.message ?: "Could not delete event."
            }
    }

    fun leaveEvent() {
        if (currentUserId.isBlank() || eventId.isBlank()) return
        if (!attendeeIds.contains(currentUserId)) return

        db
            .collection("eventInvites")
            .whereEqualTo("eventId", eventId)
            .whereEqualTo("toUserId", currentUserId)
            .get()
            .addOnSuccessListener { result ->
                val batch = db.batch()
                batch.update(eventRef, "attendeeIds", FieldValue.arrayRemove(currentUserId))
                result.documents.forEach { inviteDoc ->
                    batch.update(inviteDoc.reference, "status", "left")
                }
                batch.commit().addOnSuccessListener {
                    attendeeIds = attendeeIds.filterNot { it == currentUserId }
                    statusMessage = "You left this event."

                    if (visibility == "attendees") {
                        onBack()
                    }
                }
            }.addOnFailureListener { error ->
                statusMessage = error.message ?: "Could not leave event."
            }
    }

    fun removeAttendee(attendeeId: String) {
        if (!canManageLoadedEvent() || attendeeId.isBlank()) return

        val eventDocumentOwnerUserId = ownerUserId.ifBlank { eventOwnerUserId }

        db
            .collection("eventInvites")
            .whereEqualTo("eventId", eventId)
            .whereEqualTo("ownerUserId", eventDocumentOwnerUserId)
            .whereEqualTo("toUserId", attendeeId)
            .get()
            .addOnSuccessListener { result ->
                val batch = db.batch()
                batch.update(
                    eventRef,
                    mapOf(
                        "attendeeIds" to FieldValue.arrayRemove(attendeeId),
                        "invitedUserIds" to FieldValue.arrayRemove(attendeeId),
                    ),
                )
                result.documents.forEach { inviteDoc ->
                    batch.update(
                        inviteDoc.reference,
                        mapOf(
                            "status" to "removed",
                            "updatedAt" to System.currentTimeMillis(),
                        ),
                    )
                }
                batch.commit().addOnSuccessListener {
                    attendeeIds = attendeeIds.filterNot { it == attendeeId }
                    invitedUserIds = invitedUserIds.filterNot { it == attendeeId }
                    statusMessage = "Attendee removed."
                }
            }.addOnFailureListener { error ->
                statusMessage = error.message ?: "Could not remove attendee."
            }
    }

    fun blockUserFromEvent(blockedUserId: String) {
        if (!canManageLoadedEvent() || blockedUserId.isBlank()) return

        val eventDocumentOwnerUserId = ownerUserId.ifBlank { eventOwnerUserId }

        db
            .collection("eventInvites")
            .whereEqualTo("eventId", eventId)
            .whereEqualTo("ownerUserId", eventDocumentOwnerUserId)
            .whereEqualTo("toUserId", blockedUserId)
            .get()
            .addOnSuccessListener { result ->
                val batch = db.batch()
                batch.update(
                    eventRef,
                    mapOf(
                        "blockedUserIds" to FieldValue.arrayUnion(blockedUserId),
                        "attendeeIds" to FieldValue.arrayRemove(blockedUserId),
                        "invitedUserIds" to FieldValue.arrayRemove(blockedUserId),
                    ),
                )
                result.documents.forEach { inviteDoc ->
                    batch.update(
                        inviteDoc.reference,
                        mapOf(
                            "status" to "blocked",
                            "updatedAt" to System.currentTimeMillis(),
                        ),
                    )
                }
                batch.commit().addOnSuccessListener {
                    blockedUserIds = (blockedUserIds + blockedUserId).distinct()
                    attendeeIds = attendeeIds.filterNot { it == blockedUserId }
                    invitedUserIds = invitedUserIds.filterNot { it == blockedUserId }
                    statusMessage = "User blocked from this event."
                }
            }.addOnFailureListener { error ->
                statusMessage = error.message ?: "Could not block user from event."
            }
    }

    if (showInviteDialog) {
        AlertDialog(
            onDismissRequest = { showInviteDialog = false },
            title = { Text("Invite Friends") },
            text = {
                Column {
                    friendProfiles.forEach { friend ->
                        val alreadyInvited = invitedUserIds.contains(friend.id)
                        val isAttending = attendeeIds.contains(friend.id)
                        val isBlockedFromEvent = blockedUserIds.contains(friend.id)

                        OutlinedButton(
                            onClick = { inviteUser(friend) },
                            enabled = !alreadyInvited && !isAttending && !isBlockedFromEvent,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                when {
                                    isBlockedFromEvent -> "${friend.name} blocked"
                                    isAttending -> "${friend.name} attending"
                                    alreadyInvited -> "${friend.name} invited"
                                    else -> friend.name
                                },
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showInviteDialog = false }) {
                    Text("Done")
                }
            },
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Event") },
            text = { Text("Are you sure you want to delete ${title.ifBlank { "this event" }}?") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        deleteEvent()
                    },
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                        ),
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    if (showLeaveDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveDialog = false },
            title = { Text("Leave Event") },
            text = { Text("Are you sure you want to leave ${title.ifBlank { "this event" }}?") },
            confirmButton = {
                Button(
                    onClick = {
                        showLeaveDialog = false
                        leaveEvent()
                    },
                ) {
                    Text("Leave")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showLeaveDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    if (!eventLoaded || currentUserFriendIds == null) {
        CircularProgressIndicator()
        return
    }

    val currentEvent =
        PersonalEvent(
            id = eventId,
            ownerUserId = ownerUserId.ifBlank { eventOwnerUserId },
            createdByUserId = createdByUserId.ifBlank { ownerUserId.ifBlank { eventOwnerUserId } },
            visibility = visibility,
            date = date,
            startTime = startTime,
            endTime = endTime,
            startAtMillis = startAtMillis,
            endAtMillis = endAtMillis,
            attendeeIds = attendeeIds,
            blockedUserIds = blockedUserIds,
            imageUrl = imageUrl,
        )

    if (!canViewEvent(currentEvent, currentUserId, currentUserFriendIds.orEmpty())) {
        Text("This event is private.")
        return
    }

    val canManageEvent = canManageLoadedEvent()

    if (showEditEventScreen && canManageEvent) {
        CreatePersonalEventScreen(
            initialEvent =
                currentEvent.copy(
                    title = title,
                    description = description,
                    location = location,
                    groupId = groupId,
                    imageUrl = imageUrl,
                ),
            saveButtonText = "Save Changes",
            onSave = { updateEvent(it) },
            onCancel = { showEditEventScreen = false },
        )
        return
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
    ) {
        ProfileHeader(
            title = title.ifBlank { "Untitled Event" },
            imageUrl = imageUrl,
            placeholderIcon = Icons.Default.Info,
            subtitle = {
                Text(
                    text = listOf(date, startTime).filter { it.isNotBlank() }.joinToString(" at "),
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
        )

        ProfileInfoRow("Location", location.ifBlank { "Not set" })

        Spacer(modifier = Modifier.height(16.dp))

        Text("Description", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = description.ifBlank { "No description yet." },
            style = MaterialTheme.typography.bodyMedium,
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text("${attendeeIds.size} attending")

        if (attendeeIds.isEmpty()) {
            Text("No attendees yet.", color = MaterialTheme.colorScheme.secondary)
        } else {
            TextButton(onClick = { attendeesExpanded = !attendeesExpanded }) {
                Text(if (attendeesExpanded) "Hide attendees" else "Show attendees")
            }
        }

        if (attendeesExpanded && attendeeIds.isNotEmpty()) {
            val attendeeById = attendeeProfiles.associateBy { it.id }

            attendeeIds.forEach { attendeeId ->
                val attendee = attendeeById[attendeeId]
                val attendeeName =
                    attendee
                        ?.name
                        ?.ifBlank { attendee.email }
                        ?.ifBlank { attendeeId }
                        ?: attendeeId

                val canManageAttendee =
                    canManageEvent &&
                        attendeeId != currentUserId &&
                        attendeeId != ownerUserId &&
                        attendeeId != createdByUserId

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(
                        onClick = { onOpenUserProfile(attendeeId) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(attendeeName)
                    }

                    if (canManageAttendee) {
                        Spacer(modifier = Modifier.width(8.dp))

                        UserActionMenuButton(
                            actions =
                                listOf(
                                    UserActionMenuItem(
                                        label = "Remove Attendee",
                                        onClick = { removeAttendee(attendeeId) },
                                    ),
                                    UserActionMenuItem(
                                        label = "Block from Event",
                                        onClick = { blockUserFromEvent(attendeeId) },
                                        destructive = true,
                                    ),
                                ),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        val canLeaveEvent =
            currentUserId.isNotBlank() &&
                attendeeIds.contains(currentUserId) &&
                !canManageEvent &&
                !currentEvent.isPastEvent()

        if (canLeaveEvent) {
            OutlinedButton(onClick = { showLeaveDialog = true }) {
                Text("Leave Event")
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        if (statusMessage.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(statusMessage, color = MaterialTheme.colorScheme.secondary)
        }

        if (canManageEvent) {
            Spacer(modifier = Modifier.height(16.dp))

            Text("Manage Event", style = MaterialTheme.typography.titleMedium)

            Button(onClick = { showEditEventScreen = true }) {
                Text("Edit Event")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Spacer(modifier = Modifier.height(8.dp))

            VisibilitySelector(
                title = "Event Visibility",
                selectedValue = visibility,
                options = EventVisibilityOptions,
                onValueChange = { updateEventVisibility(it) },
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (!currentEvent.isPastEvent()) {
                Button(onClick = { showInviteDialog = true }) {
                    Text("Invite Friends")
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            Button(
                onClick = { showDeleteDialog = true },
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                    ),
            ) {
                Text("Delete Event")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Created By", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        creatorProfile?.let { creator ->
            UserLinkRow(
                label = creator.name.ifBlank { "Unknown User" },
                onClick = { onOpenUserProfile(creator.id) },
            )
        } ?: Text("Creator not found")

        if (clubId.isNotBlank()) {
            Spacer(modifier = Modifier.height(24.dp))
            Text("Linked Club", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            ClubLinkRow(
                label = clubId,
                onClick = { onOpenClubProfile(clubId) },
            )
        }
    }
}
