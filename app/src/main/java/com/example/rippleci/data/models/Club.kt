package com.example.rippleci.data.models

data class Club(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val category: String = "",
    val ownerUserId: String = "",
    val memberIds: List<String> = emptyList(),
    val adminIds: List<String> = emptyList(),
    val profilePictureUrl: String = "",
)
