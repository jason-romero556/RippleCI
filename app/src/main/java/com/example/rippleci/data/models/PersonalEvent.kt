package com.example.rippleci.data.models

const val EVENT_ATTENDEE_VISIBILITY_FULL = "full"
const val EVENT_ATTENDEE_VISIBILITY_COUNT = "count"
const val EVENT_ATTENDEE_VISIBILITY_NONE = "none"

data class PersonalEvent(
    val id: String = "",
    val title: String = "",
    val ownerUserId: String = "",
    val description: String = "",
    val location: String = "",
    val date: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val startAtMillis: Long = 0L,
    val endAtMillis: Long = 0L,
    val visibility: String = "public",
    val groupId: String = "",
    val createdByUserId: String = "",
    val attendeeIds: List<String> = emptyList(),
    val invitedUserIds: List<String> = emptyList(),
    val inviteesCanInvite: Boolean = false,
    val attendeeVisibility: String = EVENT_ATTENDEE_VISIBILITY_FULL,
    val blockedUserIds: List<String> = emptyList(),
    val imageUrl: String = "",
)
