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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.example.rippleci.data.AppRoute
import com.example.rippleci.ui.components.HelpfulLinksMenuButton
import com.example.rippleci.ui.events.EventsScreen
import com.example.rippleci.ui.messages.ConversationScreen
import com.example.rippleci.ui.messages.MessagesScreen
import com.example.rippleci.ui.messages.MessagesViewModel
import com.example.rippleci.ui.screens.ClubProfileScreen
import com.example.rippleci.ui.screens.EventProfileScreen
import com.example.rippleci.ui.screens.FriendsScreen
import com.example.rippleci.ui.screens.HomeScreen
import com.example.rippleci.ui.screens.MapScreen
import com.example.rippleci.ui.screens.ProfileScreen
import com.example.rippleci.ui.screens.UserProfileScreen
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

@Composable
fun MainApp(onSignOut: () -> Unit) {
    var currentDestination by remember { mutableStateOf(AppDestinations.MAP) }
    var route by remember { mutableStateOf<AppRoute>(AppRoute.MainTabs) }
    // var openConversationId by remember { mutableStateOf<String?>(null) }
    // var openConversationName by remember { mutableStateOf("") }
    val currentUserId = Firebase.auth.currentUser?.uid ?: "logged_out"
    val messagesViewModel: MessagesViewModel = viewModel(key = "messages_$currentUserId")

    NavigationSuiteScaffold(
        {
            AppDestinations.entries.forEach { destination ->
                item(
                    selected = currentDestination == destination,
                    onClick = {
                        route = AppRoute.MainTabs
                        currentDestination = destination
                    },
                    icon = {
                        Icon(
                            destination.icon,
                            contentDescription = destination.label,
                        )
                    },
                    label = {
                        Text(destination.label)
                    },
                )
            }
        },
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 4.dp, // Add a slight shadow for depth
            ) {
                Box(
                    modifier =
                        Modifier
                            .statusBarsPadding()
                            .height(56.dp)
                            .fillMaxWidth(),
                )
                {
                    HelpfulLinksMenuButton(
                        modifier =
                            Modifier
                                .align(Alignment.CenterStart)
                                .padding(start = 8.dp),
                    )
                    Text(
                        text = currentDestination.label,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.align(Alignment.Center),
                    )
                    if (route != AppRoute.MainTabs) {
                        IconButton(
                            onClick = { route = AppRoute.MainTabs },
                            modifier = Modifier.align(Alignment.CenterEnd),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                            )
                        }
                    }
                }
            }

            when (val currentRoute = route) {
                AppRoute.MainTabs -> {
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
                                    onOpenConversation = { conversationId, convName ->
                                        route = AppRoute.Conversation(conversationId, convName)
                                    },
                                    onOpenUserProfile = { userId ->
                                        route = AppRoute.UserProfile(userId)
                                    },
                                    messagesViewModel = messagesViewModel,
                                )
                            }

                            AppDestinations.MESSAGES -> {
                                MessagesScreen(
                                    onOpenConversation = { conversationId, convName ->
                                        route = AppRoute.Conversation(conversationId, convName)
                                    },
                                    onOpenUserProfile = { userId ->
                                        route = AppRoute.UserProfile(userId)
                                    },
                                    onOpenClubProfile = { clubId ->
                                        route = AppRoute.ClubProfile(clubId)
                                    },
                                    onOpenEventProfile = { eventId ->
                                        route = AppRoute.EventProfile(eventId)
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

                is AppRoute.Conversation -> {
                    ConversationScreen(
                        conversationId = currentRoute.conversationId,
                        conversationName = currentRoute.title,
                        onBack = { route = AppRoute.MainTabs },
                        viewModel = messagesViewModel,
                    )
                }

                is AppRoute.UserProfile -> {
                    UserProfileScreen(
                        userId = currentRoute.userId,
                        onBack = { route = AppRoute.MainTabs },
                        onOpenUserProfile = { userId ->
                            route = AppRoute.UserProfile(userId)
                        },
                        onOpenClubProfile = { clubId ->
                            route = AppRoute.ClubProfile(clubId)
                        },
                        onOpenEventProfile = { eventId ->
                            route = AppRoute.EventProfile(eventId)
                        },
                    )
                }

                is AppRoute.ClubProfile -> {
                    ClubProfileScreen(
                        clubId = currentRoute.clubId,
                        isMember = true,
                        onBack = { route = AppRoute.MainTabs },
                        onJoinClub = { /* Handle join club logic */ },
                        onLeaveClub = { /* Handle leave club logic */ },
                        onViewEvents = { /* Handle view events logic */ },
                        onOpenUserProfile = { userId ->
                            route = AppRoute.UserProfile(userId)
                        },
                        onOpenClubProfile = { clubId ->
                            route = AppRoute.ClubProfile(clubId)
                        },
                        onOpenEventProfile = { eventId ->
                            route = AppRoute.EventProfile(eventId)
                        },
                    )
                }

                is AppRoute.EventProfile -> {
                    EventProfileScreen(
                        eventId = currentRoute.eventId,
                        onBack = { route = AppRoute.MainTabs },
                        onOpenUserProfile = { userId ->
                            route = AppRoute.UserProfile(userId)
                        },
                        onOpenClubProfile = { clubId ->
                            route = AppRoute.ClubProfile(clubId)
                        },
                        onOpenEventProfile = { eventId ->
                            route = AppRoute.EventProfile(eventId)
                        },
                    )
                }
            }
        }
    }
}
