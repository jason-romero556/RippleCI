package com.example.rippleci.data

data class Message(
    val messageId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    val timestamp: Long = 0L
)

data class Conversation(
    val conversationId: String = "",
    val members: List<String> = emptyList(),
    val memberNames: Map<String, String> = emptyMap(),
    val isGroup: Boolean = false,
    val groupName: String = "",
    val lastMessage: String = "",
    val lastUpdated: Long = 0L
)

