package com.example.rippleci.data.models

data class UserProfile(
    val id: String = "",
    val name: String = "",
    val bio: String = "",
    val email: String = "",
    val major: String = "",
    val clubs: List<String> = emptyList(),
    val classes: List<String> = emptyList(),
    val friends: List<String> = emptyList(),
    val profilePictureUrl: String = "",
)
