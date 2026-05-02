package com.example.rippleci.data.models

// data/models/UserGroupInvite.kt
data class UserGroupInvite(
    val id: String = "",
    val groupId: String = "",
    val ownerUserId: String = "",
    val fromUserId: String = "",
    val fromUserName: String = "",
    val toUserId: String = "",
    val userGroupName: String = "",
    val status: String = "pending",
    val createdAt: Long = 0L,
)
