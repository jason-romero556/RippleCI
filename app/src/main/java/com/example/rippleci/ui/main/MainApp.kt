package com.example.rippleci.ui.main

import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rippleci.ui.events.EventsScreen
import com.example.rippleci.ui.messages.ConversationScreen
import com.example.rippleci.ui.messages.MessagesScreen
import com.example.rippleci.ui.messages.MessagesViewModel
import com.example.rippleci.ui.screens.FriendsScreen
import com.example.rippleci.ui.screens.HomeScreen
import com.example.rippleci.ui.screens.MapScreen
import com.example.rippleci.ui.screens.ProfileScreen

@Composable
fun MainApp(onSignOut: () -> Unit) {
    var currentDestination by remember { mutableStateOf(AppDestinations.MAP) }
    var openConversationId by remember { mutableStateOf<String?>(null) }
    var openConversationName by remember { mutableStateOf("") }
    val messagesViewModel: MessagesViewModel = viewModel()

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
                        // Find conversation name from viewModel
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
