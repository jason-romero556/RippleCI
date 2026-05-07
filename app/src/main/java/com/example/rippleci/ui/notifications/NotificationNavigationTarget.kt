package com.example.rippleci.ui.notifications

data class NotificationNavigationTarget(
    val navigateTo: String,
    val conversationId: String = "",
    val title: String = "",
    val requestId: String = "",
    val senderId: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)
