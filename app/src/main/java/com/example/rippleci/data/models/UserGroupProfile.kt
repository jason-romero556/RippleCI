package com.example.rippleci.data.models

data class UserGroupProfile(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val ownerUserId: String = "",
    val memberIds: List<String> = emptyList(),
    val adminIds: List<String> = emptyList(),
    val visibility: String = "public",
)
