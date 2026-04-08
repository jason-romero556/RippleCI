package com.example.rippleci.data

import kotlinx.serialization.Serializable

@Serializable
data class SchoolEvent(
    val eventID: Long,
    val title: String,
    val description: String,
    val location: String,
    val startDateTime: String,
    val endDateTime: String,
    val dateTimeFormatted: String,
    val permaLinkUrl: String
)
