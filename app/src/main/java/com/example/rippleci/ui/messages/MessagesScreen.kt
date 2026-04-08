package com.example.rippleci.ui.messages

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rippleci.data.Conversation

@Composable
fun MessagesScreen(
    onOpenConversation: (String, String) -> Unit,
    viewModel: MessagesViewModel
) {
    val conversations by viewModel.conversations.collectAsState()
    android.util.Log.d("MSG_DEBUG", "MessagesScreen currentUserId: ${viewModel.currentUserId}")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        Text("Messages", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        if (conversations.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "No conversations yet.\nMessage a friend from the Friends tab!",
                    color = MaterialTheme.colorScheme.secondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(conversations) { convo ->
                    ConversationItem(
                        conversation = convo,
                        currentUserId = viewModel.currentUserId,
                        onClick = { onOpenConversation(convo.conversationId, convo.groupName) }
                    )
                }
            }
        }
    }
}

@Composable
fun ConversationItem(
    conversation: Conversation,
    currentUserId: String,
    onClick: () -> Unit
) {
    val displayName = if (conversation.isGroup) {
        conversation.groupName
    } else {
        val otherUserId = conversation.members.firstOrNull { it != currentUserId }

        android.util.Log.d("MSG_DEBUG", "currentUserId: $currentUserId")
        android.util.Log.d("MSG_DEBUG", "members: ${conversation.members}")
        android.util.Log.d("MSG_DEBUG", "memberNames: ${conversation.memberNames}")
        android.util.Log.d("MSG_DEBUG", "otherUserId: $otherUserId")

        conversation.memberNames[otherUserId] ?: "Unknown"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (conversation.isGroup)
                    Icons.Default.Person else Icons.Default.AccountCircle,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = displayName, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = conversation.lastMessage.ifEmpty { "No messages yet" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 1
                )
            }
        }
    }
}

