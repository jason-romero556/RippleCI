package com.example.rippleci.data.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class SchoolEvent(
    val id: String = "",
    @SerialName("eventID")
    val eventId: Long = 0L,
    val title: String = "",
    val description: String = "",
    val location: String = "",
    val startDateTime: String = "",
    val endDateTime: String = "",
    val dateTimeFormatted: String = "",
    val permaLinkUrl: String = "",
)
