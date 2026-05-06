package com.example.rippleci.data.models

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
)
