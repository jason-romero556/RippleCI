package com.example.rippleci.data

import com.example.rippleci.data.models.PersonalEvent
import com.example.rippleci.data.models.UserProfile

fun canViewProfile(
    profile: UserProfile,
    currentUserId: String,
    currentUserFriendIds: List<String>,
): Boolean =
    profile.id == currentUserId ||
        profile.visibility == "public" ||
        (profile.visibility == "friends" && currentUserFriendIds.contains(profile.id))

fun canViewEvent(
    event: PersonalEvent,
    currentUserId: String,
    currentUserFriendIds: List<String>,
): Boolean =
    event.ownerUserId == currentUserId ||
        event.createdByUserId == currentUserId ||
        event.visibility == "public" ||
        (event.visibility == "friends" && currentUserFriendIds.contains(event.ownerUserId)) ||
        (event.visibility == "attendees" && event.attendeeIds.contains(currentUserId))
