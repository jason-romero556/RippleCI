package com.example.rippleci.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.rippleci.data.canViewEvent
import com.example.rippleci.data.canViewGroupProfile
import com.example.rippleci.data.canViewPastGroupEvents
import com.example.rippleci.data.eventSortMillis
import com.example.rippleci.data.isPastEvent
import com.example.rippleci.data.models.EVENT_ATTENDEE_VISIBILITY_FULL
import com.example.rippleci.data.models.GROUP_PAST_EVENTS_ADMINS
import com.example.rippleci.data.models.PersonalEvent
import com.example.rippleci.data.models.UserGroupAdminPermissions
import com.example.rippleci.data.models.UserGroupBulletinComment
import com.example.rippleci.data.models.UserGroupBulletinPost
import com.example.rippleci.data.models.UserGroupProfile
import com.example.rippleci.data.models.UserProfile
import com.example.rippleci.data.toPersonalEvent
import com.example.rippleci.data.toUserGroupBulletinComment
import com.example.rippleci.data.toUserGroupBulletinPost
import com.example.rippleci.data.toUserGroupProfile
import com.example.rippleci.data.toUserProfile
import com.example.rippleci.ui.components.GroupEventVisibilityOptions
import com.example.rippleci.ui.components.GroupVisibilityOptions
import com.example.rippleci.ui.components.ImageUploadControls
import com.example.rippleci.ui.components.PastGroupEventsVisibilityOptions
import com.example.rippleci.ui.components.PersonalEventCard
import com.example.rippleci.ui.components.ProfileHeader
import com.example.rippleci.ui.components.RippleButton
import com.example.rippleci.ui.components.RippleOutlinedButton
import com.example.rippleci.ui.components.UserActionMenuButton
import com.example.rippleci.ui.components.UserActionMenuItem
import com.example.rippleci.ui.components.BulletinBoardVisibilityOptions
import com.example.rippleci.ui.components.VisibilitySelector
import com.example.rippleci.ui.components.createImageCaptureUri
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
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
    val currentUserId = Firebase.auth.currentUser?.uid.orEmpty()
    val groupRef = remember(userGroupId) { db.collection("userGroups").document(userGroupId) }

    var groupProfile by remember { mutableStateOf<UserGroupProfile?>(null) }
    var memberProfiles by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var friendProfiles by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var groupEvents by remember { mutableStateOf<List<PersonalEvent>>(emptyList()) }
    var personalEvents by remember { mutableStateOf<List<PersonalEvent>>(emptyList()) }
    var bulletinPosts by remember { mutableStateOf<List<UserGroupBulletinPost>>(emptyList()) }
    var invitedUserIdsState by remember { mutableStateOf<List<String>>(emptyList()) }
    var currentUserName by remember { mutableStateOf("") }
    var currentUserProfilePictureUrl by remember { mutableStateOf("") }
    var currentUserFriendIds by remember { mutableStateOf<List<String>>(emptyList()) }
    var currentUserLoaded by remember { mutableStateOf(currentUserId.isBlank()) }
    var groupLoaded by remember { mutableStateOf(false) }
    var showEditGroupDialog by remember { mutableStateOf(false) }
    var editedGroupName by remember { mutableStateOf("") }
    var editedGroupDescription by remember { mutableStateOf("") }
    var editedGroupProfilePictureUrl by remember { mutableStateOf("") }
    var editedGroupVisibility by remember { mutableStateOf("public") }
    var editedMembersCanInvite by remember { mutableStateOf(false) }
    var editedMembersCanPostBulletin by remember { mutableStateOf(false) }
    var editedBulletinVisibility by remember { mutableStateOf("public") }
    var editedEventDefaultVisibility by remember { mutableStateOf("members") }
    var editedPastEventsVisibility by remember { mutableStateOf(GROUP_PAST_EVENTS_ADMINS) }
    var editedAdminPermissions by remember { mutableStateOf(UserGroupAdminPermissions()) }
    var adminPermissionsExpanded by remember { mutableStateOf(false) }
    var isUploadingGroupImage by remember { mutableStateOf(false) }
    var pendingGroupCameraUri by remember { mutableStateOf<Uri?>(null) }
    var showDisableGroupInvitesDialog by remember { mutableStateOf(false) }
    var showInviteDialog by remember { mutableStateOf(false) }
    var showLeaveDialog by remember { mutableStateOf(false) }
    var showOwnerLeaveDialog by remember { mutableStateOf(false) }
    var showCreateEventScreen by remember { mutableStateOf(false) }
    var showCreateBulletinPostDialog by remember { mutableStateOf(false) }
    var showCreateBulletinEventScreen by remember { mutableStateOf(false) }
    var showBulletinInvitePromptDialog by remember { mutableStateOf(false) }
    var showExistingBulletinEventInvitePromptDialog by remember { mutableStateOf(false) }
    var showAllPostsDialog by remember { mutableStateOf(false) }
    var selectedBulletinPost by remember { mutableStateOf<UserGroupBulletinPost?>(null) }
    var selectedBulletinComments by remember { mutableStateOf<List<UserGroupBulletinComment>>(emptyList()) }
    var bulletinCommentDraft by remember { mutableStateOf("") }
    var bulletinExpanded by remember { mutableStateOf(true) }
    var membersExpanded by remember { mutableStateOf(true) }
    var bulletinTitle by remember { mutableStateOf("") }
    var bulletinDescription by remember { mutableStateOf("") }
    var bulletinImageUrl by remember { mutableStateOf("") }
    var bulletinLinkedEventId by remember { mutableStateOf("") }
    var bulletinLinkedEventTitle by remember { mutableStateOf("") }
    var bulletinLinkedEventOwnerUserId by remember { mutableStateOf("") }
    var bulletinLinkedEventGroupId by remember { mutableStateOf("") }
    var bulletinEventMenuExpanded by remember { mutableStateOf(false) }
    var pendingBulletinEventDraft by remember { mutableStateOf<PersonalEvent?>(null) }
    var isUploadingBulletinImage by remember { mutableStateOf(false) }
    var pendingBulletinCameraUri by remember { mutableStateOf<Uri?>(null) }
    var statusMessage by remember { mutableStateOf("") }

    val safeProfile = groupProfile ?: UserGroupProfile()
    val ownerUserId = safeProfile.ownerUserId
    val adminIds = safeProfile.adminIds
    val memberIds = safeProfile.memberIds

    DisposableEffect(userGroupId) {
        val registration =
            groupRef.addSnapshotListener { snapshot, _ ->
                groupLoaded = true
                if (snapshot?.exists() == true) {
                    groupProfile = snapshot.toUserGroupProfile()
                    invitedUserIdsState =
                        (snapshot.get("invitedUserIds") as? List<*>)
                            ?.mapNotNull { it as? String }
                            ?: emptyList()
                } else {
                    groupProfile = null
                    invitedUserIdsState = emptyList()
                }
            }

        onDispose { registration.remove() }
    }

    DisposableEffect(userGroupId) {
        val registration =
            groupRef.collection("events")
                .addSnapshotListener { snapshot, _ ->
                    groupEvents =
                        snapshot?.documents?.map { doc ->
                            doc.toPersonalEvent().copy(
                                id = doc.id,
                                ownerUserId =
                                    doc.getString("ownerUserId")
                                        .orEmpty()
                                        .ifBlank { doc.getString("createdByUserId").orEmpty() },
                                groupId = userGroupId,
                            )
                        } ?: emptyList()
                }

        onDispose { registration.remove() }
    }

    DisposableEffect(currentUserId) {
        if (currentUserId.isBlank()) {
            personalEvents = emptyList()
            onDispose { }
        } else {
            val registration =
                db.collection("users")
                    .document(currentUserId)
                    .collection("personalEvents")
                    .addSnapshotListener { snapshot, _ ->
                        personalEvents =
                            snapshot?.documents?.map { doc ->
                                doc.toPersonalEvent().copy(
                                    id = doc.id,
                                    ownerUserId =
                                        doc.getString("ownerUserId")
                                            .orEmpty()
                                            .ifBlank { currentUserId },
                                )
                            } ?: emptyList()
                    }

            onDispose { registration.remove() }
        }
    }

    DisposableEffect(userGroupId) {
        val registration =
            groupRef.collection("bulletinPosts")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, _ ->
                    bulletinPosts = snapshot?.documents?.map { it.toUserGroupBulletinPost() } ?: emptyList()
                }

        onDispose { registration.remove() }
    }

    DisposableEffect(selectedBulletinPost?.id) {
        val postId = selectedBulletinPost?.id
        if (postId.isNullOrBlank()) {
            selectedBulletinComments = emptyList()
            onDispose { }
        } else {
            val registration =
                groupRef.collection("bulletinPosts")
                    .document(postId)
                    .collection("comments")
                    .orderBy("createdAt", Query.Direction.ASCENDING)
                    .addSnapshotListener { snapshot, _ ->
                        selectedBulletinComments =
                            snapshot?.documents?.map { it.toUserGroupBulletinComment() } ?: emptyList()
                    }

            onDispose { registration.remove() }
        }
    }

    LaunchedEffect(memberIds) {
        fetchUsersByIds(db, memberIds) { loadedProfiles ->
            memberProfiles = loadedProfiles
        }
    }

    LaunchedEffect(currentUserId) {
        if (currentUserId.isBlank()) {
            currentUserLoaded = true
            currentUserFriendIds = emptyList()
            friendProfiles = emptyList()
            return@LaunchedEffect
        }

        db.collection("users")
            .document(currentUserId)
            .get()
            .addOnSuccessListener { userDoc ->
                currentUserName =
                    userDoc.getString("name")
                        .orEmpty()
                        .ifBlank { userDoc.getString("email").orEmpty() }
                        .ifBlank { Firebase.auth.currentUser?.email.orEmpty() }
                        .ifBlank { "Unknown user" }
                currentUserProfilePictureUrl = userDoc.getString("profilePictureUrl").orEmpty()

                val friendIds =
                    (userDoc.get("friends") as? List<*>)
                        ?.mapNotNull { it as? String }
                        ?: emptyList()

                currentUserFriendIds = friendIds
                currentUserLoaded = true

                fetchUsersByIds(db, friendIds) { loadedProfiles ->
                    friendProfiles = loadedProfiles
                }
            }.addOnFailureListener {
                currentUserLoaded = true
                currentUserFriendIds = emptyList()
                friendProfiles = emptyList()
            }
    }

    fun uploadGroupImage(uri: Uri) {
        isUploadingGroupImage = true

        val storageRef =
            storage.reference.child("group_pictures/$userGroupId/${System.currentTimeMillis()}.jpg")

        storageRef.putFile(uri)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                    editedGroupProfilePictureUrl = downloadUrl.toString()
                    isUploadingGroupImage = false
                }
            }.addOnFailureListener { error ->
                statusMessage = error.message ?: "Could not upload group image."
                isUploadingGroupImage = false
            }
    }

    fun uploadBulletinImage(uri: Uri) {
        isUploadingBulletinImage = true

        val storageRef =
            storage.reference.child("group_bulletin_images/$userGroupId/${System.currentTimeMillis()}.jpg")

        storageRef.putFile(uri)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                    bulletinImageUrl = downloadUrl.toString()
                    isUploadingBulletinImage = false
                }
            }.addOnFailureListener { error ->
                statusMessage = error.message ?: "Could not upload bulletin image."
                isUploadingBulletinImage = false
            }
    }

    val groupImagePickerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let(::uploadGroupImage)
        }

    val groupCameraLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { didCapture ->
            val uri = pendingGroupCameraUri
            if (didCapture && uri != null) {
                uploadGroupImage(uri)
            }
        }

    val bulletinImagePickerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let(::uploadBulletinImage)
        }

    val bulletinCameraLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { didCapture ->
            val uri = pendingBulletinCameraUri
            if (didCapture && uri != null) {
                uploadBulletinImage(uri)
            }
        }

    val isOwner = currentUserId == ownerUserId
    val isAdmin = adminIds.contains(currentUserId)
    val isMember = memberIds.contains(currentUserId)
    val adminPermissions = safeProfile.adminPermissions
    val canKickMembers = isOwner || (isAdmin && adminPermissions.canKickMembers)
    val canPromoteAdmins = isOwner || (isAdmin && adminPermissions.canPromoteAdmins)
    val canCreateGroupEvents = isOwner || (isAdmin && adminPermissions.canCreateEvents)
    val canSetGroupEventVisibility = isOwner || (isAdmin && adminPermissions.canSetEventVisibility)
    val canEditGroupProfile = isOwner || (isAdmin && adminPermissions.canEditGroupProfile)
    val canManageInviteSettings = isOwner || (isAdmin && adminPermissions.canManageInviteSettings)
    val canManagePermissionMenu = isOwner || (isAdmin && adminPermissions.canManagePermissionMenu)
    val canManageBulletinPosts = isOwner || (isAdmin && adminPermissions.canManageBulletinPosts)
    val canCreateBulletinPost =
        isOwner ||
            (isAdmin && adminPermissions.canPostToBulletin) ||
            (isMember && safeProfile.membersCanPostBulletin)
    val canInviteToGroup = isOwner || isAdmin || (safeProfile.membersCanInvite && isMember)
    val canSeeGroupProfile =
        groupProfile?.let { canViewGroupProfile(it, currentUserId, currentUserFriendIds) } ?: false
    val canSeePastEvents =
        groupProfile?.let { canViewPastGroupEvents(it, currentUserId) } ?: false
    val canSeeBulletinBoard =
        when (safeProfile.bulletinVisibility) {
            "members" -> isMember || isAdmin || isOwner
            "moderators" -> isOwner || (isAdmin && adminPermissions.canManageBulletinPosts)
            "owner" -> isOwner
            else -> canSeeGroupProfile
        }
    val canEditAnyGroupSettings = canEditGroupProfile || canManageInviteSettings || canManagePermissionMenu

    fun clearBulletinDraft() {
        bulletinTitle = ""
        bulletinDescription = ""
        bulletinImageUrl = ""
        bulletinLinkedEventId = ""
        bulletinLinkedEventTitle = ""
        bulletinLinkedEventOwnerUserId = ""
        bulletinLinkedEventGroupId = ""
        pendingBulletinEventDraft = null
    }

    fun inviteUser(user: UserProfile) {
        if (!canInviteToGroup || user.id.isBlank()) return

        val inviteId = "${ownerUserId}_${userGroupId}_${user.id}"
        val inviteRef = db.collection("userGroupInvites").document(inviteId)
        val batch = db.batch()

        batch.set(
            inviteRef,
            mapOf(
                "groupId" to userGroupId,
                "ownerUserId" to ownerUserId,
                "fromUserId" to currentUserId,
                "fromUserName" to currentUserName,
                "toUserId" to user.id,
                "userGroupName" to safeProfile.name,
                "status" to "pending",
                "createdAt" to System.currentTimeMillis(),
            ),
        )
        batch.update(groupRef, "invitedUserIds", FieldValue.arrayUnion(user.id))
        batch.commit().addOnSuccessListener {
            invitedUserIdsState = (invitedUserIdsState + user.id).distinct()
        }
    }

    fun disbandGroup() {
        if (!isOwner) return
        groupRef.delete().addOnSuccessListener { onBack() }
    }

    fun transferLeadershipAndLeave(newOwnerId: String) {
        if (!isOwner || newOwnerId.isBlank()) return

        val remainingMembers = memberIds.filterNot { it == currentUserId }
        val updatedAdmins =
            (adminIds + newOwnerId)
                .filterNot { it == currentUserId }
                .distinct()

        groupRef.update(
            mapOf(
                "ownerUserId" to newOwnerId,
                "memberIds" to remainingMembers,
                "adminIds" to updatedAdmins,
            ),
        ).addOnSuccessListener { onBack() }
    }

    fun leaveGroup() {
        if (currentUserId.isBlank() || !isMember || isOwner) return

        groupRef.update(
            mapOf(
                "memberIds" to FieldValue.arrayRemove(currentUserId),
                "adminIds" to FieldValue.arrayRemove(currentUserId),
            ),
        ).addOnSuccessListener { onBack() }
    }

    fun kickMember(memberId: String) {
        if (!canKickMembers || memberId.isBlank() || memberId == ownerUserId) return

        groupRef.update(
            mapOf(
                "memberIds" to FieldValue.arrayRemove(memberId),
                "adminIds" to FieldValue.arrayRemove(memberId),
                "invitedUserIds" to FieldValue.arrayRemove(memberId),
            ),
        )
    }

    fun promoteToAdmin(memberId: String) {
        if (!canPromoteAdmins || memberId.isBlank() || memberId == ownerUserId) return

        groupRef.update("adminIds", FieldValue.arrayUnion(memberId))
    }

    fun demoteAdmin(memberId: String) {
        if (!canPromoteAdmins || memberId.isBlank() || memberId == ownerUserId) return

        groupRef.update("adminIds", FieldValue.arrayRemove(memberId))
    }

    fun updateGroupProfile(removePeopleOutsideFriends: Boolean = false) {
        if (!canEditAnyGroupSettings || safeProfile.id.isBlank()) return

        val allowedUserIds =
            (currentUserFriendIds + currentUserId + ownerUserId)
                .filter { it.isNotBlank() }
                .toSet()

        val updatedMemberIds =
            if (removePeopleOutsideFriends) {
                memberIds.filter { it in allowedUserIds }
            } else {
                memberIds
            }
        val updatedAdminIds =
            if (removePeopleOutsideFriends) {
                adminIds.filter { it in allowedUserIds }
            } else {
                adminIds
            }
        val updatedInvitedUserIds =
            if (removePeopleOutsideFriends) {
                invitedUserIdsState.filter { it in allowedUserIds }
            } else {
                invitedUserIdsState
            }
        val removedUserIds =
            if (removePeopleOutsideFriends) {
                (memberIds + invitedUserIdsState)
                    .distinct()
                    .filterNot { it in allowedUserIds }
            } else {
                emptyList()
            }

        val updates = mutableMapOf<String, Any>()

        if (canEditGroupProfile) {
            updates["name"] = editedGroupName.trim()
            updates["description"] = editedGroupDescription.trim()
            updates["profilePictureUrl"] = editedGroupProfilePictureUrl.trim()
            updates["visibility"] = editedGroupVisibility
        }

        if (canManageInviteSettings) {
            updates["membersCanInvite"] = editedMembersCanInvite
        }

        if (canManagePermissionMenu) {
            updates["bulletinVisibility"] = editedBulletinVisibility
            updates["eventDefaultVisibility"] = editedEventDefaultVisibility
            updates["pastEventsVisibility"] = editedPastEventsVisibility
            updates["membersCanPostBulletin"] = editedMembersCanPostBulletin
        }

        if (isOwner) {
            updates["adminPermissions"] =
                mapOf(
                    "canKickMembers" to editedAdminPermissions.canKickMembers,
                    "canPromoteAdmins" to editedAdminPermissions.canPromoteAdmins,
                    "canCreateEvents" to editedAdminPermissions.canCreateEvents,
                    "canSetEventVisibility" to editedAdminPermissions.canSetEventVisibility,
                    "canEditGroupProfile" to editedAdminPermissions.canEditGroupProfile,
                    "canManageInviteSettings" to editedAdminPermissions.canManageInviteSettings,
                    "canManagePermissionMenu" to editedAdminPermissions.canManagePermissionMenu,
                    "canPostToBulletin" to editedAdminPermissions.canPostToBulletin,
                    "canManageBulletinPosts" to editedAdminPermissions.canManageBulletinPosts,
                )
            updates["adminsCanManageInvites"] = editedAdminPermissions.canManageInviteSettings
        }

        if (removePeopleOutsideFriends) {
            updates["memberIds"] = updatedMemberIds
            updates["adminIds"] = updatedAdminIds
            updates["invitedUserIds"] = updatedInvitedUserIds
        }

        if (updates.isEmpty()) return

        fun applySuccessfulUpdate() {
            groupProfile =
                safeProfile.copy(
                    name = if (canEditGroupProfile) editedGroupName.trim() else safeProfile.name,
                    description = if (canEditGroupProfile) editedGroupDescription.trim() else safeProfile.description,
                    profilePictureUrl =
                        if (canEditGroupProfile) {
                            editedGroupProfilePictureUrl.trim()
                        } else {
                            safeProfile.profilePictureUrl
                        },
                    visibility = if (canEditGroupProfile) editedGroupVisibility else safeProfile.visibility,
                    membersCanInvite = if (canManageInviteSettings) editedMembersCanInvite else safeProfile.membersCanInvite,
                    membersCanPostBulletin =
                        if (canManagePermissionMenu) {
                            editedMembersCanPostBulletin
                        } else {
                            safeProfile.membersCanPostBulletin
                        },
                    bulletinVisibility =
                        if (canManagePermissionMenu) {
                            editedBulletinVisibility
                        } else {
                            safeProfile.bulletinVisibility
                        },
                    eventDefaultVisibility =
                        if (canManagePermissionMenu) {
                            editedEventDefaultVisibility
                        } else {
                            safeProfile.eventDefaultVisibility
                        },
                    pastEventsVisibility =
                        if (canManagePermissionMenu) {
                            editedPastEventsVisibility
                        } else {
                            safeProfile.pastEventsVisibility
                        },
                    adminPermissions =
                        if (isOwner) {
                            editedAdminPermissions
                        } else {
                            safeProfile.adminPermissions
                        },
                    adminsCanManageInvites =
                        if (isOwner) {
                            editedAdminPermissions.canManageInviteSettings
                        } else {
                            safeProfile.adminsCanManageInvites
                        },
                    memberIds = if (removePeopleOutsideFriends) updatedMemberIds else safeProfile.memberIds,
                    adminIds = if (removePeopleOutsideFriends) updatedAdminIds else safeProfile.adminIds,
                )
            invitedUserIdsState = if (removePeopleOutsideFriends) updatedInvitedUserIds else invitedUserIdsState
            showEditGroupDialog = false
        }

        if (!removePeopleOutsideFriends || removedUserIds.isEmpty()) {
            groupRef.update(updates).addOnSuccessListener { applySuccessfulUpdate() }
                .addOnFailureListener { error ->
                    statusMessage = error.message ?: "Could not update group."
                }
            return
        }

        db.collection("userGroupInvites")
            .whereEqualTo("groupId", userGroupId)
            .get()
            .addOnSuccessListener { result ->
                val batch = db.batch()
                batch.update(groupRef, updates)
                result.documents
                    .filter { inviteDoc -> removedUserIds.contains(inviteDoc.getString("toUserId").orEmpty()) }
                    .forEach { inviteDoc ->
                        batch.update(
                            inviteDoc.reference,
                            mapOf(
                                "status" to "removed",
                                "updatedAt" to System.currentTimeMillis(),
                            ),
                        )
                    }
                batch.commit().addOnSuccessListener { applySuccessfulUpdate() }
                    .addOnFailureListener { error ->
                        statusMessage = error.message ?: "Could not update group."
                    }
            }
    }

    fun createGroupEvent(
        newEvent: PersonalEvent,
        inviteAllGroupMembers: Boolean = true,
        onCreated: ((String, String) -> Unit)? = null,
    ) {
        if (!canCreateGroupEvents) return

        val inviteeIds =
            if (inviteAllGroupMembers) {
                memberIds.filter { it.isNotBlank() && it != currentUserId }
            } else {
                emptyList()
            }

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
                "inviteesCanInvite" to newEvent.inviteesCanInvite,
                "attendeeVisibility" to newEvent.attendeeVisibility,
                "visibility" to newEvent.visibility,
                "createdAt" to System.currentTimeMillis(),
                "ownerUserId" to currentUserId,
                "invitedUserIds" to inviteeIds,
            )

        val eventRef = groupRef.collection("events").document()
        val batch = db.batch()
        batch.set(eventRef, eventData)
        inviteeIds.forEach { inviteeId ->
            val inviteRef = db.collection("eventInvites").document("${currentUserId}_${eventRef.id}_$inviteeId")
            batch.set(
                inviteRef,
                mapOf(
                    "eventId" to eventRef.id,
                    "ownerUserId" to currentUserId,
                    "groupId" to userGroupId,
                    "fromUserId" to currentUserId,
                    "toUserId" to inviteeId,
                    "eventTitle" to newEvent.title,
                    "status" to "pending",
                    "createdAt" to System.currentTimeMillis(),
                ),
            )
        }

        batch.commit()
            .addOnSuccessListener {
                showCreateEventScreen = false
                onCreated?.invoke(eventRef.id, newEvent.title)
            }
            .addOnFailureListener { error ->
                statusMessage = error.message ?: "Could not create group event."
            }
    }

    fun createBulletinLinkedEvent(
        newEvent: PersonalEvent,
        inviteAllGroupMembers: Boolean,
        onCreated: (String, String) -> Unit,
    ) {
        if (!canCreateBulletinPost || currentUserId.isBlank()) return

        val eventId = db.collection("users").document(currentUserId).collection("personalEvents").document().id
        val now = System.currentTimeMillis()
        val invitedIds =
            if (inviteAllGroupMembers) {
                memberIds.filter { it.isNotBlank() && it != currentUserId }
            } else {
                emptyList()
            }
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
                "inviteesCanInvite" to newEvent.inviteesCanInvite,
                "attendeeVisibility" to newEvent.attendeeVisibility,
                "visibility" to newEvent.visibility,
                "createdAt" to now,
                "ownerUserId" to currentUserId,
                "invitedUserIds" to invitedIds,
            )

        val personalEventRef =
            db.collection("users")
                .document(currentUserId)
                .collection("personalEvents")
                .document(eventId)
        val groupEventRef = groupRef.collection("events").document(eventId)
        val batch = db.batch()

        batch.set(personalEventRef, eventData)
        batch.set(groupEventRef, eventData)
        invitedIds.forEach { inviteeId ->
            val inviteRef = db.collection("eventInvites").document("${currentUserId}_${eventId}_$inviteeId")
            batch.set(
                inviteRef,
                mapOf(
                    "eventId" to eventId,
                    "ownerUserId" to currentUserId,
                    "groupId" to userGroupId,
                    "fromUserId" to currentUserId,
                    "toUserId" to inviteeId,
                    "eventTitle" to newEvent.title,
                    "status" to "pending",
                    "createdAt" to now,
                ),
            )
        }
        batch.commit()
            .addOnSuccessListener {
                onCreated(eventId, newEvent.title)
            }
            .addOnFailureListener { error ->
                statusMessage = error.message ?: "Could not create event for bulletin post."
                showCreateBulletinPostDialog = true
            }
    }

    fun publishBulletinPost() {
        val now = System.currentTimeMillis()
        groupRef.collection("bulletinPosts")
            .add(
                mapOf(
                    "title" to bulletinTitle.trim(),
                    "description" to bulletinDescription.trim(),
                    "authorUserId" to currentUserId,
                    "authorName" to currentUserName,
                    "authorProfilePictureUrl" to currentUserProfilePictureUrl,
                    "imageUrl" to bulletinImageUrl,
                    "linkedEventId" to bulletinLinkedEventId,
                    "linkedEventOwnerUserId" to bulletinLinkedEventOwnerUserId,
                    "linkedEventGroupId" to bulletinLinkedEventGroupId,
                    "linkedEventTitle" to bulletinLinkedEventTitle,
                    "createdAt" to now,
                    "updatedAt" to now,
                ),
            )
            .addOnSuccessListener {
                clearBulletinDraft()
                showCreateBulletinPostDialog = false
            }
            .addOnFailureListener { error ->
                statusMessage = error.message ?: "Could not publish bulletin post."
            }
    }

    fun inviteGroupMembersToExistingEvent(
        event: PersonalEvent,
        onFinished: () -> Unit,
    ) {
        val inviteeIds =
            memberIds
                .filter { it.isNotBlank() && it != currentUserId && !event.invitedUserIds.contains(it) && !event.attendeeIds.contains(it) }
                .distinct()

        if (inviteeIds.isEmpty()) {
            onFinished()
            return
        }

        val ownerId = event.ownerUserId.ifBlank { currentUserId }
        val eventRef =
            if (event.groupId.isNotBlank()) {
                db.collection("userGroups").document(event.groupId).collection("events").document(event.id)
            } else {
                db.collection("users").document(ownerId).collection("personalEvents").document(event.id)
            }

        val batch = db.batch()
        batch.update(eventRef, "invitedUserIds", FieldValue.arrayUnion(*inviteeIds.toTypedArray()))

        inviteeIds.forEach { inviteeId ->
            val inviteRef = db.collection("eventInvites").document("${ownerId}_${event.id}_$inviteeId")
            batch.set(
                inviteRef,
                mapOf(
                    "eventId" to event.id,
                    "ownerUserId" to ownerId,
                    "groupId" to event.groupId.ifBlank { userGroupId },
                    "fromUserId" to currentUserId,
                    "toUserId" to inviteeId,
                    "eventTitle" to event.title,
                    "status" to "pending",
                    "createdAt" to System.currentTimeMillis(),
                ),
            )
        }

        batch.commit()
            .addOnSuccessListener { onFinished() }
            .addOnFailureListener { error ->
                statusMessage = error.message ?: "Could not invite group members to this event."
            }
    }

    fun saveBulletinPost() {
        if (!canCreateBulletinPost || bulletinTitle.isBlank()) return

        val linkedEvent =
            (personalEvents + groupEvents)
                .distinctBy { it.id }
                .firstOrNull { it.id == bulletinLinkedEventId }
        if (linkedEvent != null && linkedEvent.isPastEvent()) {
            statusMessage = "Past events cannot be attached to bulletin posts."
            return
        }

        if (linkedEvent != null && linkedEvent.ownerUserId != currentUserId && !linkedEvent.inviteesCanInvite) {
            statusMessage = "Only the event owner can attach that event because invitees cannot invite friends."
            return
        }

        if (linkedEvent != null) {
            showExistingBulletinEventInvitePromptDialog = true
            return
        }

        publishBulletinPost()
    }

    fun saveBulletinComment(post: UserGroupBulletinPost) {
        if (currentUserId.isBlank() || bulletinCommentDraft.isBlank()) return

        groupRef.collection("bulletinPosts")
            .document(post.id)
            .collection("comments")
            .add(
                mapOf(
                    "authorUserId" to currentUserId,
                    "authorName" to currentUserName,
                    "authorProfilePictureUrl" to currentUserProfilePictureUrl,
                    "body" to bulletinCommentDraft.trim(),
                    "createdAt" to System.currentTimeMillis(),
                ),
            )
            .addOnSuccessListener {
                bulletinCommentDraft = ""
            }
            .addOnFailureListener { error ->
                statusMessage = error.message ?: "Could not post comment."
            }
    }

    fun deleteBulletinPost(post: UserGroupBulletinPost) {
        if (!(canManageBulletinPosts || post.authorUserId == currentUserId)) return

        groupRef.collection("bulletinPosts")
            .document(post.id)
            .delete()
            .addOnSuccessListener {
                if (selectedBulletinPost?.id == post.id) {
                    selectedBulletinPost = null
                }
            }
            .addOnFailureListener { error ->
                statusMessage = error.message ?: "Could not remove bulletin post."
            }
    }

    if (!groupLoaded || !currentUserLoaded) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(48.dp))
            CircularProgressIndicator()
        }
        return
    }

    if (groupProfile == null) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(16.dp),
        ) {
            Text("This group could not be found.")
        }
        return
    }

    if (!canSeeGroupProfile) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(16.dp),
        ) {
            Text("This group profile is private.")
        }
        return
    }

    val nowMillis = System.currentTimeMillis()
    val visibleGroupEvents =
        groupEvents.filter { event ->
            canViewEvent(
                event = event,
                currentUserId = currentUserId,
                currentUserFriendIds = currentUserFriendIds,
                groupProfile = safeProfile,
            )
        }
    val upcomingGroupEvents =
        visibleGroupEvents
            .filterNot { it.isPastEvent(nowMillis) }
            .sortedBy { it.eventSortMillis() }
    val pastGroupEvents =
        if (canSeePastEvents) {
            visibleGroupEvents
                .filter { it.isPastEvent(nowMillis) }
                .sortedByDescending { it.eventSortMillis() }
        } else {
            emptyList()
        }
    val bulletinPreviewPosts = bulletinPosts.take(5)
    val attachableEvents =
        (personalEvents + upcomingGroupEvents + pastGroupEvents)
            .distinctBy { it.id }
            .filter { it.id.isNotBlank() }
            .filterNot { it.isPastEvent(nowMillis) }
            .filter { event ->
                event.ownerUserId == currentUserId || event.inviteesCanInvite
            }
    val canShowManageGroup = canEditAnyGroupSettings || canInviteToGroup || canCreateGroupEvents || isMember

    if (showCreateBulletinEventScreen) {
        CreatePersonalEventScreen(
            visibilityTitle = "Group Event Visibility",
            visibilityOptions = GroupEventVisibilityOptions,
            defaultVisibility = safeProfile.eventDefaultVisibility,
            canEditVisibility = isOwner || canSetGroupEventVisibility,
            onSave = { newEvent ->
                pendingBulletinEventDraft = newEvent
                showCreateBulletinEventScreen = false
                showBulletinInvitePromptDialog = true
            },
            onCancel = {
                showCreateBulletinEventScreen = false
                pendingBulletinEventDraft = null
                showCreateBulletinPostDialog = true
            },
        )
        return
    }

    if (showCreateEventScreen) {
        CreatePersonalEventScreen(
            visibilityTitle = "Group Event Visibility",
            visibilityOptions = GroupEventVisibilityOptions,
            defaultVisibility = safeProfile.eventDefaultVisibility,
            canEditVisibility = isOwner || canSetGroupEventVisibility,
            onSave = { newEvent -> createGroupEvent(newEvent) },
            onCancel = { showCreateEventScreen = false },
        )
        return
    }

    if (showInviteDialog) {
        AlertDialog(
            onDismissRequest = { showInviteDialog = false },
            title = { Text("Invite Friends") },
            text = {
                Column {
                    val inviteOptions =
                        friendProfiles.filterNot { memberIds.contains(it.id) || invitedUserIdsState.contains(it.id) }

                    if (inviteOptions.isEmpty()) {
                        Text("No friends available to invite.")
                    } else {
                        inviteOptions.forEach { friend ->
                            RippleOutlinedButton(
                                text = "Invite ${friend.name.ifBlank { friend.email.ifBlank { friend.id } }}",
                                onClick = { inviteUser(friend) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(modifier = Modifier.height(8.dp))
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

    if (showCreateBulletinPostDialog) {
        AlertDialog(
            onDismissRequest = {
                showCreateBulletinPostDialog = false
                clearBulletinDraft()
            },
            title = { Text("Create Bulletin Post") },
            text = {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .sizeIn(maxHeight = 520.dp)
                            .verticalScroll(rememberScrollState()),
                ) {
                    OutlinedTextField(
                        value = bulletinTitle,
                        onValueChange = { bulletinTitle = it },
                        label = { Text("Post title") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = bulletinDescription,
                        onValueChange = { bulletinDescription = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    ImageUploadControls(
                        imageUrl = bulletinImageUrl,
                        isUploading = isUploadingBulletinImage,
                        onChooseFromLibrary = { bulletinImagePickerLauncher.launch("image/*") },
                        onUseCamera = {
                            val uri = createImageCaptureUri(context, "group_bulletin_images")
                            pendingBulletinCameraUri = uri
                            bulletinCameraLauncher.launch(uri)
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Attach Event Link",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Box(modifier = Modifier.fillMaxWidth()) {
                        RippleOutlinedButton(
                            text = if (bulletinLinkedEventTitle.isBlank()) "Choose Existing Event" else bulletinLinkedEventTitle,
                            onClick = { bulletinEventMenuExpanded = true },
                            modifier = Modifier.fillMaxWidth(),
                        )

                        DropdownMenu(
                            expanded = bulletinEventMenuExpanded,
                            onDismissRequest = { bulletinEventMenuExpanded = false },
                        ) {
                            if (attachableEvents.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("No events available") },
                                    onClick = { bulletinEventMenuExpanded = false },
                                )
                            } else {
                                attachableEvents.forEach { event ->
                                DropdownMenuItem(
                                    text = { Text(event.title.ifBlank { "Untitled Event" }) },
                                    onClick = {
                                        bulletinLinkedEventId = event.id
                                        bulletinLinkedEventTitle = event.title
                                        bulletinLinkedEventOwnerUserId = event.ownerUserId
                                        bulletinLinkedEventGroupId = event.groupId
                                        bulletinEventMenuExpanded = false
                                    },
                                )
                            }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    RippleButton(
                        text = "Create and Attach New Event",
                        onClick = {
                            showCreateBulletinPostDialog = false
                            showCreateBulletinEventScreen = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    if (bulletinLinkedEventId.isNotBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        BulletinEventAttachmentSummary(
                            title = bulletinLinkedEventTitle,
                            onClear = {
                                bulletinLinkedEventId = ""
                                bulletinLinkedEventTitle = ""
                                bulletinLinkedEventOwnerUserId = ""
                                bulletinLinkedEventGroupId = ""
                            },
                        )
                    }
                }
            },
            confirmButton = {
                RippleButton(
                    text = "Publish",
                    onClick = { saveBulletinPost() },
                    enabled = bulletinTitle.isNotBlank() && !isUploadingBulletinImage,
                )
            },
            dismissButton = {
                RippleOutlinedButton(
                    text = "Cancel",
                    onClick = {
                        showCreateBulletinPostDialog = false
                        clearBulletinDraft()
                    },
                )
            },
        )
    }

    if (showBulletinInvitePromptDialog) {
        AlertDialog(
            onDismissRequest = {
                showBulletinInvitePromptDialog = false
                pendingBulletinEventDraft = null
                showCreateBulletinPostDialog = true
            },
            title = { Text("Invite Group Members") },
            text = { Text("Invite all group members to the new event before attaching it to the bulletin post?") },
            confirmButton = {
                RippleButton(
                    text = "Invite Everyone",
                    onClick = {
                        val draft = pendingBulletinEventDraft ?: return@RippleButton
                        showBulletinInvitePromptDialog = false
                        createBulletinLinkedEvent(draft, inviteAllGroupMembers = true) { eventId, eventTitle ->
                            bulletinLinkedEventId = eventId
                            bulletinLinkedEventTitle = eventTitle
                            bulletinLinkedEventOwnerUserId = currentUserId
                            bulletinLinkedEventGroupId = userGroupId
                            pendingBulletinEventDraft = null
                            showCreateBulletinPostDialog = true
                        }
                    },
                )
            },
            dismissButton = {
                RippleOutlinedButton(
                    text = "Skip Invites",
                    onClick = {
                        val draft = pendingBulletinEventDraft ?: return@RippleOutlinedButton
                        showBulletinInvitePromptDialog = false
                        createBulletinLinkedEvent(draft, inviteAllGroupMembers = false) { eventId, eventTitle ->
                            bulletinLinkedEventId = eventId
                            bulletinLinkedEventTitle = eventTitle
                            bulletinLinkedEventOwnerUserId = currentUserId
                            bulletinLinkedEventGroupId = userGroupId
                            pendingBulletinEventDraft = null
                            showCreateBulletinPostDialog = true
                        }
                    },
                )
            },
        )
    }

    if (showExistingBulletinEventInvitePromptDialog) {
        val linkedEvent =
            (personalEvents + groupEvents)
                .distinctBy { it.id }
                .firstOrNull { it.id == bulletinLinkedEventId }

        AlertDialog(
            onDismissRequest = { showExistingBulletinEventInvitePromptDialog = false },
            title = { Text("Invite Group Members") },
            text = {
                Text(
                    "Invite all group members to ${linkedEvent?.title?.ifBlank { "this event" } ?: "this event"} before publishing the bulletin post?"
                )
            },
            confirmButton = {
                RippleButton(
                    text = "Invite Everyone",
                    onClick = {
                        val event = linkedEvent ?: run {
                            showExistingBulletinEventInvitePromptDialog = false
                            publishBulletinPost()
                            return@RippleButton
                        }
                        showExistingBulletinEventInvitePromptDialog = false
                        inviteGroupMembersToExistingEvent(event) {
                            publishBulletinPost()
                        }
                    },
                )
            },
            dismissButton = {
                RippleOutlinedButton(
                    text = "Skip Invites",
                    onClick = {
                        showExistingBulletinEventInvitePromptDialog = false
                        publishBulletinPost()
                    },
                )
            },
        )
    }

    if (showEditGroupDialog && !showDisableGroupInvitesDialog) {
        AlertDialog(
            onDismissRequest = { showEditGroupDialog = false },
            title = { Text("Edit Group") },
            text = {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .sizeIn(maxHeight = 520.dp)
                            .verticalScroll(rememberScrollState()),
                ) {
                    if (canEditGroupProfile) {
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

                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    if (canManagePermissionMenu) {
                        VisibilitySelector(
                            title = "Bulletin Board Visibility",
                            selectedValue = editedBulletinVisibility,
                            options = BulletinBoardVisibilityOptions,
                            onValueChange = { editedBulletinVisibility = it },
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        VisibilitySelector(
                            title = "Default Group Event Visibility",
                            selectedValue = editedEventDefaultVisibility,
                            options = GroupEventVisibilityOptions,
                            onValueChange = { editedEventDefaultVisibility = it },
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        VisibilitySelector(
                            title = "Past Event Visibility",
                            selectedValue = editedPastEventsVisibility,
                            options = PastGroupEventsVisibilityOptions,
                            onValueChange = { editedPastEventsVisibility = it },
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        LabeledSwitchRow(
                            label = "Members can post to bulletin board",
                            checked = editedMembersCanPostBulletin,
                            onCheckedChange = { editedMembersCanPostBulletin = it },
                        )

                        if (isOwner) {
                            Spacer(modifier = Modifier.height(8.dp))

                            TextButton(
                                onClick = { adminPermissionsExpanded = !adminPermissionsExpanded },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = "Admin Permissions",
                                        style = MaterialTheme.typography.titleSmall,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Icon(
                                        imageVector =
                                            if (adminPermissionsExpanded) {
                                                Icons.Default.KeyboardArrowUp
                                            } else {
                                                Icons.Default.KeyboardArrowDown
                                            },
                                        contentDescription = null,
                                    )
                                }
                            }

                            if (adminPermissionsExpanded) {
                                PermissionToggleRow(
                                    label = "Admins can kick members",
                                    checked = editedAdminPermissions.canKickMembers,
                                    onCheckedChange = {
                                        editedAdminPermissions = editedAdminPermissions.copy(canKickMembers = it)
                                    },
                                )
                                PermissionToggleRow(
                                    label = "Admins can promote admins",
                                    checked = editedAdminPermissions.canPromoteAdmins,
                                    onCheckedChange = {
                                        editedAdminPermissions = editedAdminPermissions.copy(canPromoteAdmins = it)
                                    },
                                )
                                PermissionToggleRow(
                                    label = "Admins can create events",
                                    checked = editedAdminPermissions.canCreateEvents,
                                    onCheckedChange = {
                                        editedAdminPermissions = editedAdminPermissions.copy(canCreateEvents = it)
                                    },
                                )
                                PermissionToggleRow(
                                    label = "Admins can set event visibility",
                                    checked = editedAdminPermissions.canSetEventVisibility,
                                    onCheckedChange = {
                                        editedAdminPermissions = editedAdminPermissions.copy(canSetEventVisibility = it)
                                    },
                                )
                                PermissionToggleRow(
                                    label = "Admins can edit group profile",
                                    checked = editedAdminPermissions.canEditGroupProfile,
                                    onCheckedChange = {
                                        editedAdminPermissions = editedAdminPermissions.copy(canEditGroupProfile = it)
                                    },
                                )
                                PermissionToggleRow(
                                    label = "Admins can manage invite settings",
                                    checked = editedAdminPermissions.canManageInviteSettings,
                                    onCheckedChange = {
                                        editedAdminPermissions = editedAdminPermissions.copy(canManageInviteSettings = it)
                                    },
                                )
                                PermissionToggleRow(
                                    label = "Admins can manage permission menu",
                                    checked = editedAdminPermissions.canManagePermissionMenu,
                                    onCheckedChange = {
                                        editedAdminPermissions = editedAdminPermissions.copy(canManagePermissionMenu = it)
                                    },
                                )
                                PermissionToggleRow(
                                    label = "Admins can post to bulletin board",
                                    checked = editedAdminPermissions.canPostToBulletin,
                                    onCheckedChange = {
                                        editedAdminPermissions = editedAdminPermissions.copy(canPostToBulletin = it)
                                    },
                                )
                                PermissionToggleRow(
                                    label = "Admins can manage bulletin posts",
                                    checked = editedAdminPermissions.canManageBulletinPosts,
                                    onCheckedChange = {
                                        editedAdminPermissions = editedAdminPermissions.copy(canManageBulletinPosts = it)
                                    },
                                )
                            }
                        }
                    }

                    if (canManageInviteSettings) {
                        Spacer(modifier = Modifier.height(12.dp))
                        LabeledSwitchRow(
                            label = "Members can invite friends",
                            checked = editedMembersCanInvite,
                            onCheckedChange = { editedMembersCanInvite = it },
                        )
                    }
                }
            },
            confirmButton = {
                RippleButton(
                    text = "Save",
                    onClick = {
                        if (canManageInviteSettings && safeProfile.membersCanInvite && !editedMembersCanInvite) {
                            showDisableGroupInvitesDialog = true
                        } else {
                            updateGroupProfile()
                        }
                    },
                    enabled =
                        (if (canEditGroupProfile) editedGroupName.isNotBlank() else true) &&
                            !isUploadingGroupImage,
                )
            },
            dismissButton = {
                RippleOutlinedButton(text = "Cancel", onClick = { showEditGroupDialog = false })
            },
        )
    }

    if (showDisableGroupInvitesDialog) {
        AlertDialog(
            onDismissRequest = { showDisableGroupInvitesDialog = false },
            title = { Text("Disable Open Invites") },
            text = { Text("Remove members and pending invitees who are not in your friends list?") },
            confirmButton = {
                RippleButton(
                    text = "Remove Non-Friends",
                    onClick = {
                        showDisableGroupInvitesDialog = false
                        updateGroupProfile(removePeopleOutsideFriends = true)
                    },
                )
            },
            dismissButton = {
                RippleOutlinedButton(
                    text = "Keep Everyone",
                    onClick = {
                        showDisableGroupInvitesDialog = false
                        updateGroupProfile(removePeopleOutsideFriends = false)
                    },
                )
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
                            RippleOutlinedButton(
                                text = member.name.ifBlank { member.email.ifBlank { member.id } },
                                onClick = { transferLeadershipAndLeave(member.id) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            },
            confirmButton = {
                RippleButton(text = "Disband Group", onClick = { disbandGroup() })
            },
            dismissButton = {
                RippleOutlinedButton(text = "Cancel", onClick = { showOwnerLeaveDialog = false })
            },
        )
    }

    if (showLeaveDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveDialog = false },
            title = { Text("Leave Group") },
            text = { Text("Are you sure you want to leave ${safeProfile.name}?") },
            confirmButton = {
                RippleButton(text = "Leave", onClick = { leaveGroup() })
            },
            dismissButton = {
                RippleOutlinedButton(text = "Cancel", onClick = { showLeaveDialog = false })
            },
        )
    }

    selectedBulletinPost?.let { post ->
        BulletinPostProfileScreen(
            groupName = safeProfile.name,
            post = post,
            comments = selectedBulletinComments,
            commentDraft = bulletinCommentDraft,
            onCommentDraftChange = { bulletinCommentDraft = it },
            onBack = { selectedBulletinPost = null },
            onOpenEvent = {
                onOpenEventProfile(
                    post.linkedEventId,
                    post.linkedEventOwnerUserId,
                    post.linkedEventGroupId,
                )
            },
            onPostComment = { saveBulletinComment(post) },
            onDeletePost = if (canManageBulletinPosts || post.authorUserId == currentUserId) {
                {
                    deleteBulletinPost(post)
                    selectedBulletinPost = null
                }
            } else {
                null
            },
        )
        return
    }

    if (showAllPostsDialog) {
        AlertDialog(
            onDismissRequest = { showAllPostsDialog = false },
            title = { Text("All Bulletin Posts") },
            text = {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .sizeIn(maxHeight = 520.dp)
                            .verticalScroll(rememberScrollState()),
                ) {
                    if (bulletinPosts.isEmpty()) {
                        Text("No bulletin posts yet.")
                    } else {
                        bulletinPosts.forEach { post ->
                            BulletinPostCard(
                                post = post,
                                onClick = {
                                    showAllPostsDialog = false
                                    selectedBulletinPost = post
                                },
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAllPostsDialog = false }) {
                    Text("Done")
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
            title = safeProfile.name.ifBlank { "Unknown group" },
            imageUrl = safeProfile.profilePictureUrl,
            placeholderIcon = Icons.Default.AccountBox,
            subtitle = {
                Text(
                    text = "${memberIds.size} members - ${safeProfile.visibility.replaceFirstChar { it.uppercase() }}",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            actions = {
                if (isMember) {
                    RippleOutlinedButton(
                        text = "Leave",
                        onClick = {
                            if (isOwner) showOwnerLeaveDialog = true else showLeaveDialog = true
                        },
                    )
                }
            },
        )

        if (safeProfile.description.isNotBlank()) {
            Text(safeProfile.description, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (statusMessage.isNotBlank()) {
            Text(statusMessage, color = MaterialTheme.colorScheme.secondary)
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (canSeeBulletinBoard) {
            CollapsibleSection(
                title = "Bulletin Board",
                expanded = bulletinExpanded,
                onToggle = { bulletinExpanded = !bulletinExpanded },
            ) {
                if (canCreateBulletinPost) {
                    RippleButton(
                        text = "Create Bulletin Post",
                        onClick = {
                            if (!showCreateBulletinPostDialog) {
                                clearBulletinDraft()
                            }
                            showCreateBulletinPostDialog = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                }

                if (bulletinPreviewPosts.isEmpty()) {
                    Text("No bulletin posts yet.")
                } else {
                    bulletinPreviewPosts.forEach { post ->
                        BulletinPostCard(
                            post = post,
                            onClick = { selectedBulletinPost = post },
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    RippleOutlinedButton(
                        text = "View All Posts",
                        onClick = { showAllPostsDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        } else {
            Text(
                text = "The bulletin board is restricted to group moderators.",
                color = MaterialTheme.colorScheme.secondary,
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Upcoming Group Events", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        if (upcomingGroupEvents.isEmpty()) {
            Text("No upcoming group events.")
        } else {
            upcomingGroupEvents.forEach { event ->
                PersonalEventCard(
                    event = event,
                    onClick = { onOpenEventProfile(event.id, event.ownerUserId, userGroupId) },
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Past Group Events", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        when {
            !canSeePastEvents -> Text("Past events are restricted to group leadership.")
            pastGroupEvents.isEmpty() -> Text("No past group events.")
            else -> {
                pastGroupEvents.forEach { event ->
                    PersonalEventCard(
                        event = event,
                        onClick = { onOpenEventProfile(event.id, event.ownerUserId, userGroupId) },
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        CollapsibleSection(
            title = "Members",
            expanded = membersExpanded,
            onToggle = { membersExpanded = !membersExpanded },
        ) {
            memberProfiles.forEach { member ->
                val role =
                    when {
                        member.id == ownerUserId -> "Owner"
                        adminIds.contains(member.id) -> "Admin"
                        else -> "Member"
                    }

                val memberActions = buildList {
                    if (canPromoteAdmins && member.id != currentUserId && member.id != ownerUserId) {
                        if (adminIds.contains(member.id)) {
                            add(
                                UserActionMenuItem(
                                    label = "Remove Admin",
                                    onClick = { demoteAdmin(member.id) },
                                ),
                            )
                        } else {
                            add(
                                UserActionMenuItem(
                                    label = "Make Admin",
                                    onClick = { promoteToAdmin(member.id) },
                                ),
                            )
                        }
                    }

                    if (canKickMembers && member.id != currentUserId && member.id != ownerUserId) {
                        add(
                            UserActionMenuItem(
                                label = "Kick from Group",
                                onClick = { kickMember(member.id) },
                                destructive = true,
                            ),
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RippleOutlinedButton(
                        text = "${member.name.ifBlank { member.email.ifBlank { member.id } }} - $role",
                        onClick = { onOpenUserProfile(member.id) },
                        modifier = Modifier.weight(1f),
                    )

                    if (memberActions.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        UserActionMenuButton(actions = memberActions)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        if (canShowManageGroup) {
            Spacer(modifier = Modifier.height(24.dp))
            Text("Manage Group", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (canEditAnyGroupSettings) {
            RippleOutlinedButton(
                text = "Edit Group",
                onClick = {
                    editedGroupName = safeProfile.name
                    editedGroupDescription = safeProfile.description
                    editedGroupProfilePictureUrl = safeProfile.profilePictureUrl
                    editedGroupVisibility = safeProfile.visibility
                    editedMembersCanInvite = safeProfile.membersCanInvite
                    editedMembersCanPostBulletin = safeProfile.membersCanPostBulletin
                    editedBulletinVisibility = safeProfile.bulletinVisibility
                    editedEventDefaultVisibility = safeProfile.eventDefaultVisibility
                    editedPastEventsVisibility = safeProfile.pastEventsVisibility
                    editedAdminPermissions = safeProfile.adminPermissions
                    adminPermissionsExpanded = false
                    showEditGroupDialog = true
                },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(8.dp))
        }

        if (canInviteToGroup) {
            RippleButton(
                text = "Invite Friends",
                onClick = { showInviteDialog = true },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(8.dp))
        }

        if (canCreateGroupEvents) {
            RippleButton(
                text = "Create Group Event",
                onClick = { showCreateEventScreen = true },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun BulletinPostProfileScreen(
    groupName: String,
    post: UserGroupBulletinPost,
    comments: List<UserGroupBulletinComment>,
    commentDraft: String,
    onCommentDraftChange: (String) -> Unit,
    onBack: () -> Unit,
    onOpenEvent: () -> Unit,
    onPostComment: () -> Unit,
    onDeletePost: (() -> Unit)?,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
    ) {
        RippleOutlinedButton(
            text = "Back to $groupName",
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(16.dp))

        ProfileHeader(
            title = post.title.ifBlank { "Untitled Post" },
            imageUrl = post.imageUrl,
            placeholderIcon = Icons.AutoMirrored.Filled.Article,
            subtitle = {
                Text(
                    text = "Posted by ${post.authorName.ifBlank { "Unknown member" }}",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
        )

        Text(
            text = post.description.ifBlank { "No details provided." },
            style = MaterialTheme.typography.bodyMedium,
        )

        if (post.linkedEventId.isNotBlank()) {
            Spacer(modifier = Modifier.height(16.dp))
            RippleOutlinedButton(
                text = "Open Event: ${post.linkedEventTitle.ifBlank { "Linked Event" }}",
                onClick = onOpenEvent,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (onDeletePost != null) {
            Spacer(modifier = Modifier.height(16.dp))
            RippleButton(
                text = "Delete Post",
                onClick = onDeletePost,
                modifier = Modifier.fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Comments", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = commentDraft,
            onValueChange = onCommentDraftChange,
            label = { Text("Add a comment") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
        )

        Spacer(modifier = Modifier.height(8.dp))

        RippleButton(
            text = "Post Comment",
            onClick = onPostComment,
            modifier = Modifier.fillMaxWidth(),
            enabled = commentDraft.isNotBlank(),
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (comments.isEmpty()) {
            Text("No comments yet.")
        } else {
            comments.forEach { comment ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = comment.authorName.ifBlank { "Unknown member" },
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = comment.body,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun BulletinPostCard(
    post: UserGroupBulletinPost,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = post.title.ifBlank { "Untitled Post" },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = post.description.ifBlank { "No description provided." },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Posted by ${post.authorName.ifBlank { "Unknown member" }}",
                style = MaterialTheme.typography.labelMedium,
            )
            if (post.imageUrl.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Includes image",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            if (post.linkedEventId.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Event,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = post.linkedEventTitle.ifBlank { "Linked Event" },
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun BulletinEventAttachmentSummary(
    title: String,
    onClear: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Article,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title.ifBlank { "Linked Event" },
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            RippleOutlinedButton(
                text = "Remove Event Link",
                onClick = onClear,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun PermissionToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }

    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun LabeledSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

private fun fetchUsersByIds(
    db: FirebaseFirestore,
    ids: List<String>,
    onResult: (List<UserProfile>) -> Unit,
) {
    if (ids.isEmpty()) {
        onResult(emptyList())
        return
    }

    val chunks = ids.distinct().chunked(10)
    val collected = mutableListOf<UserProfile>()
    var remaining = chunks.size

    chunks.forEach { chunk ->
        db.collection("users")
            .whereIn(FieldPath.documentId(), chunk)
            .get()
            .addOnSuccessListener { result ->
                collected += result.documents.map { it.toUserProfile() }
                remaining--
                if (remaining == 0) {
                    onResult(collected.distinctBy { it.id }.sortedBy { ids.indexOf(it.id) })
                }
            }
            .addOnFailureListener {
                remaining--
                if (remaining == 0) {
                    onResult(collected.distinctBy { it.id }.sortedBy { ids.indexOf(it.id) })
                }
            }
    }
}
