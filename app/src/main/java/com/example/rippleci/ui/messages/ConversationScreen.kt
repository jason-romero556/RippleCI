package com.example.rippleci.ui.messages

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rippleci.data.Message
import com.example.rippleci.ui.components.HelpfulLinksMenuButton
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationScreen(
    conversationId: String,
    conversationName: String,
    onBack: () -> Unit,
    viewModel: MessagesViewModel = viewModel()
) {
    val messages by viewModel.messages.collectAsState()
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(conversationId) {
        viewModel.loadMessages(conversationId)
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            scope.launch {
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(conversationName) },
                navigationIcon = {
                    Row {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                        HelpfulLinksMenuButton()
                    }
                },
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
                            }
                        )
                    }
                }
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    placeholder = { Text("Type a message...") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        val trimmed = messageText.trim()
                        if (trimmed.isNotEmpty()) {
                            viewModel.sendMessage(conversationId, trimmed)
                            messageText = ""
                        }
                    }
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { message ->
                MessageBubble(
                    message = message,
                    isFromMe = message.senderId == viewModel.currentUserId
                )
            }
        }
    }
}

@Composable
fun MessageBubble(message: Message, isFromMe: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isFromMe) Alignment.End else Alignment.Start
    ) {
        if (!isFromMe) {
            Text(
                text = message.senderName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
            )
        }
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isFromMe) 16.dp else 4.dp,
                bottomEnd = if (isFromMe) 4.dp else 16.dp
            ),
            color = if (isFromMe)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                color = if (isFromMe)
                    MaterialTheme.colorScheme.onPrimary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
