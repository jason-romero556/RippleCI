package com.example.rippleci.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.rippleci.data.eventSortMillis
import com.example.rippleci.data.isPastEvent
import com.example.rippleci.data.models.PersonalEvent
import com.example.rippleci.data.models.UserProfile
import com.example.rippleci.data.toPersonalEvent
import com.example.rippleci.data.toUserProfile
import com.example.rippleci.ui.components.GroupVisibilityOptions
import com.example.rippleci.ui.components.ImageUploadControls
import com.example.rippleci.ui.components.PersonalEventCard
import com.example.rippleci.ui.components.ProfileHeader
import com.example.rippleci.ui.components.UserActionMenuButton
import com.example.rippleci.ui.components.UserActionMenuItem
import com.example.rippleci.ui.components.VisibilitySelector
import com.example.rippleci.ui.components.createImageCaptureUri
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.storage

@Composable
fun UserGroupProfileScreen(
    userGroupId: String,
    onBack: () -> Unit,
    onOpenUserProfile: (String) -> Unit,
    onOpenEventProfile: (String, String, String) -> Unit,
) {
    val db = Firebase.firestore
    val storage = Firebase.storage
    val context = LocalContext.current

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
    var groupProfilePictureUrl by remember { mutableStateOf("") }
    var memberIds by remember { mutableStateOf<List<String>>(emptyList()) }
    var memberProfiles by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    val isMember = memberIds.contains(currentUserId)
    var invitedUserIds by remember { mutableStateOf<List<String>>(emptyList()) }
    var currentUserName by remember { mutableStateOf("") }
    var friendProfiles by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var groupVisibility by remember { mutableStateOf("public") }
    var showEditGroupDialog by remember { mutableStateOf(false) }
    var editedGroupName by remember { mutableStateOf("") }
    var editedGroupDescription by remember { mutableStateOf("") }
    var editedGroupProfilePictureUrl by remember { mutableStateOf("") }
    var isUploadingGroupImage by remember { mutableStateOf(false) }
    var pendingGroupCameraUri by remember { mutableStateOf<Uri?>(null) }
    var editedGroupVisibility by remember { mutableStateOf("public") }
    var showInviteDialog by remember { mutableStateOf(false) }
    var showLeaveDialog by remember { mutableStateOf(false) }
    var showOwnerLeaveDialog by remember { mutableStateOf(false) }
    var showCreateEventScreen by remember { mutableStateOf(false) }
    var groupEvents by remember { mutableStateOf<List<PersonalEvent>>(emptyList()) }

    fun uploadGroupImage(uri: Uri) {
        isUploadingGroupImage = true

        val storageRef =
            storage.reference.child("group_pictures/$userGroupId/${System.currentTimeMillis()}.jpg")

        storageRef
            .putFile(uri)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                    editedGroupProfilePictureUrl = downloadUrl.toString()
                    isUploadingGroupImage = false
                }
            }.addOnFailureListener {
                isUploadingGroupImage = false
            }
    }

    val groupImagePickerLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent(),
        ) { uri: Uri? ->
            uri?.let(::uploadGroupImage)
        }

    val groupCameraLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.TakePicture(),
        ) { didCapture ->
            val uri = pendingGroupCameraUri
            if (didCapture && uri != null) {
                uploadGroupImage(uri)
            }
        }

    LaunchedEffect(userGroupId) {
        db
            .collection("userGroups")
            .document(userGroupId)
            .get()
            .addOnSuccessListener { doc ->
                userGroupName = doc.getString("name").orEmpty()
                description = doc.getString("description").orEmpty()
                groupProfilePictureUrl = doc.getString("profilePictureUrl").orEmpty()
                ownerUserId = doc.getString("ownerUserId").orEmpty()
                adminIds = (doc.get("adminIds") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                memberIds = (doc.get("memberIds") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                groupVisibility = doc.getString("visibility") ?: "public"
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
        if (!canManageMembers || memberId == ownerUserId) return

        db
            .collection("userGroups")
            .document(userGroupId)
            .update("adminIds", FieldValue.arrayUnion(memberId))
            .addOnSuccessListener {
                adminIds = (adminIds + memberId).distinct()
            }
    }

    fun demoteAdmin(memberId: String) {
        if (!canManageMembers || memberId == ownerUserId) return

        db
            .collection("userGroups")
            .document(userGroupId)
            .update("adminIds", FieldValue.arrayRemove(memberId))
            .addOnSuccessListener {
                adminIds = adminIds.filterNot { it == memberId }
            }
    }

    fun updateGroupProfile() {
        if (!canManageMembers || editedGroupName.isBlank()) return

        db
            .collection("userGroups")
            .document(userGroupId)
            .update(
                mapOf(
                    "name" to editedGroupName.trim(),
                    "description" to editedGroupDescription.trim(),
                    "profilePictureUrl" to editedGroupProfilePictureUrl.trim(),
                    "visibility" to editedGroupVisibility,
                ),
            ).addOnSuccessListener {
                userGroupName = editedGroupName.trim()
                description = editedGroupDescription.trim()
                groupProfilePictureUrl = editedGroupProfilePictureUrl.trim()
                groupVisibility = editedGroupVisibility
                showEditGroupDialog = false
            }
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
                "imageUrl" to newEvent.imageUrl,
                "groupId" to userGroupId,
                "createdByUserId" to currentUserId,
                "attendeeIds" to emptyList<String>(),
                "blockedUserIds" to emptyList<String>(),
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

    if (showEditGroupDialog) {
        AlertDialog(
            onDismissRequest = { showEditGroupDialog = false },
            title = { Text("Edit Group") },
            text = {
                Column {
                    OutlinedTextField(
                        value = editedGroupName,
                        onValueChange = { editedGroupName = it },
                        label = { Text("Group name") },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = editedGroupDescription,
                        onValueChange = { editedGroupDescription = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    ImageUploadControls(
                        imageUrl = editedGroupProfilePictureUrl,
                        isUploading = isUploadingGroupImage,
                        onChooseFromLibrary = { groupImagePickerLauncher.launch("image/*") },
                        onUseCamera = {
                            val uri = createImageCaptureUri(context, "group_images")
                            pendingGroupCameraUri = uri
                            groupCameraLauncher.launch(uri)
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    VisibilitySelector(
                        title = "Group Visibility",
                        selectedValue = editedGroupVisibility,
                        options = GroupVisibilityOptions,
                        onValueChange = { editedGroupVisibility = it },
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { updateGroupProfile() },
                    enabled = editedGroupName.isNotBlank() && !isUploadingGroupImage,
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showEditGroupDialog = false }) {
                    Text("Cancel")
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

        ProfileHeader(
            title = userGroupName.ifBlank { "Unknown group" },
            imageUrl = groupProfilePictureUrl,
            placeholderIcon = Icons.Default.AccountBox,
            subtitle = {
                Text("${memberIds.size} members", style = MaterialTheme.typography.bodyMedium)
            },
            actions = {
                if (isMember) {
                    OutlinedButton(
                        onClick = {
                            if (isOwner) showOwnerLeaveDialog = true else showLeaveDialog = true
                        },
                    ) {
                        Text("Leave")
                    }
                }
            },
        )

        if (description.isNotBlank()) {
            Text(description, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (canManageMembers || isMember) {
            Text("Manage Group", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (canManageMembers) {
            OutlinedButton(
                onClick = {
                    editedGroupName = userGroupName
                    editedGroupDescription = description
                    editedGroupProfilePictureUrl = groupProfilePictureUrl
                    editedGroupVisibility = groupVisibility
                    showEditGroupDialog = true
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Edit Group")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { showInviteDialog = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Invite Friends")
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        if (isMember) {
            Button(
                onClick = { showCreateEventScreen = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
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
                        onOpenEventProfile(event.id, event.ownerUserId, userGroupId)
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
                        onOpenEventProfile(event.id, event.ownerUserId, userGroupId)
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    onClick = { onOpenUserProfile(member.id) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("${member.name.ifBlank { member.email.ifBlank { member.id } }} - $role")
                }

                if (canManageMembers && member.id != currentUserId && member.id != ownerUserId) {
                    Spacer(modifier = Modifier.width(8.dp))

                    UserActionMenuButton(
                        actions =
                            listOf(
                                if (adminIds.contains(member.id)) {
                                    UserActionMenuItem(
                                        label = "Remove Admin",
                                        onClick = { demoteAdmin(member.id) },
                                    )
                                } else {
                                    UserActionMenuItem(
                                        label = "Make Admin",
                                        onClick = { promoteToAdmin(member.id) },
                                    )
                                },
                                UserActionMenuItem(
                                    label = "Kick from Group",
                                    onClick = { kickMember(member.id) },
                                    destructive = true,
                                ),
                            ),
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
