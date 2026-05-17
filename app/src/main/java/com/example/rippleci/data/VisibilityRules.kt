package com.example.rippleci.data

import com.example.rippleci.data.models.PersonalEvent
import com.example.rippleci.data.models.UserGroupProfile
import com.example.rippleci.data.models.GROUP_PAST_EVENTS_ADMINS
import com.example.rippleci.data.models.GROUP_PAST_EVENTS_MEMBERS
import com.example.rippleci.data.models.GROUP_PAST_EVENTS_OWNER
import com.example.rippleci.data.models.UserProfile

fun canViewProfile(
    profile: UserProfile,
    currentUserId: String,
    currentUserFriendIds: List<String>,
): Boolean =
    profile.id == currentUserId ||
        profile.visibility == "public" ||
        (profile.visibility == "friends" && currentUserFriendIds.contains(profile.id))

fun canViewGroupProfile(
    profile: UserGroupProfile,
    currentUserId: String,
    currentUserFriendIds: List<String>,
): Boolean =
    currentUserId == profile.ownerUserId ||
        profile.adminIds.contains(currentUserId) ||
        profile.memberIds.contains(currentUserId) ||
        profile.visibility == "public" ||
        (profile.visibility == "friends" && currentUserFriendIds.contains(profile.ownerUserId))

fun canViewPastGroupEvents(
    profile: UserGroupProfile,
    currentUserId: String,
): Boolean =
    when (profile.pastEventsVisibility) {
        GROUP_PAST_EVENTS_OWNER -> currentUserId == profile.ownerUserId
        GROUP_PAST_EVENTS_ADMINS -> currentUserId == profile.ownerUserId || profile.adminIds.contains(currentUserId)
        GROUP_PAST_EVENTS_MEMBERS -> currentUserId == profile.ownerUserId || profile.adminIds.contains(currentUserId) || profile.memberIds.contains(currentUserId)
        else -> true
    }

fun canViewEvent(
    event: PersonalEvent,
    currentUserId: String,
    currentUserFriendIds: List<String>,
    groupProfile: UserGroupProfile? = null,
): Boolean {
    val canManageEvent =
        event.ownerUserId == currentUserId ||
            event.createdByUserId == currentUserId

    if (!canManageEvent && event.blockedUserIds.contains(currentUserId)) {
        return false
    }

    val canManageGroupContext =
        groupProfile?.let { profile ->
            currentUserId == profile.ownerUserId || profile.adminIds.contains(currentUserId)
        } ?: false

    if (groupProfile != null && event.isPastEvent() && !canManageEvent && !canManageGroupContext) {
        if (!canViewPastGroupEvents(groupProfile, currentUserId)) {
            return false
        }
    }

    val isGroupMember =
        groupProfile?.let { profile ->
            currentUserId == profile.ownerUserId ||
                profile.adminIds.contains(currentUserId) ||
                profile.memberIds.contains(currentUserId)
        } ?: false

    return canManageEvent ||
        canManageGroupContext ||
        event.visibility == "public" ||
        (event.visibility == "friends" && currentUserFriendIds.contains(event.ownerUserId)) ||
        (event.visibility == "members" && isGroupMember) ||
        (
            event.visibility == "attendees" &&
                (event.attendeeIds.contains(currentUserId) || event.invitedUserIds.contains(currentUserId))
        )
}
