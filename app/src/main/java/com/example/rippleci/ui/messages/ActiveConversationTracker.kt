package com.example.rippleci.ui.messages

object ActiveConversationTracker {
    @Volatile
    var conversationId: String? = null
}
