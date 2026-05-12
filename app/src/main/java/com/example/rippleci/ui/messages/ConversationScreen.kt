package com.example.rippleci.ui.messages

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.rippleci.data.Message
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationScreen(
    conversationId: String,
    conversationName: String,
    onBack: () -> Unit,
    viewModel: MessagesViewModel,
) {
    val messages by viewModel.messages.collectAsState()
    val conversations by viewModel.conversations.collectAsState()
    val typingUsers by viewModel.typingUsers.collectAsState()
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    var typingJob by remember { mutableStateOf<Job?>(null) }
    var isConversationVisible by remember {
        mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
    }

    val members = conversations
        .firstOrNull { it.conversationId == conversationId }
        ?.members ?: emptyList()

    // Is anyone else typing?
    val someoneElseTyping = typingUsers.any { (uid, isTyping) ->
        uid != viewModel.currentUserId && isTyping
    }

    LaunchedEffect(conversationId) {
        viewModel.loadMessages(conversationId)
        viewModel.markMessagesAsRead(conversationId)
        viewModel.observeTyping(conversationId)
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            scope.launch {
                listState.animateScrollToItem(messages.size - 1)
            }
            viewModel.markMessagesAsRead(conversationId)
        }
    }

    // Clear typing indicator when leaving
    DisposableEffect(conversationId, lifecycleOwner) {
        viewModel.setActiveConversation(conversationId)
        val observer =
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_START,
                    Lifecycle.Event.ON_RESUME -> {
                        isConversationVisible = true
                        viewModel.setActiveConversation(conversationId)
                    }

                    Lifecycle.Event.ON_STOP -> {
                        isConversationVisible = false
                        viewModel.clearActiveConversation(conversationId)
                    }
                    else -> Unit
                }
            }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.setTyping(conversationId, false)
            viewModel.clearActiveConversation(conversationId)
        }
    }

    LaunchedEffect(conversationId, isConversationVisible) {
        if (!isConversationVisible) return@LaunchedEffect

        while (true) {
            viewModel.setActiveConversation(conversationId)
            delay(60_000L)
        }
    }

    Scaffold(
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { newValue ->
                        messageText = newValue
                        viewModel.setTyping(conversationId, true)
                        typingJob?.cancel()
                        typingJob = scope.launch {
                            delay(20000)
                            viewModel.setTyping(conversationId, false)
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
                            viewModel.setTyping(conversationId, false)
                            typingJob?.cancel()
                            viewModel.sendMessage(conversationId, trimmed)
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(messages) { message ->
                    MessageBubble(
                        message = message,
                        isFromMe = message.senderId == viewModel.currentUserId,
                        conversationMembers = members
                    )
                }
                if (someoneElseTyping) {
                    item { TypingIndicatorBubble() }
                }
            }
        }
    }

    // Auto-scroll when typing indicator appears/disappears
    LaunchedEffect(someoneElseTyping) {
        if (someoneElseTyping) {
            scope.launch {
                listState.animateScrollToItem(listState.layoutInfo.totalItemsCount - 1)
            }
        }
    }
}

@Composable
fun TypingIndicatorBubble() {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")

    val dot1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(0)
        ), label = "dot1"
    )
    val dot2Alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(150)
        ), label = "dot2"
    )
    val dot3Alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(300)
        ), label = "dot3"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp, topEnd = 16.dp,
                bottomStart = 4.dp, bottomEnd = 16.dp
            ),
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("●", fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = dot1Alpha))
                Text("●", fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = dot2Alpha))
                Text("●", fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = dot3Alpha))
            }
        }
    }
}

@Composable
fun MessageBubble(
    message: Message,
    isFromMe: Boolean,
    conversationMembers: List<String> = emptyList(),
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
            shape = RoundedCornerShape(
                topStart = 16.dp, topEnd = 16.dp,
                bottomStart = if (isFromMe) 16.dp else 4.dp,
                bottomEnd = if (isFromMe) 4.dp else 16.dp,
            ),
            color = if (isFromMe) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            modifier = Modifier.widthIn(max = 280.dp),
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                color = if (isFromMe) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
        if (isFromMe) {
            val otherMembers = conversationMembers.filter { it != message.senderId }
            val allRead = otherMembers.isNotEmpty() &&
                    otherMembers.all { message.readBy.contains(it) }
            Text(
                text = if (allRead) "✓✓ Read" else "✓ Sent",
                style = MaterialTheme.typography.labelSmall,
                color = if (allRead) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.secondary
                },
                modifier = Modifier.padding(end = 4.dp, top = 2.dp),
            )
        }
    }
}
