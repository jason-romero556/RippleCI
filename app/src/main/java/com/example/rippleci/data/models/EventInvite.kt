package com.example.rippleci.data.models

data class EventInvite(
    val id: String = "",
    val eventId: String = "",
    val ownerUserId: String = "",
    val fromUserId: String = "",
    val toUserId: String = "",
    val eventTitle: String = "",
    val status: String = "pending",
    val createdAt: Long = 0L,
)
