package com.example.rippleci.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.rippleci.data.canViewEvent
import com.example.rippleci.data.canViewProfile
import com.example.rippleci.data.eventSortMillis
import com.example.rippleci.data.firstNameFromCandidates
import com.example.rippleci.data.isPastEvent
import com.example.rippleci.data.models.MESSAGE_PRIVACY_EVERYONE
import com.example.rippleci.data.models.PersonalEvent
import com.example.rippleci.data.models.UserProfile
import com.example.rippleci.data.toPersonalEvent
import com.example.rippleci.data.toUserProfile
import com.example.rippleci.ui.components.ClubLinkRow
import com.example.rippleci.ui.components.FriendListCard
import com.example.rippleci.ui.components.FriendshipStatusMenuButton
import com.example.rippleci.ui.components.PersonalEventCard
import com.example.rippleci.ui.components.ProfileHeader
import com.example.rippleci.ui.components.RippleButton
import com.example.rippleci.ui.components.UserPresenceIndicator
import com.example.rippleci.ui.messages.MessagesViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.firestore

@Composable
fun UserProfileScreen(
    userId: String,
    onBack: () -> Unit,
    onProfileLoaded: (String, String) -> Unit = { _, _ -> },
    onOpenConversation: (String, String) -> Unit,
    onOpenUserProfile: (String, String) -> Unit,
    onOpenClubProfile: (String) -> Unit,
    onOpenEventProfile: (String, String, String) -> Unit,
    messagesViewModel: MessagesViewModel,
) {
    val db = Firebase.firestore
    val auth = Firebase.auth
    val currentUserId = auth.currentUser?.uid.orEmpty()

    var userProfile by remember { mutableStateOf(UserProfile()) }
    var friendProfiles by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var personalEvents by remember { mutableStateOf<List<PersonalEvent>>(emptyList()) }
    var attendingEvents by remember { mutableStateOf<List<PersonalEvent>>(emptyList()) }
    var isFriend by remember { mutableStateOf(false) }
    var isPending by remember { mutableStateOf(false) }
    var pendingRequestIds by remember { mutableStateOf<List<String>>(emptyList()) }
    var profileLoaded by remember { mutableStateOf(false) }
    var currentUserFriendIds by remember { mutableStateOf<List<String>?>(null) }
    var currentUserName by remember { mutableStateOf("") }
    var isFriendListExpanded by remember { mutableStateOf(true) }
    var isClubListExpanded by remember { mutableStateOf(true) }
    var isEventListExpanded by remember { mutableStateOf(true) }
    var showPastEvents by remember { mutableStateOf(false) }
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
        attendingEvents = emptyList()
        showPastEvents = false
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

                db
                    .collection("eventInvites")
                    .whereEqualTo("toUserId", userId)
                    .whereEqualTo("status", "accepted")
                    .get()
                    .addOnSuccessListener { result ->
                        attendingEvents = emptyList()

                        result.documents.forEach { inviteDoc ->
                            val acceptedEventId = inviteDoc.getString("eventId").orEmpty()
                            val acceptedEventOwnerUserId = inviteDoc.getString("ownerUserId").orEmpty()
                            val acceptedEventGroupId = inviteDoc.getString("groupId").orEmpty()

                            if (acceptedEventId.isBlank()) return@forEach

                            val eventRef =
                                if (acceptedEventGroupId.isNotBlank()) {
                                    db
                                        .collection("userGroups")
                                        .document(acceptedEventGroupId)
                                        .collection("events")
                                        .document(acceptedEventId)
                                } else {
                                    db
                                        .collection("users")
                                        .document(acceptedEventOwnerUserId)
                                        .collection("personalEvents")
                                        .document(acceptedEventId)
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
                                                .ifBlank { acceptedEventOwnerUserId },
                                        groupId =
                                            acceptedEventGroupId.ifBlank {
                                                eventDoc.getString("groupId").orEmpty()
                                            },
                                    )

                                attendingEvents =
                                    (
                                        attendingEvents.filterNot {
                                            it.id == event.id &&
                                                it.ownerUserId == event.ownerUserId &&
                                                it.groupId == event.groupId
                                        } + event
                                    )
                            }
                        }
                    }
            }
    }

    LaunchedEffect(currentUserId, userId) {
        if (currentUserId.isBlank()) {
            currentUserFriendIds = emptyList()
            pendingRequestIds = emptyList()
            isPending = false
            return@LaunchedEffect
        }

        if (currentUserId == userId) {
            pendingRequestIds = emptyList()
            isPending = false

            db
                .collection("users")
                .document(currentUserId)
                .addSnapshotListener { doc, _ ->
                    currentUserName = doc?.getString("name").orEmpty()
                    currentUserFriendIds =
                        (doc?.get("friends") as? List<*>)
                            ?.mapNotNull { it as? String }
                            ?: emptyList()
                    isFriend = false
                }

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
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snapshot, _ ->
                pendingRequestIds =
                    snapshot
                        ?.documents
                        ?.mapNotNull { it.getString("toUserId") }
                        ?: emptyList()
                isPending = pendingRequestIds.contains(userId)
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

    fun addFriend(targetUserId: String = userId) {
        if (currentUserId.isBlank() || currentUserId == targetUserId) return
        if (targetUserId in hiddenUserIds) return

        val request =
            hashMapOf(
                "fromUserId" to currentUserId,
                "fromUserName" to
                    firstNameFromCandidates(
                        currentUserName,
                        auth.currentUser?.displayName,
                    ),
                "toUserId" to targetUserId,
                "status" to "pending",
                "timestamp" to System.currentTimeMillis(),
            )

        db.collection("friendRequests").add(request)
    }

    fun removeFriend(targetUserId: String = userId) {
        if (currentUserId.isBlank() || currentUserId == targetUserId) return

        val batch = db.batch()

        batch.update(
            db.collection("users").document(currentUserId),
            "friends",
            FieldValue.arrayRemove(targetUserId),
        )

        batch.update(
            db.collection("users").document(targetUserId),
            "friends",
            FieldValue.arrayRemove(currentUserId),
        )

        batch.commit()
    }

    fun blockUser(targetProfile: UserProfile = userProfile) {
        val targetUserId = targetProfile.id
        if (currentUserId.isBlank() || currentUserId == targetUserId || targetUserId.isBlank()) return

        val outgoingRequests =
            db
                .collection("friendRequests")
                .whereEqualTo("fromUserId", currentUserId)
                .whereEqualTo("toUserId", targetUserId)

        val incomingRequests =
            db
                .collection("friendRequests")
                .whereEqualTo("fromUserId", targetUserId)
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
                                .document(blockDocumentId(currentUserId, targetUserId))

                        batch.set(
                            blockRef,
                            mapOf(
                                "blockerUserId" to currentUserId,
                                "blockedUserId" to targetUserId,
                                "blockedUserName" to targetProfile.name,
                                "createdAt" to System.currentTimeMillis(),
                            ),
                        )
                        batch.update(
                            db.collection("users").document(currentUserId),
                            "friends",
                            FieldValue.arrayRemove(targetUserId),
                        )
                        batch.update(
                            db.collection("users").document(targetUserId),
                            "friends",
                            FieldValue.arrayRemove(currentUserId),
                        )

                        (outgoing.documents + incoming.documents).forEach { requestDoc ->
                            batch.delete(requestDoc.reference)
                        }

                        batch.commit().addOnSuccessListener {
                            blockedUserIds = blockedUserIds + targetUserId
                            friendProfiles = friendProfiles.filterNot { it.id == targetUserId }
                            pendingRequestIds = pendingRequestIds.filterNot { it == targetUserId }
                            if (targetUserId == userId) {
                                hasBlockedUser = true
                                isFriend = false
                                isPending = false
                            }
                        }
                    }
            }
    }

    fun unblockUser(targetUserId: String = userId) {
        if (currentUserId.isBlank() || currentUserId == targetUserId) return

        db
            .collection("blockedUsers")
            .document(blockDocumentId(currentUserId, targetUserId))
            .delete()
            .addOnSuccessListener {
                blockedUserIds = blockedUserIds - targetUserId
                if (targetUserId == userId) {
                    hasBlockedUser = false
                }
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
        (personalEvents + attendingEvents)
            .distinctBy { event -> "${event.groupId}|${event.ownerUserId}|${event.id}" }
            .filter { event ->
            canViewEvent(event, currentUserId, currentUserFriendIds.orEmpty())
        }
    val visibleFriendProfiles =
        friendProfiles.filter { friend ->
            friend.id !in hiddenUserIds
        }
    val displayName = userProfile.name.ifBlank { userProfile.email.ifBlank { "Unknown User" } }
    val canMessageUser =
        currentUserId != userId &&
            !hasBlockedUser &&
            !isBlockedByUser &&
            (isFriend || userProfile.messagePrivacy == MESSAGE_PRIVACY_EVERYONE)

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
    ) {
        ProfileHeader(
            title = displayName,
            imageUrl = userProfile.profilePictureUrl,
            placeholderIcon = Icons.Default.AccountCircle,
            imageSize = 128.dp,
            subtitle = {
                UserPresenceIndicator(user = userProfile)
            },
            actions = {
                if (currentUserId != userId) {
                    FriendshipStatusMenuButton(
                        isFriend = isFriend,
                        isPending = isPending,
                        hasBlockedUser = hasBlockedUser,
                        onAddFriend = { addFriend() },
                        onRemoveFriend = { removeFriend() },
                        onBlockUser = { blockUser() },
                        onUnblockUser = { unblockUser() },
                        compact = true,
                    )
                }
            },
        )

        ProfileInfoRow("Major", userProfile.major.ifBlank { "Unlisted Major" })

        Spacer(modifier = Modifier.height(8.dp))

        ProfileInfoRow("Bio", userProfile.bio.ifBlank { "No bio yet" })

        if (canMessageUser) {
            Spacer(modifier = Modifier.height(12.dp))

            RippleButton(
                text = "Send Message",
                onClick = {
                    messagesViewModel.getOrCreateDMConversation(
                        otherUserId = userId,
                        otherUserName = displayName,
                    ) { conversationId ->
                        onOpenConversation(conversationId, displayName)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (visibleFriendProfiles.isEmpty()) {
            Text("No friends yet")
        } else {
            CollapsibleSection(
                title = "Friends List",
                expanded = isFriendListExpanded,
                onToggle = { isFriendListExpanded = !isFriendListExpanded },
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                visibleFriendProfiles.forEach { friend ->
                    FriendListCard(
                        user = friend,
                        isFriend = currentUserFriendIds.orEmpty().contains(friend.id),
                        isPending = pendingRequestIds.contains(friend.id),
                        hasBlockedUser = blockedUserIds.contains(friend.id),
                        onViewProfile = {
                            onOpenUserProfile(friend.id, friend.name.ifBlank { friend.email })
                        },
                        onAddFriend = { addFriend(friend.id) },
                        onRemoveFriend = { removeFriend(friend.id) },
                        onBlockUser = { blockUser(friend) },
                        onUnblockUser = { unblockUser(friend.id) },
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (visiblePersonalEvents.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))

            CollapsibleSection(
                title = "Attending Events",
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

                if (upcomingEvents.isEmpty()) {
                    Text("No upcoming visible events.")
                } else {
                    Text("Upcoming Events", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(8.dp))

                    upcomingEvents.forEach { event ->
                        PersonalEventCard(
                            event = event,
                            onClick = {
                                onOpenEventProfile(
                                    event.id,
                                    event.ownerUserId.ifBlank { userId },
                                    event.groupId,
                                )
                            },
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                if (pastEvents.isNotEmpty()) {
                    TextButton(onClick = { showPastEvents = !showPastEvents }) {
                        Text(if (showPastEvents) "Hide past events" else "View past events")
                    }
                }

                if (showPastEvents && pastEvents.isNotEmpty()) {
                    Text("Past Events", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(8.dp))

                    pastEvents.forEach { event ->
                        PersonalEventCard(
                            event = event,
                            onClick = {
                                onOpenEventProfile(
                                    event.id,
                                    event.ownerUserId.ifBlank { userId },
                                    event.groupId,
                                )
                            },
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        } else {
            Text("No visible attending events.")
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (userProfile.clubIds.isEmpty()) {
            Text("No clubs listed.")
        } else {
            CollapsibleSection(
                title = "Clubs",
                expanded = isClubListExpanded,
                onToggle = { isClubListExpanded = !isClubListExpanded },
            ) {
                Spacer(modifier = Modifier.height(8.dp))

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
