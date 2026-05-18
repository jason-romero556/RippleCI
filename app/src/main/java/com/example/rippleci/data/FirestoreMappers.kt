package com.example.rippleci.data

import com.example.rippleci.data.models.ClubEvent
import com.example.rippleci.data.models.ClubProfile
import com.example.rippleci.data.models.EventInvite
import com.example.rippleci.data.models.FriendRequest
import com.example.rippleci.data.models.MESSAGE_PRIVACY_FRIENDS
import com.example.rippleci.data.models.PersonalEvent
import com.example.rippleci.data.models.SchoolEvent
import com.example.rippleci.data.models.UserGroupAdminPermissions
import com.example.rippleci.data.models.UserGroupBulletinComment
import com.example.rippleci.data.models.UserGroupBulletinPost
import com.example.rippleci.data.models.UserGroupInvite
import com.example.rippleci.data.models.UserGroupProfile
import com.example.rippleci.data.models.UserProfile
import com.google.firebase.firestore.DocumentSnapshot

fun DocumentSnapshot.toUserProfile(): UserProfile {
    val classYear = getString("classYear").orEmpty()
    val visibility = getString("visibility") ?: "public"
    val classes =
        (get("classes") as? List<*>)
            ?.mapNotNull { it as? String }
            ?.ifEmpty { null }
            ?: listOf(classYear).filter { it.isNotBlank() }

    return UserProfile(
        id = id,
        name = getString("name").orEmpty(),
        bio = getString("bio").orEmpty(),
        email = getString("email").orEmpty(),
        major = getString("major").orEmpty(),
        clubIds = (get("clubs") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
        classes = classes,
        friendIds = (get("friends") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
        profilePictureUrl = getString("profilePictureUrl").orEmpty(),
        presenceMode = getString("presenceMode").orEmpty().ifBlank { "automatic" },
        presenceStatus = getString("presenceStatus").orEmpty().ifBlank { "closed" },
        presenceUpdatedAt = getLong("presenceUpdatedAt") ?: 0L,
        visibility = visibility,
        friendsVisibility = getString("friendsVisibility") ?: visibility,
        eventsVisibility = getString("eventsVisibility") ?: visibility,
        groupsVisibility = getString("groupsVisibility") ?: visibility,
        clubsVisibility = getString("clubsVisibility") ?: visibility,
        messagePrivacy = getString("messagePrivacy") ?: MESSAGE_PRIVACY_FRIENDS,
    )
}

fun DocumentSnapshot.toFriendRequest(): FriendRequest =
    FriendRequest(
        id = id,
        fromUserId = getString("fromUserId").orEmpty(),
        fromUserName = getString("fromUserName").orEmpty(),
        toUserId = getString("toUserId").orEmpty(),
        status = getString("status") ?: "pending",
        timestamp = getLong("timestamp") ?: 0L,
    )

fun DocumentSnapshot.toPersonalEvent(): PersonalEvent =
    PersonalEvent(
        id = id,
        title = getString("title").orEmpty(),
        description = getString("description").orEmpty(),
        location = getString("location").orEmpty(),
        date = getString("date").orEmpty(),
        startTime = getString("startTime").orEmpty(),
        endTime = getString("endTime").orEmpty(),
        startAtMillis = getLong("startAtMillis") ?: 0L,
        endAtMillis = getLong("endAtMillis") ?: 0L,
        ownerUserId = getString("ownerUserId").orEmpty(),
        visibility = getString("visibility") ?: "public",
        groupId = getString("groupId").orEmpty(),
        createdByUserId = getString("createdByUserId").orEmpty(),
        attendeeIds = (get("attendeeIds") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
        invitedUserIds = (get("invitedUserIds") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
        inviteesCanInvite = getBoolean("inviteesCanInvite") ?: false,
        attendeeVisibility = getString("attendeeVisibility") ?: "full",
        blockedUserIds = (get("blockedUserIds") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
        imageUrl = getString("imageUrl").orEmpty().ifBlank { getString("profilePictureUrl").orEmpty() },
    )

fun DocumentSnapshot.toSchoolEvent(): SchoolEvent =
    SchoolEvent(
        id = id,
        eventId = getLong("eventId") ?: getLong("eventID") ?: 0L,
        title = getString("title").orEmpty().ifBlank { getString("name").orEmpty() },
        description = getString("description").orEmpty(),
        location = getString("location").orEmpty(),
        startDateTime = getString("startDateTime").orEmpty(),
        endDateTime = getString("endDateTime").orEmpty(),
        dateTimeFormatted = getString("dateTimeFormatted").orEmpty(),
        permaLinkUrl = getString("permaLinkURL").orEmpty(),
    )

fun DocumentSnapshot.toEventInvite(): EventInvite =
    EventInvite(
        id = id,
        eventId = getString("eventId").orEmpty(),
        ownerUserId = getString("ownerUserId").orEmpty(),
        groupId = getString("groupId").orEmpty(),
        fromUserId = getString("fromUserId").orEmpty(),
        toUserId = getString("toUserId").orEmpty(),
        eventTitle = getString("eventTitle").orEmpty(),
        status = getString("status") ?: "pending",
        createdAt = getLong("createdAt") ?: 0L,
    )

fun DocumentSnapshot.toClubProfile(): ClubProfile =
    ClubProfile(
        id = id,
        name = getString("name").orEmpty(),
        description = getString("description").orEmpty(),
        category = getString("category").orEmpty(),
        ownerUserId = getString("ownerUserId").orEmpty(),
        memberIds = (get("memberIds") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
        officerIds = (get("officerIds") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
        adminIds = (get("adminIds") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
        blockedUserIds = (get("blockedUserIds") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
        profilePictureUrl = getString("profilePictureUrl").orEmpty(),
    )

fun DocumentSnapshot.toClubEvent(): ClubEvent =
    ClubEvent(
        id = id,
        title = getString("title").orEmpty(),
        description = getString("description").orEmpty(),
        location = getString("location").orEmpty(),
        startTime = getString("startDateTime").orEmpty(),
        endTime = getString("endDateTime").orEmpty(),
        date = getString("date").orEmpty(),
        permaLinkUrl = getString("permaLinkUrl").orEmpty(),
    )

fun DocumentSnapshot.toUserGroupProfile(): UserGroupProfile =
    UserGroupProfile(
        id = id,
        name = getString("name").orEmpty(),
        description = getString("description").orEmpty(),
        ownerUserId = getString("ownerUserId").orEmpty(),
        memberIds = (get("memberIds") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
        adminIds = (get("adminIds") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
        visibility = (getString("visibility") ?: "public").let { if (it == "private") "members" else it },
        bulletinVisibility = getString("bulletinVisibility") ?: "public",
        membersCanInvite = getBoolean("membersCanInvite") ?: false,
        membersCanCreateEvents = getBoolean("membersCanCreateEvents") ?: false,
        membersCanPostBulletin = getBoolean("membersCanPostBulletin") ?: false,
        adminsCanManageInvites = getBoolean("adminsCanManageInvites") ?: false,
        eventDefaultVisibility = getString("eventDefaultVisibility") ?: "members",
        pastEventsVisibility = getString("pastEventsVisibility") ?: "admins",
        adminPermissions =
            (
                get("adminPermissions") as? Map<*, *>
            )?.let { permissions ->
                UserGroupAdminPermissions(
                    canKickMembers = permissions["canKickMembers"] as? Boolean ?: false,
                    canPromoteAdmins = permissions["canPromoteAdmins"] as? Boolean ?: false,
                    canCreateEvents = permissions["canCreateEvents"] as? Boolean ?: false,
                    canSetEventVisibility = permissions["canSetEventVisibility"] as? Boolean ?: false,
                    canEditGroupProfile = permissions["canEditGroupProfile"] as? Boolean ?: false,
                    canManageInviteSettings =
                        permissions["canManageInviteSettings"] as? Boolean
                            ?: (getBoolean("adminsCanManageInvites") ?: false),
                    canManagePermissionMenu = permissions["canManagePermissionMenu"] as? Boolean ?: false,
                    canPostToBulletin = permissions["canPostToBulletin"] as? Boolean ?: false,
                    canManageBulletinPosts = permissions["canManageBulletinPosts"] as? Boolean ?: false,
                )
            } ?: UserGroupAdminPermissions(
                canManageInviteSettings = getBoolean("adminsCanManageInvites") ?: false,
            ),
        profilePictureUrl = getString("profilePictureUrl").orEmpty(),
    )

fun DocumentSnapshot.toUserGroupBulletinPost(): UserGroupBulletinPost =
    UserGroupBulletinPost(
        id = id,
        title = getString("title").orEmpty(),
        description = getString("description").orEmpty(),
        authorUserId = getString("authorUserId").orEmpty(),
        authorName = getString("authorName").orEmpty(),
        authorProfilePictureUrl = getString("authorProfilePictureUrl").orEmpty(),
        imageUrl = getString("imageUrl").orEmpty(),
        linkedEventId = getString("linkedEventId").orEmpty(),
        linkedEventOwnerUserId = getString("linkedEventOwnerUserId").orEmpty(),
        linkedEventGroupId = getString("linkedEventGroupId").orEmpty(),
        linkedEventTitle = getString("linkedEventTitle").orEmpty(),
        createdAt = getLong("createdAt") ?: 0L,
        updatedAt = getLong("updatedAt") ?: 0L,
    )

fun DocumentSnapshot.toUserGroupBulletinComment(): UserGroupBulletinComment =
    UserGroupBulletinComment(
        id = id,
        authorUserId = getString("authorUserId").orEmpty(),
        authorName = getString("authorName").orEmpty(),
        authorProfilePictureUrl = getString("authorProfilePictureUrl").orEmpty(),
        body = getString("body").orEmpty(),
        createdAt = getLong("createdAt") ?: 0L,
    )

fun DocumentSnapshot.toUserGroupInvite(): UserGroupInvite =
    UserGroupInvite(
        id = id,
        groupId = getString("groupId").orEmpty(),
        ownerUserId = getString("ownerUserId").orEmpty(),
        fromUserId = getString("fromUserId").orEmpty(),
        fromUserName = getString("fromUserName").orEmpty(),
        toUserId = getString("toUserId").orEmpty(),
        userGroupName = getString("userGroupName").orEmpty(),
        status = getString("status") ?: "pending",
        createdAt = getLong("createdAt") ?: 0L,
    )
