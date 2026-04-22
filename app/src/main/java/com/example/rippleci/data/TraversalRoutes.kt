package com.example.rippleci.data

sealed class AppRoute {
    data object MainTabs : AppRoute()

    data class UserProfile(
        val userId: String,
    ) : AppRoute()

    data class ClubProfile(
        val clubId: String,
    ) : AppRoute()

    data class EventProfile(
        val eventId: String,
    ) : AppRoute()

    data class Conversation(
        val conversationId: String,
        val title: String,
    ) : AppRoute()
}
