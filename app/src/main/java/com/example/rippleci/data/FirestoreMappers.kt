package com.example.rippleci.data

// import com.example.rippleci.data.models.Club
// import com.example.rippleci.data.models.ClubEvent
import com.example.rippleci.data.models.ClubMessage
import com.example.rippleci.data.models.FriendRequest
import com.example.rippleci.data.models.PersonalEvent
import com.example.rippleci.data.models.SchoolEvent
import com.example.rippleci.data.models.UserMessage
import com.example.rippleci.data.models.UserProfile
import com.google.firebase.firestore.DocumentSnapshot
/*
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

fun DocumentSnapshot.UserMessage(): UserMessage =
    UserMessage(
        id = id,

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
        eventID = id,
        title = getString(field = "title").orEmpty(),
        description = getString(field = "description").orEmpty(),
        location = getString(field = "location").orEmpty(),
        startDateTime = getString(field = "startDateTime").orEmpty(),
        endDateTime = getString(field = "endDateTime").orEmpty(),
        dateTimeFormatted = getString(field = "dateTimeFormatted").orEmpty(),
        permaLinkURL = getString(field = "permaLinkURL").orEmpty(),
    )

fun DocumentSnapshot.toClub(): Club =
    Club(
        id = id,
        focus = getString("focus").orEmpty(),
    )

fun DocumentSnapshot.toClubEvent(): ClubEvent =
    ClubEvent(
        eventID = id,
        title = getString(field = "title").orEmpty(),
        description = getString(field = "description").orEmpty(),
        location = getString(field = "location").orEmpty(),
        startDateTime = getString(field = "startDateTime").orEmpty(),
        endDateTime = getString(field = "endDateTime").orEmpty(),
        dateTimeFormatted = getString(field = "dateTimeFormatted").orEmpty(),
        permaLinkURL = getString(field = "permaLinkURL").orEmpty(),
    )

fun DocumentSnapshot.toClubMessage(): ClubMessage =
    ClubMessage(

 */
