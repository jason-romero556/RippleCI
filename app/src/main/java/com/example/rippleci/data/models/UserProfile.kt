package com.example.rippleci.data.models

data class UserProfile(
    val id: String = "",
    val name: String = "",
    val bio: String = "",
    val email: String = "",
    val major: String = "",
    val classes: List<String> = emptyList(),
    val clubIds: List<String> = emptyList(),
    val eventIds: List<String> = emptyList(),
    val friendIds: List<String> = emptyList(),
    val profilePictureUrl: String = "",
    val visibility: String = "public",
)
