package com.example.rippleci.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rippleci.ui.components.HelpfulLinksMenuButton
import com.example.rippleci.ui.events.EventsScreen
import com.example.rippleci.ui.messages.ConversationScreen
import com.example.rippleci.ui.messages.MessagesScreen
import com.example.rippleci.ui.messages.MessagesViewModel
import com.example.rippleci.ui.screens.FriendsScreen
import com.example.rippleci.ui.screens.HomeScreen
import com.example.rippleci.ui.screens.MapScreen
import com.example.rippleci.ui.screens.ProfileScreen
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

@Composable
fun MainApp(onSignOut: () -> Unit) {
    var currentDestination by remember { mutableStateOf(AppDestinations.MAP) }
    var openConversationId by remember { mutableStateOf<String?>(null) }
    var openConversationName by remember { mutableStateOf("") }
    val currentUserId = Firebase.auth.currentUser?.uid ?: "logged_out"
    val messagesViewModel: MessagesViewModel = viewModel(key = "messages_$currentUserId")

    // If a conversation is open, show it full screen
    if (openConversationId != null) {
        ConversationScreen(
            conversationId = openConversationId!!,
            conversationName = openConversationName,
            onBack = { openConversationId = null },
            viewModel = messagesViewModel,
        )
        return
    }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                item(
                    icon = {
                        Icon(it.icon, contentDescription = it.label)
                    },
                    label = { Text(it.label) },
                    selected = it == currentDestination,
                    onClick = { currentDestination = it },
                )
            }
        },
    ) {
        // WRAP THE CONTENT IN A COLUMN
        Column(modifier = Modifier.fillMaxSize()) {
            // THE TOP BUFFER BOX
            // This box now contains the hamburger menu and handles status bar padding
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 4.dp // Add a slight shadow for depth
            ) {
                Box(
                    modifier = Modifier
                        .statusBarsPadding()
                        .height(56.dp)
                        .fillMaxWidth()
                ) {
                    HelpfulLinksMenuButton(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 8.dp)
                    )
                    
                    Text(
                        text = currentDestination.label,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }

            // 2. MAIN SCREEN CONTENT
            // Modifier.weight(1f) ensures this takes up all remaining space
            // without covering the bottom navigation bar.
            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                when (currentDestination) {
                    AppDestinations.MAP -> {
                        MapScreen()
                    }

                    AppDestinations.FRIENDS -> {
                        FriendsScreen(
                            onOpenConversation = { convId, convName ->
                                openConversationId = convId
                                openConversationName = convName
                            },
                            messagesViewModel = messagesViewModel,
                        )
                    }

                    AppDestinations.MESSAGES -> {
                        MessagesScreen(
                            onOpenConversation = { convId, friendName ->
                                val convo =
                                    messagesViewModel.conversations.value
                                        .find { it.conversationId == convId }
                                openConversationName =
                                    if (convo?.isGroup == true) {
                                        convo.groupName
                                    } else {
                                        val otherUserId =
                                            convo
                                                ?.members
                                                ?.firstOrNull { it != messagesViewModel.currentUserId }

                                        convo?.memberNames?.get(otherUserId) ?: "Chat"
                                    }

                                openConversationId = convId
                            },
                            viewModel = messagesViewModel,
                        )
                    }

                    AppDestinations.PROFILE -> {
                        ProfileScreen(onSignOut = onSignOut)
                    }

                    AppDestinations.EVENTS -> {
                        EventsScreen()
                    }
                }
            }
        }
    }
}
