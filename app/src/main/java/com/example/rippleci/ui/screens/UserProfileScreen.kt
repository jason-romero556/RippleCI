package com.example.rippleci.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.rippleci.data.canViewEvent
import com.example.rippleci.data.canViewProfile
import com.example.rippleci.data.eventSortMillis
import com.example.rippleci.data.firstNameFromCandidates
import com.example.rippleci.data.isPastEvent
import com.example.rippleci.data.models.PersonalEvent
import com.example.rippleci.data.models.UserProfile
import com.example.rippleci.data.toPersonalEvent
import com.example.rippleci.data.toUserProfile
import com.example.rippleci.ui.components.ClubLinkRow
import com.example.rippleci.ui.components.FriendListCard
import com.example.rippleci.ui.components.PersonalEventCard
import com.example.rippleci.ui.components.ProfileHeader
import com.example.rippleci.ui.components.UserLinkRow
import com.example.rippleci.ui.components.UserPresenceIndicator
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.auth.User
import com.google.firebase.firestore.firestore

@Composable
fun UserProfileScreen(
    userId: String,
    onBack: () -> Unit,
    onProfileLoaded: (String, String) -> Unit = { _, _ -> },
    onOpenUserProfile: (String, String) -> Unit,
    onOpenClubProfile: (String) -> Unit,
    onOpenEventProfile: (String) -> Unit,
) {
    val db = Firebase.firestore
    val auth = Firebase.auth
    val currentUserId = auth.currentUser?.uid.orEmpty()

    var userProfile by remember { mutableStateOf(UserProfile()) }
    var friendProfiles by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var personalEvents by remember { mutableStateOf<List<PersonalEvent>>(emptyList()) }
    var isFriend by remember { mutableStateOf(false) }
    var isPending by remember { mutableStateOf(false) }
    var profileLoaded by remember { mutableStateOf(false) }
    var currentUserFriendIds by remember { mutableStateOf<List<String>?>(null) }
    var currentUserName by remember { mutableStateOf("") }
    var isFriendListExpanded by remember { mutableStateOf(true) }
    var isClubListExpanded by remember { mutableStateOf(true) }
    var isEventListExpanded by remember { mutableStateOf(true) }
    var showFriendshipDialog by remember { mutableStateOf(false) }
    var hasBlockedUser by remember { mutableStateOf(false) }
    var isBlockedByUser by remember { mutableStateOf(false) }
    var blockedUserIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var blockedByUserIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var outgoingBlockLoaded by remember { mutableStateOf(false) }
    var incomingBlockLoaded by remember { mutableStateOf(false) }
    val hiddenUserIds = blockedUserIds + blockedByUserIds
    val blockStateLoaded = outgoingBlockLoaded && incomingBlockLoaded

    LaunchedEffect(userId) {
        profileLoaded = false
        friendProfiles = emptyList()
        personalEvents = emptyList()
        db
            .collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { doc ->
                val loadedProfile = doc.toUserProfile()
                userProfile = loadedProfile
                profileLoaded = true
                onProfileLoaded(
                    userId,
                    loadedProfile.name.ifBlank { loadedProfile.email },
                )

                if (userProfile.friendIds.isNotEmpty()) {
                    db
                        .collection("users")
                        .whereIn("__name__", userProfile.friendIds)
                        .get()
                        .addOnSuccessListener { result ->
                            friendProfiles = result.documents.map { it.toUserProfile() }
                        }
                }

                db
                    .collection("users")
                    .document(userId)
                    .collection("personalEvents")
                    .get()
                    .addOnSuccessListener { result ->
                        personalEvents =
                            result.documents.map { it.toPersonalEvent().copy(ownerUserId = userId) }
                    }
            }
    }

    LaunchedEffect(currentUserId, userId) {
        if (currentUserId.isBlank()) {
            currentUserFriendIds = emptyList()
            return@LaunchedEffect
        }

        if (currentUserId == userId) {
            currentUserFriendIds = emptyList()
            return@LaunchedEffect
        }

        db
            .collection("users")
            .document(currentUserId)
            .addSnapshotListener { doc, _ ->
                currentUserName = doc?.getString("name").orEmpty()
                val friendIds =
                    (doc?.get("friends") as? List<*>)
                        ?.mapNotNull { it as? String }
                        ?: emptyList()
                currentUserFriendIds = friendIds
                isFriend = friendIds.contains(userId)
            }

        db
            .collection("friendRequests")
            .whereEqualTo("fromUserId", currentUserId)
            .whereEqualTo("toUserId", userId)
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snapshot, _ ->
                isPending = snapshot?.isEmpty == false
            }
    }

    DisposableEffect(currentUserId, userId) {
        outgoingBlockLoaded = false
        incomingBlockLoaded = false

        if (currentUserId.isBlank() || currentUserId == userId) {
            hasBlockedUser = false
            isBlockedByUser = false
            blockedUserIds = emptySet()
            blockedByUserIds = emptySet()
            outgoingBlockLoaded = true
            incomingBlockLoaded = true

            onDispose {}
        } else {
            val outgoing =
                db
                    .collection("blockedUsers")
                    .whereEqualTo("blockerUserId", currentUserId)
                    .addSnapshotListener { snapshot, _ ->
                        val ids =
                            snapshot
                                ?.documents
                                ?.mapNotNull { it.getString("blockedUserId") }
                                ?.toSet()
                                ?: emptySet()
                        blockedUserIds = ids
                        hasBlockedUser = userId in ids
                        outgoingBlockLoaded = true
                    }

            val incoming =
                db
                    .collection("blockedUsers")
                    .whereEqualTo("blockedUserId", currentUserId)
                    .addSnapshotListener { snapshot, _ ->
                        val ids =
                            snapshot
                                ?.documents
                                ?.mapNotNull { it.getString("blockerUserId") }
                                ?.toSet()
                                ?: emptySet()
                        blockedByUserIds = ids
                        isBlockedByUser = userId in ids
                        incomingBlockLoaded = true
                    }

            onDispose {
                outgoing.remove()
                incoming.remove()
            }
        }
    }

    fun addFriend() {
        if (currentUserId.isBlank() || currentUserId == userId) return

        val request =
            hashMapOf(
                "fromUserId" to currentUserId,
                "fromUserName" to
                    firstNameFromCandidates(
                        currentUserName,
                        auth.currentUser?.displayName,
                    ),
                "toUserId" to userId,
                "status" to "pending",
                "timestamp" to System.currentTimeMillis(),
            )

        db.collection("friendRequests").add(request)
    }

    fun removeFriend() {
        if (currentUserId.isBlank() || currentUserId == userId) return

        val batch = db.batch()

        batch.update(
            db.collection("users").document(currentUserId),
            "friends",
            FieldValue.arrayRemove(userId),
        )

        batch.update(
            db.collection("users").document(userId),
            "friends",
            FieldValue.arrayRemove(currentUserId),
        )

        batch.commit()
    }

    fun blockUser() {
        if (currentUserId.isBlank() || currentUserId == userId) return

        val outgoingRequests =
            db
                .collection("friendRequests")
                .whereEqualTo("fromUserId", currentUserId)
                .whereEqualTo("toUserId", userId)

        val incomingRequests =
            db
                .collection("friendRequests")
                .whereEqualTo("fromUserId", userId)
                .whereEqualTo("toUserId", currentUserId)

        outgoingRequests
            .get()
            .addOnSuccessListener { outgoing ->
                incomingRequests
                    .get()
                    .addOnSuccessListener { incoming ->
                        val batch = db.batch()
                        val blockRef =
                            db
                                .collection("blockedUsers")
                                .document(blockDocumentId(currentUserId, userId))

                        batch.set(
                            blockRef,
                            mapOf(
                                "blockerUserId" to currentUserId,
                                "blockedUserId" to userId,
                                "blockedUserName" to userProfile.name,
                                "createdAt" to System.currentTimeMillis(),
                            ),
                        )
                        batch.update(
                            db.collection("users").document(currentUserId),
                            "friends",
                            FieldValue.arrayRemove(userId),
                        )
                        batch.update(
                            db.collection("users").document(userId),
                            "friends",
                            FieldValue.arrayRemove(currentUserId),
                        )

                        (outgoing.documents + incoming.documents).forEach { requestDoc ->
                            batch.delete(requestDoc.reference)
                        }

                        batch.commit().addOnSuccessListener {
                            hasBlockedUser = true
                            blockedUserIds = blockedUserIds + userId
                            isFriend = false
                            isPending = false
                            showFriendshipDialog = false
                        }
                    }
            }
    }

    fun unblockUser() {
        if (currentUserId.isBlank() || currentUserId == userId) return

        db
            .collection("blockedUsers")
            .document(blockDocumentId(currentUserId, userId))
            .delete()
            .addOnSuccessListener {
                hasBlockedUser = false
                blockedUserIds = blockedUserIds - userId
                showFriendshipDialog = false
            }
    }

    if (!profileLoaded || currentUserFriendIds == null || !blockStateLoaded) {
        CircularProgressIndicator()
        return
    }

    if (isBlockedByUser) {
        Text("This profile is unavailable.")
        return
    }

    if (!canViewProfile(userProfile, currentUserId, currentUserFriendIds.orEmpty())) {
        Text("This profile is private.")
        return
    }

    val visiblePersonalEvents =
        personalEvents.filter { event ->
            canViewEvent(event, currentUserId, currentUserFriendIds.orEmpty())
        }
    val visibleFriendProfiles =
        friendProfiles.filter { friend ->
            friend.id !in hiddenUserIds
        }

    if (showFriendshipDialog) {
        AlertDialog(
            onDismissRequest = { showFriendshipDialog = false },
            title = { Text("Friendship Status") },
            text = {
                Column {
                    Text("Manage your connection with ${userProfile.name.ifBlank { "this user" }}.")

                    Spacer(modifier = Modifier.height(12.dp))

                    when {
                        hasBlockedUser -> {
                            Button(
                                onClick = { unblockUser() },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Unblock")
                            }
                        }

                        isFriend -> {
                            OutlinedButton(
                                onClick = {
                                    removeFriend()
                                    showFriendshipDialog = false
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Remove Friend")
                            }
                        }

                        isPending -> {
                            OutlinedButton(
                                onClick = {},
                                enabled = false,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Friend Request Pending")
                            }
                        }

                        else -> {
                            Button(
                                onClick = {
                                    addFriend()
                                    showFriendshipDialog = false
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Send Friend Request")
                            }
                        }
                    }

                    if (!hasBlockedUser) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { blockUser() },
                            modifier = Modifier.fillMaxWidth(),
                            colors =
                                ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                ),
                        ) {
                            Text("Block User")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFriendshipDialog = false }) {
                    Text("Close")
                }
            },
        )
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
    ) {
        ProfileHeader(
            title = userProfile.name.ifBlank { "Unknown User" },
            imageUrl = userProfile.profilePictureUrl,
            placeholderIcon = Icons.Default.AccountCircle,
            subtitle = {
                UserPresenceIndicator(user = userProfile)
            },
            actions = {
                if (currentUserId != userId) {
                    val label =
                        when {
                            hasBlockedUser -> "Blocked"
                            isFriend -> "Friends"
                            isPending -> "Pending"
                            else -> "Not Friends"
                        }

                    OutlinedButton(onClick = { showFriendshipDialog = true }) {
                        Text(label)
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Manage friendship",
                        )
                    }
                }
            },
        )

        ProfileInfoRow("Major", userProfile.major.ifBlank { "Unlisted Major" })

        Spacer(modifier = Modifier.height(8.dp))

        ProfileInfoRow("Bio", userProfile.bio.ifBlank { "No bio yet" })

        Spacer(modifier = Modifier.height(24.dp))

        CollapsibleSection(
            title = "Friends List",
            expanded = isFriendListExpanded,
            onToggle = { isFriendListExpanded = !isFriendListExpanded },
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            if (visibleFriendProfiles.isEmpty()) {
                Text("No friends yet")
            } else {
                visibleFriendProfiles.forEach { friend ->
                    FriendListCard(
                        user = friend,
                        onViewProfile = {
                            onOpenUserProfile(friend.id, friend.name.ifBlank { friend.email })
                        },
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (visiblePersonalEvents.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))

            CollapsibleSection(
                title = "Events",
                expanded = isEventListExpanded,
                onToggle = { isEventListExpanded = !isEventListExpanded },
            ) {
                val nowMillis = System.currentTimeMillis()
                val upcomingEvents =
                    visiblePersonalEvents
                        .filterNot { it.isPastEvent(nowMillis) }
                        .sortedBy { it.eventSortMillis() }
                val pastEvents =
                    visiblePersonalEvents
                        .filter { it.isPastEvent(nowMillis) }
                        .sortedByDescending { it.eventSortMillis() }

                Spacer(modifier = Modifier.height(8.dp))

                if (upcomingEvents.isNotEmpty()) {
                    Text("Upcoming Events", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(8.dp))

                    upcomingEvents.forEach { event ->
                        PersonalEventCard(
                            event = event,
                            onClick = { onOpenEventProfile(event.id) },
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                if (pastEvents.isNotEmpty()) {
                    Text("Past Events", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(8.dp))

                    pastEvents.forEach { event ->
                        PersonalEventCard(
                            event = event,
                            onClick = { onOpenEventProfile(event.id) },
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        } else {
            Text("No visible personal events.")
        }

        Spacer(modifier = Modifier.height(16.dp))

        CollapsibleSection(
            title = "Clubs",
            expanded = isClubListExpanded,
            onToggle = { isClubListExpanded = !isClubListExpanded },
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            if (userProfile.clubIds.isEmpty()) {
                Text("No clubs listed.")
            } else {
                userProfile.clubIds.forEach { clubId ->
                    ClubLinkRow(
                        label = clubId,
                        onClick = { onOpenClubProfile(clubId) },
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

private fun blockDocumentId(
    blockerUserId: String,
    blockedUserId: String,
): String = "${blockerUserId}_$blockedUserId"
