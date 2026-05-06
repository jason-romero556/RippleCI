package com.example.rippleci.data.models

data class ClubEvent(
    val id: String = "",
    val clubID: String = "",
    val title: String,
    val description: String,
    val createdByUserID: String = "",
    val location: String,
    val startTime: String,
    val endTime: String,
    val date: String,
    val permaLinkUrl: String,
    val visibility: String = "public",
)
