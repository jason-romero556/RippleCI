package com.example.rippleci.data.models

const val MESSAGE_PRIVACY_EVERYONE = "everyone"
const val MESSAGE_PRIVACY_FRIENDS = "friends"

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
    val friendsVisibility: String = "public",
    val eventsVisibility: String = "public",
    val groupsVisibility: String = "public",
    val clubsVisibility: String = "public",
    val messagePrivacy: String = MESSAGE_PRIVACY_FRIENDS,
    val presenceMode: String = "automatic",
    val presenceStatus: String = "closed",
    val presenceUpdatedAt: Long = 0L,
)
