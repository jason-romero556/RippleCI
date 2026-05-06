package com.example.rippleci.ui.messages

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rippleci.data.Message
import com.example.rippleci.ui.components.HelpfulLinksMenuButton
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationScreen(
    conversationId: String,
    conversationName: String,
    onBack: () -> Unit,
    viewModel: MessagesViewModel = viewModel(),
) {
    val messages by viewModel.messages.collectAsState()
    val activeConversation by viewModel.activeConversation.collectAsState()
    var messageText by remember { mutableStateOf("") }
    var nowMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var lastTypingUpdateAt by remember { mutableStateOf(0L) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val typingTimeoutMillis = 10_000L
    val isSomeoneTyping =
        activeConversation
            ?.typingUsers
            ?.any { (userId, updatedAt) ->
                userId != viewModel.currentUserId && nowMillis - updatedAt <= typingTimeoutMillis
            } == true
    val latestOutgoingMessageId =
        messages
            .lastOrNull { it.senderId == viewModel.currentUserId }
            ?.messageId

    LaunchedEffect(conversationId) {
        viewModel.loadMessages(conversationId)
    }

    DisposableEffect(conversationId) {
        onDispose {
            viewModel.leaveConversation(conversationId)
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000L)
            nowMillis = System.currentTimeMillis()
        }
    }

    LaunchedEffect(conversationId, messages.lastOrNull()?.messageId) {
        viewModel.markConversationRead(conversationId)
    }

    LaunchedEffect(messages.lastOrNull()?.messageId, isSomeoneTyping) {
        val targetIndex =
            when {
                isSomeoneTyping -> messages.size
                messages.isNotEmpty() -> messages.lastIndex
                else -> -1
            }

        if (targetIndex >= 0) {
            scope.launch {
                listState.animateScrollToItem(targetIndex)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(conversationName) },
                actions = {
                    var showConfirmDialog by remember { mutableStateOf(false) }

                    IconButton(onClick = { showConfirmDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear Chat")
                    }

                    if (showConfirmDialog) {
                        AlertDialog(
                            onDismissRequest = { showConfirmDialog = false },
                            title = { Text("Clear Chat History") },
                            text = { Text("This will delete all messages for you. Are you sure?") },
                            confirmButton = {
                                Button(onClick = {
                                    viewModel.clearChatHistory(conversationId) {
                                        showConfirmDialog = false
                                    }
                                }) {
                                    Text("Clear")
                                }
                            },
                            dismissButton = {
                                OutlinedButton(onClick = { showConfirmDialog = false }) {
                                    Text("Cancel")
                                }
                            },
                        )
                    }
                },
            )
        },
        bottomBar = {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { newText ->
                        val wasTyping = messageText.isNotBlank()
                        messageText = newText

                        val now = System.currentTimeMillis()
                        if (newText.isBlank()) {
                            lastTypingUpdateAt = 0L
                            viewModel.updateTypingStatus(conversationId, false)
                        } else if (!wasTyping || now - lastTypingUpdateAt > 3_000L) {
                            lastTypingUpdateAt = now
                            viewModel.updateTypingStatus(conversationId, true)
                        }
                    },
                    placeholder = { Text("Type a message...") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        val trimmed = messageText.trim()
                        if (trimmed.isNotEmpty()) {
                            viewModel.sendMessage(conversationId, trimmed)
                            viewModel.updateTypingStatus(conversationId, false)
                            lastTypingUpdateAt = 0L
                            messageText = ""
                        }
                    },
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        },
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            itemsIndexed(
                items = messages,
                key = { _, message -> message.messageId },
            ) { _, message ->
                MessageBubble(
                    message = message,
                    isFromMe = message.senderId == viewModel.currentUserId,
                    readReceiptText =
                        if (message.messageId == latestOutgoingMessageId) {
                            viewModel.readReceiptText(message)
                        } else {
                            null
                        },
                )
            }

            if (isSomeoneTyping) {
                item(key = "typing-indicator") {
                    TypingIndicator()
                }
            }
        }
    }
}

@Composable
fun MessageBubble(
    message: Message,
    isFromMe: Boolean,
    readReceiptText: String? = null,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isFromMe) Alignment.End else Alignment.Start,
    ) {
        if (!isFromMe) {
            Text(
                text = message.senderName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp),
            )
        }
        Surface(
            shape =
                RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isFromMe) 16.dp else 4.dp,
                    bottomEnd = if (isFromMe) 4.dp else 16.dp,
                ),
            color =
                if (isFromMe) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
            modifier = Modifier.widthIn(max = 280.dp),
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                color =
                    if (isFromMe) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }

        if (isFromMe && !readReceiptText.isNullOrBlank()) {
            Text(
                text = readReceiptText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(top = 2.dp, end = 4.dp),
            )
        }
    }
}

@Composable
fun TypingIndicator() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start,
    ) {
        Surface(
            shape =
                RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = 4.dp,
                    bottomEnd = 16.dp,
                ),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.widthIn(max = 72.dp),
        ) {
            Text(
                text = "...",
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}
