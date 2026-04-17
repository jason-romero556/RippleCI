package com.example.rippleci.data.models

data class EventProfile(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val location: String = "",
    val date: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val createdByUserId: String = "",
    val clubId: String? = null,
    val attendeeIds: List<String> = emptyList(),
    val visibility: String = "public",
)
