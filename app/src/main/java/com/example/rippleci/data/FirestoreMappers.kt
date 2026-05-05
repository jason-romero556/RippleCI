package com.example.rippleci.data

import com.example.rippleci.data.models.ClubEvent
import com.example.rippleci.data.models.ClubProfile
import com.example.rippleci.data.models.EventInvite
import com.example.rippleci.data.models.FriendRequest
import com.example.rippleci.data.models.PersonalEvent
import com.example.rippleci.data.models.SchoolEvent
import com.example.rippleci.data.models.UserGroupInvite
import com.example.rippleci.data.models.UserGroupProfile
import com.example.rippleci.data.models.UserProfile
import com.google.firebase.firestore.DocumentSnapshot

fun DocumentSnapshot.toUserProfile(): UserProfile {
    val classYear = getString("classYear").orEmpty()
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
        presenceStatus = getString("presenceStatus").orEmpty().ifBlank { "closed" },
        presenceUpdatedAt = getLong("presenceUpdatedAt") ?: 0L,
        visibility = getString("visibility") ?: "public",
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
        ownerUserId = getString("ownerUserId").orEmpty(),
        visibility = getString("visibility") ?: "public",
        groupId = getString("groupId").orEmpty(),
        createdByUserId = getString("createdByUserId").orEmpty(),
        attendeeIds = (get("attendeeIds") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
        invitedUserIds = (get("invitedUserIds") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
    )

fun DocumentSnapshot.toSchoolEvent(): SchoolEvent =
    SchoolEvent(
        id = id,
        title = getString("name").orEmpty(),
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
        visibility = getString("visibility") ?: "public",
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
