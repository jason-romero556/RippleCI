package com.example.rippleci.data.models

data class ClubMessage(
    val id: String = "",
    val clubId: String = "",
    val senderUserId: String = "",
    val message: String = "",
    val timestamp: Long = 0L,
)
