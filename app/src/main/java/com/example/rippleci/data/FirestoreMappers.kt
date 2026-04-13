package com.example.rippleci.data

import com.example.rippleci.data.models.Club
import com.example.rippleci.data.models.ClubEvent
import com.example.rippleci.data.models.FriendRequest
import com.example.rippleci.data.models.PersonalEvent
import com.example.rippleci.data.models.SchoolEvent
import com.example.rippleci.data.models.UserProfile
import com.google.firebase.firestore.DocumentSnapshot

fun DocumentSnapshot.toUserProfile(): UserProfile =
    UserProfile(
        id = id,
        name = getString("name").orEmpty(),
        bio = getString("bio").orEmpty(),
        email = getString("email").orEmpty(),
        major = getString("major").orEmpty(),
        clubs = (get("clubs") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
        classes = (get("classes") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
        friends = (get("friends") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
        profilePictureUrl = getString("profilePictureUrl").orEmpty(),
    )

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

fun DocumentSnapshot.toClub(): Club =
    Club(
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
