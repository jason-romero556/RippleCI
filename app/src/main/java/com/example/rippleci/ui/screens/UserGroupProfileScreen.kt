package com.example.rippleci.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.rippleci.data.eventSortMillis
import com.example.rippleci.data.isPastEvent
import com.example.rippleci.data.models.PersonalEvent
import com.example.rippleci.data.models.UserProfile
import com.example.rippleci.data.toPersonalEvent
import com.example.rippleci.data.toUserProfile
import com.example.rippleci.ui.components.EventVisibilityOptions
import com.example.rippleci.ui.components.PersonalEventCard
import com.example.rippleci.ui.components.UserLinkRow
import com.example.rippleci.ui.components.VisibilitySelector
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.firestore

@Composable
fun UserGroupProfileScreen(
    userGroupId: String,
    onBack: () -> Unit,
    onOpenUserProfile: (String) -> Unit,
) {
    val db = Firebase.firestore

    val currentUserId =
        Firebase.auth.currentUser
            ?.uid
            .orEmpty()

    var description by remember { mutableStateOf("") }
    var ownerUserId by remember { mutableStateOf("") }
    var adminIds by remember { mutableStateOf<List<String>>(emptyList()) }
    var isOwner = currentUserId == ownerUserId
    var isAdmin = adminIds.contains(currentUserId)
    var canManageMembers = isOwner || isAdmin
    var userGroupName by remember { mutableStateOf("") }
    var memberIds by remember { mutableStateOf<List<String>>(emptyList()) }
    var memberProfiles by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    val isMember = memberIds.contains(currentUserId)
    var invitedUserIds by remember { mutableStateOf<List<String>>(emptyList()) }
    var currentUserName by remember { mutableStateOf("") }
    var friendProfiles by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var showInviteDialog by remember { mutableStateOf(false) }
    var showLeaveDialog by remember { mutableStateOf(false) }
    var showOwnerLeaveDialog by remember { mutableStateOf(false) }
    var showCreateEventScreen by remember { mutableStateOf(false) }
    var groupEvents by remember { mutableStateOf<List<PersonalEvent>>(emptyList()) }
    var selectedGroupEvent by remember { mutableStateOf<PersonalEvent?>(null) }
    var groupEventStatusMessage by remember { mutableStateOf("") }

    LaunchedEffect(userGroupId) {
        db
            .collection("userGroups")
            .document(userGroupId)
            .get()
            .addOnSuccessListener { doc ->
                userGroupName = doc.getString("name").orEmpty()
                description = doc.getString("description").orEmpty()
                ownerUserId = doc.getString("ownerUserId").orEmpty()
                adminIds = (doc.get("adminIds") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                memberIds = (doc.get("memberIds") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                invitedUserIds =
                    (doc.get("invitedUserIds") as? List<*>)
                        ?.mapNotNull { it as? String }
                        ?: emptyList()
            }
    }

    LaunchedEffect(memberIds) {
        if (memberIds.isEmpty()) {
            memberProfiles = emptyList()
        } else {
            db
                .collection("users")
                .whereIn("__name__", memberIds.take(10))
                .get()
                .addOnSuccessListener { result ->
                    memberProfiles = result.documents.map { it.toUserProfile() }
                }
        }
    }

    LaunchedEffect(currentUserId) {
        if (currentUserId.isBlank()) return@LaunchedEffect

        db
            .collection("users")
            .document(currentUserId)
            .get()
            .addOnSuccessListener { userDoc ->
                currentUserName =
                    userDoc
                        .getString("name")
                        .orEmpty()
                        .ifBlank { userDoc.getString("email").orEmpty() }
                        .ifBlank {
                            Firebase.auth.currentUser
                                ?.email
                                .orEmpty()
                        }.ifBlank { "Unknown user" }

                val friendIds =
                    (userDoc.get("friends") as? List<*>)
                        ?.mapNotNull { it as? String }
                        ?: emptyList()

                if (friendIds.isNotEmpty()) {
                    db
                        .collection("users")
                        .whereIn("__name__", friendIds.take(10))
                        .get()
                        .addOnSuccessListener { result ->
                            friendProfiles = result.documents.map { it.toUserProfile() }
                        }
                }
            }
    }

    DisposableEffect(userGroupId) {
        val registration =
            db
                .collection("userGroups")
                .document(userGroupId)
                .collection("events")
                .addSnapshotListener { snapshot, _ ->
                    groupEvents =
                        snapshot
                            ?.documents
                            ?.map { doc ->
                                doc.toPersonalEvent().copy(
                                    id = doc.id,
                                    ownerUserId =
                                        doc
                                            .getString("ownerUserId")
                                            .orEmpty()
                                            .ifBlank { doc.getString("createdByUserId").orEmpty() },
                                )
                            }
                            ?: emptyList()
                }

        onDispose { registration.remove() }
    }

    fun inviteUser(user: UserProfile) {
        val inviteId = "${ownerUserId}_${userGroupId}_${user.id}"

        val groupRef =
            db
                .collection("userGroups")
                .document(userGroupId)

        val inviteRef =
            db
                .collection("userGroupInvites")
                .document(inviteId)

        val batch = db.batch()

        batch.set(
            inviteRef,
            mapOf(
                "groupId" to userGroupId,
                "ownerUserId" to ownerUserId,
                "fromUserId" to currentUserId,
                "fromUserName" to currentUserName,
                "toUserId" to user.id,
                "userGroupName" to userGroupName,
                "status" to "pending",
                "createdAt" to System.currentTimeMillis(),
            ),
        )

        batch.update(groupRef, "invitedUserIds", FieldValue.arrayUnion(user.id))

        batch.commit().addOnSuccessListener {
            invitedUserIds = invitedUserIds + user.id
        }
    }

    fun disbandGroup() {
        db
            .collection("userGroups")
            .document(userGroupId)
            .delete()
            .addOnSuccessListener { onBack() }
    }

    fun transferLeadershipAndLeave(newOwnerId: String) {
        val remainingMembers = memberIds.filterNot { it == currentUserId }
        val updatedAdmins =
            (adminIds + newOwnerId)
                .filterNot { it == currentUserId }
                .distinct()

        db
            .collection("userGroups")
            .document(userGroupId)
            .update(
                mapOf(
                    "ownerUserId" to newOwnerId,
                    "memberIds" to remainingMembers,
                    "adminIds" to updatedAdmins,
                ),
            ).addOnSuccessListener { onBack() }
    }

    fun leaveGroup() {
        if (currentUserId.isBlank() || !isMember) return
        if (isOwner) return

        db
            .collection("userGroups")
            .document(userGroupId)
            .update(
                mapOf(
                    "memberIds" to FieldValue.arrayRemove(currentUserId),
                    "adminIds" to FieldValue.arrayRemove(currentUserId),
                ),
            ).addOnSuccessListener {
                onBack()
            }
    }

    fun kickMember(memberId: String) {
        db
            .collection("userGroups")
            .document(userGroupId)
            .update(
                mapOf(
                    "memberIds" to FieldValue.arrayRemove(memberId),
                    "adminIds" to FieldValue.arrayRemove(memberId),
                ),
            )
    }

    fun promoteToAdmin(memberId: String) {
        db
            .collection("userGroups")
            .document(userGroupId)
            .update("adminIds", FieldValue.arrayUnion(memberId))
    }

    fun createGroupEvent(newEvent: PersonalEvent) {
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
                "groupId" to userGroupId,
                "createdByUserId" to currentUserId,
                "attendeeIds" to emptyList<String>(),
                "visibility" to newEvent.visibility,
                "createdAt" to System.currentTimeMillis(),
                "ownerUserId" to currentUserId,
                "invitedUserIds" to memberIds,
            )

        db
            .collection("userGroups")
            .document(userGroupId)
            .collection("events")
            .add(eventData)
            .addOnSuccessListener {
                showCreateEventScreen = false
            }
    }

    fun joinGroupEvent(event: PersonalEvent) {
        if (currentUserId.isBlank() || event.id.isBlank()) return
        if (event.isPastEvent()) return

        db
            .collection("userGroups")
            .document(userGroupId)
            .collection("events")
            .document(event.id)
            .update("attendeeIds", FieldValue.arrayUnion(currentUserId))
    }

    fun leaveGroupEvent(event: PersonalEvent) {
        if (currentUserId.isBlank() || event.id.isBlank()) return

        db
            .collection("userGroups")
            .document(userGroupId)
            .collection("events")
            .document(event.id)
            .update("attendeeIds", FieldValue.arrayRemove(currentUserId))
            .addOnSuccessListener {
                val updatedEvent = event.copy(attendeeIds = event.attendeeIds.filterNot { it == currentUserId })
                groupEvents =
                    groupEvents.map { groupEvent ->
                        if (groupEvent.id == event.id) updatedEvent else groupEvent
                    }
                selectedGroupEvent = updatedEvent
            }
    }

    fun canManageGroupEvent(event: PersonalEvent): Boolean =
        currentUserId.isNotBlank() &&
            (event.ownerUserId == currentUserId || event.createdByUserId == currentUserId)

    fun updateGroupEventVisibility(
        event: PersonalEvent,
        newVisibility: String,
    ) {
        if (!canManageGroupEvent(event) || event.id.isBlank()) return
        if (newVisibility == event.visibility) return

        db
            .collection("userGroups")
            .document(userGroupId)
            .collection("events")
            .document(event.id)
            .update("visibility", newVisibility)
            .addOnSuccessListener {
                val updatedEvent = event.copy(visibility = newVisibility)
                groupEvents =
                    groupEvents.map { groupEvent ->
                        if (groupEvent.id == event.id) updatedEvent else groupEvent
                    }
                selectedGroupEvent = updatedEvent
                groupEventStatusMessage = "Visibility updated."
            }.addOnFailureListener { error ->
                groupEventStatusMessage = error.message ?: "Could not update visibility."
            }
    }

    fun deleteGroupEvent(event: PersonalEvent) {
        if (!canManageGroupEvent(event) || event.id.isBlank()) return

        db
            .collection("userGroups")
            .document(userGroupId)
            .collection("events")
            .document(event.id)
            .delete()
            .addOnSuccessListener {
                selectedGroupEvent = null
                groupEventStatusMessage = ""
            }.addOnFailureListener { error ->
                groupEventStatusMessage = error.message ?: "Could not delete event."
            }
    }

    if (showInviteDialog) {
        AlertDialog(
            onDismissRequest = { showInviteDialog = false },
            title = { Text("Invite Friends") },
            text = {
                Column {
                    val inviteOptions = friendProfiles.filterNot { memberIds.contains(it.id) }

                    if (inviteOptions.isEmpty()) {
                        Text("No friends available to invite.")
                    } else {
                        inviteOptions.forEach { friend ->
                            val isPendingInvite = invitedUserIds.contains(friend.id)
                            val displayName = friend.name.ifBlank { friend.email.ifBlank { friend.id } }

                            OutlinedButton(
                                onClick = { inviteUser(friend) },
                                enabled = !isPendingInvite,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    if (isPendingInvite) {
                                        "$displayName - pending invite"
                                    } else {
                                        "Invite $displayName"
                                    },
                                )
                            }
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

    if (showOwnerLeaveDialog) {
        val possibleNewLeaders = memberProfiles.filter { it.id != currentUserId }

        AlertDialog(
            onDismissRequest = { showOwnerLeaveDialog = false },
            title = { Text("Leave Group") },
            text = {
                Column {
                    if (possibleNewLeaders.isEmpty()) {
                        Text("You are the only member. You can only disband this group.")
                    } else {
                        Text("Choose a new leader before leaving.")

                        possibleNewLeaders.forEach { member ->
                            OutlinedButton(
                                onClick = { transferLeadershipAndLeave(member.id) },
                            ) {
                                Text(member.name.ifBlank { member.email.ifBlank { member.id } })
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { disbandGroup() }) {
                    Text("Disband Group")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showOwnerLeaveDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    if (showLeaveDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveDialog = false },
            title = { Text("Leave Group") },
            text = { Text("Are you sure you want to leave $userGroupName?") },
            confirmButton = {
                Button(onClick = { leaveGroup() }) {
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

    selectedGroupEvent?.let { event ->
        val isAttending = event.attendeeIds.contains(currentUserId)
        val canManageEvent = canManageGroupEvent(event)
        val eventHasPassed = event.isPastEvent()
        var attendeesExpanded by remember(event.id) { mutableStateOf(false) }
        var showDeleteEventDialog by remember(event.id) { mutableStateOf(false) }
        val memberById = memberProfiles.associateBy { it.id }

        if (showDeleteEventDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteEventDialog = false },
                title = { Text("Delete Event") },
                text = { Text("Are you sure you want to delete ${event.title.ifBlank { "this event" }}?") },
                confirmButton = {
                    Button(
                        onClick = {
                            showDeleteEventDialog = false
                            deleteGroupEvent(event)
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
                    OutlinedButton(onClick = { showDeleteEventDialog = false }) {
                        Text("Cancel")
                    }
                },
            )
        } else {
            AlertDialog(
                onDismissRequest = { selectedGroupEvent = null },
                title = { Text(event.title.ifBlank { "Group Event" }) },
                text = {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        Text(event.description.ifBlank { "No description yet." })
                        Text("${event.date} ${event.startTime} - ${event.endTime}")
                        Text("${event.attendeeIds.size} attending")

                        TextButton(onClick = { attendeesExpanded = !attendeesExpanded }) {
                            Text(if (attendeesExpanded) "Hide attendees" else "Show attendees")
                        }

                        if (attendeesExpanded) {
                            event.attendeeIds.forEach { attendeeId ->
                                val attendee = memberById[attendeeId]
                                UserLinkRow(
                                    label =
                                        attendee
                                            ?.name
                                            ?.ifBlank { attendee.email }
                                            ?.ifBlank { attendeeId }
                                            ?: attendeeId,
                                    onClick = { onOpenUserProfile(attendeeId) },
                                )
                            }
                        }

                        if (canManageEvent) {
                            Spacer(modifier = Modifier.height(16.dp))

                            VisibilitySelector(
                                title = "Event Visibility",
                                selectedValue = event.visibility,
                                options = EventVisibilityOptions,
                                onValueChange = { updateGroupEventVisibility(event, it) },
                            )

                            if (groupEventStatusMessage.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(groupEventStatusMessage, color = MaterialTheme.colorScheme.secondary)
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Button(
                                onClick = { showDeleteEventDialog = true },
                                colors =
                                    ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error,
                                    ),
                            ) {
                                Text("Delete Event")
                            }
                        }
                    }
                },
                confirmButton = {
                    if (eventHasPassed) {
                        OutlinedButton(
                            onClick = {},
                            enabled = false,
                        ) {
                            Text("Past Event")
                        }
                    } else if (isAttending) {
                        OutlinedButton(
                            onClick = { leaveGroupEvent(event) },
                        ) {
                            Text("Leave Event")
                        }
                    } else {
                        Button(
                            onClick = {
                                joinGroupEvent(event)
                                selectedGroupEvent = null
                            },
                        ) {
                            Text("Join Event")
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { selectedGroupEvent = null }) {
                        Text("Close")
                    }
                },
            )
        }
    }

    if (showCreateEventScreen) {
        CreatePersonalEventScreen(
            onSave = { newEvent -> createGroupEvent(newEvent) },
            onCancel = { showCreateEventScreen = false },
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
        val nowMillis = System.currentTimeMillis()
        val upcomingGroupEvents =
            groupEvents
                .filterNot { it.isPastEvent(nowMillis) }
                .sortedBy { it.eventSortMillis() }
        val pastGroupEvents =
            groupEvents
                .filter { it.isPastEvent(nowMillis) }
                .sortedByDescending { it.eventSortMillis() }

        if (isMember) {
            OutlinedButton(
                onClick = {
                    if (isOwner) showOwnerLeaveDialog = true else showLeaveDialog = true
                },
            ) {
                Text("Leave Group")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = userGroupName.ifBlank { "Unknown group" },
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (canManageMembers) {
            Button(onClick = { showInviteDialog = true }) {
                Text("Invite Friends")
            }
        }

        if (isMember) {
            Button(onClick = { showCreateEventScreen = true }) {
                Text("Create Group Event")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Group Events", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        if (upcomingGroupEvents.isEmpty()) {
            Text("No upcoming group events.")
        } else {
            upcomingGroupEvents.forEach { event ->
                PersonalEventCard(
                    event = event,
                    onClick = {
                        groupEventStatusMessage = ""
                        selectedGroupEvent = event
                    },
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Past Group Events", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        if (pastGroupEvents.isEmpty()) {
            Text("No past group events.")
        } else {
            pastGroupEvents.forEach { event ->
                PersonalEventCard(
                    event = event,
                    onClick = {
                        groupEventStatusMessage = ""
                        selectedGroupEvent = event
                    },
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Spacer(modifier = Modifier.height(24.dp))

        Text("Members", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        memberProfiles.forEach { member ->
            val role =
                when {
                    member.id == ownerUserId -> "Owner"
                    adminIds.contains(member.id) -> "Admin"
                    else -> "Member"
                }

            UserLinkRow(
                label = "${member.name.ifBlank { member.email.ifBlank { member.id } }} - $role",
                onClick = { onOpenUserProfile(member.id) },
            )

            if (canManageMembers && member.id != currentUserId && member.id != ownerUserId) {
                OutlinedButton(onClick = { kickMember(member.id) }) {
                    Text("Kick")
                }

                if (!adminIds.contains(member.id)) {
                    Button(onClick = { promoteToAdmin(member.id) }) {
                        Text("Make Admin")
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
