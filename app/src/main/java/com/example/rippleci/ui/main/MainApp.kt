package com.example.rippleci.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
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
import com.example.rippleci.ui.screens.UserGroupProfileScreen
import com.example.rippleci.ui.theme.AppTheme
import com.example.rippleci.ui.theme.ThemeViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

@Composable
fun MainApp(
    themeViewModel: ThemeViewModel,
    onSignOut: () -> Unit,
    navigateTo: String? = null,
    conversationId: String? = null
) {
    var currentDestination by remember { mutableStateOf(AppDestinations.HOME) }
    var route by remember { mutableStateOf<AppRoute>(AppRoute.MainTabs) }
    // var openConversationId by remember { mutableStateOf<String?>(null) }
    // var openConversationName by remember { mutableStateOf("") }
    val currentUserId = Firebase.auth.currentUser?.uid ?: "logged_out"
    val messagesViewModel: MessagesViewModel = viewModel(key = "messages_$currentUserId")

    LaunchedEffect(navigateTo) {
        when (navigateTo) {
            "friends" -> {
                currentDestination = AppDestinations.FRIENDS
                route = AppRoute.MainTabs
            }
            "messages" -> {
                currentDestination = AppDestinations.MESSAGES
                if (conversationId != null) {
                    FirebaseFirestore.getInstance()
                        .collection("conversations")
                        .document(conversationId)
                        .get()
                        .addOnSuccessListener { doc ->
                            val name = doc.getString("name") ?: "Chat"
                            route = AppRoute.Conversation(conversationId, name)
                        }
                } else {
                    route = AppRoute.MainTabs
                }
            }
        }
    }

    val navSuiteColors = if (themeViewModel.appTheme != AppTheme.DYNAMIC) {
        NavigationSuiteDefaults.colors(
            navigationBarContainerColor = MaterialTheme.colorScheme.primary,
            navigationBarContentColor = MaterialTheme.colorScheme.onPrimary,
            navigationRailContainerColor = MaterialTheme.colorScheme.primary,
            navigationRailContentColor = MaterialTheme.colorScheme.onPrimary
        )
    } else {
        NavigationSuiteDefaults.colors()
    }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
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
        navigationSuiteColors = navSuiteColors
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = if (themeViewModel.appTheme != AppTheme.DYNAMIC) {
                    MaterialTheme.colorScheme.primary
                } else {
                    Color.Transparent
                },
                shadowElevation = 4.dp,
            ) {
                Box(
                    modifier = Modifier
                        .then(
                            if (themeViewModel.appTheme == AppTheme.DYNAMIC) {
                                Modifier.background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                            MaterialTheme.colorScheme.surface
                                        )
                                    )
                                )
                            } else {
                                Modifier
                            }
                        )
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
                        tint = if (themeViewModel.appTheme != AppTheme.DYNAMIC) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = currentDestination.label,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.align(Alignment.Center),
                        color = if (themeViewModel.appTheme != AppTheme.DYNAMIC) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    )
                    if (route != AppRoute.MainTabs) {
                        IconButton(
                            onClick = { route = AppRoute.MainTabs },
                            modifier = Modifier.align(Alignment.CenterEnd),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = if (themeViewModel.appTheme != AppTheme.DYNAMIC) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
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

                            AppDestinations.HOME -> {
                                HomeScreen(themeViewModel = themeViewModel)
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
                        ownerUserId = currentRoute.ownerUserId,
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

                is AppRoute.UserGroupProfile -> {
                    UserGroupProfileScreen(
                        userGroupId = currentRoute.userGroupId,
                        onBack = { route = AppRoute.MainTabs },
                        onOpenUserProfile = { userId ->
                            route = AppRoute.UserProfile(userId)
                        },
                    )
                }
            }
        }
    }
}
