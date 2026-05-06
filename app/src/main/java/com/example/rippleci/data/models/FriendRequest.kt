package com.example.rippleci.data.models

data class FriendRequest(
    val id: String = "",
    val fromUserId: String = "",
    val fromUserName: String = "",
    val toUserId: String = "",
    val status: String = "pending",
    val timestamp: Long = 0L,
)
