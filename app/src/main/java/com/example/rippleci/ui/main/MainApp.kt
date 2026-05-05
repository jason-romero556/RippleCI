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
import com.example.rippleci.ui.screens.UserGroupProfileScreen
import com.example.rippleci.ui.screens.UserProfileScreen
import com.example.rippleci.ui.theme.AppTheme
import com.example.rippleci.ui.theme.ThemeViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

@Composable
fun MainApp(
    themeViewModel: ThemeViewModel,
    onSignOut: () -> Unit,
) {
    var currentDestination by remember { mutableStateOf(AppDestinations.HOME) }
    val currentUserId = Firebase.auth.currentUser?.uid ?: "logged_out"
    val messagesViewModel: MessagesViewModel = viewModel(key = "messages_$currentUserId")
    var routeStack by remember { mutableStateOf<List<AppRoute>>(emptyList()) }
    val route = routeStack.lastOrNull() ?: AppRoute.MainTabs

    fun navigateTo(nextRoute: AppRoute) {
        routeStack = routeStack + nextRoute
    }

    fun popRoute() {
        routeStack = routeStack.dropLast(1)
    }

    fun resetToTabs() {
        routeStack = emptyList()
    }

    val usesAppPalette = themeViewModel.appTheme != AppTheme.DYNAMIC
    val topBarContentColor =
        if (usesAppPalette) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurface
        }
    val navSuiteColors =
        if (usesAppPalette) {
            NavigationSuiteDefaults.colors(
                navigationBarContainerColor = MaterialTheme.colorScheme.primary,
                navigationBarContentColor = MaterialTheme.colorScheme.onPrimary,
                navigationRailContainerColor = MaterialTheme.colorScheme.primary,
                navigationRailContentColor = MaterialTheme.colorScheme.onPrimary,
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
                        resetToTabs()
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
        navigationSuiteColors = navSuiteColors,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = if (usesAppPalette) MaterialTheme.colorScheme.primary else Color.Transparent,
                shadowElevation = 4.dp,
            ) {
                Box(
                    modifier =
                        Modifier
                            .then(
                                if (usesAppPalette) {
                                    Modifier
                                } else {
                                    Modifier.background(
                                        brush =
                                            Brush.verticalGradient(
                                                colors =
                                                    listOf(
                                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                                        MaterialTheme.colorScheme.surface,
                                                    ),
                                            ),
                                    )
                                },
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
                        tint = topBarContentColor,
                    )
                    Text(
                        text = currentDestination.label,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.align(Alignment.Center),
                        color = topBarContentColor,
                    )
                    if (routeStack.isNotEmpty()) {
                        IconButton(
                            onClick = { popRoute() },
                            modifier = Modifier.align(Alignment.CenterEnd),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = topBarContentColor,
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
                            AppDestinations.HOME -> {
                                HomeScreen(
                                    themeViewModel = themeViewModel,
                                    onOpenEventProfile = { ownerUserId, eventId ->
                                        navigateTo(AppRoute.EventProfile(eventId, ownerUserId))
                                    },
                                )
                            }

                            AppDestinations.MAP -> {
                                MapScreen()
                            }

                            AppDestinations.FRIENDS -> {
                                FriendsScreen(
                                    onOpenConversation = { conversationId, convName ->
                                        navigateTo(AppRoute.Conversation(conversationId, convName))
                                    },
                                    onOpenUserProfile = { userId ->
                                        navigateTo(AppRoute.UserProfile(userId))
                                    },
                                    onOpenUserGroupProfile = { groupId ->
                                        navigateTo(AppRoute.UserGroupProfile(groupId))
                                    },
                                    messagesViewModel = messagesViewModel,
                                )
                            }

                            AppDestinations.MESSAGES -> {
                                MessagesScreen(
                                    onOpenConversation = { conversationId, convName ->
                                        navigateTo(AppRoute.Conversation(conversationId, convName))
                                    },
                                    onOpenUserProfile = { userId ->
                                        navigateTo(AppRoute.UserProfile(userId))
                                    },
                                    onOpenClubProfile = { clubId ->
                                        navigateTo(AppRoute.ClubProfile(clubId))
                                    },
                                    onOpenEventProfile = { eventId ->
                                        navigateTo(AppRoute.EventProfile(eventId))
                                    },
                                    viewModel = messagesViewModel,
                                )
                            }

                            AppDestinations.PROFILE -> {
                                ProfileScreen(onSignOut = onSignOut)
                            }

                            AppDestinations.EVENTS -> {
                                EventsScreen(
                                    onOpenEventProfile = { ownerUserId, eventId ->
                                        navigateTo(
                                            AppRoute.EventProfile(
                                                eventId,
                                                ownerUserId,
                                            ),
                                        )
                                    },
                                )
                            }
                        }
                    }
                }

                is AppRoute.Conversation -> {
                    ConversationScreen(
                        conversationId = currentRoute.conversationId,
                        conversationName = currentRoute.title,
                        onBack = { popRoute() },
                        viewModel = messagesViewModel,
                    )
                }

                is AppRoute.UserProfile -> {
                    UserProfileScreen(
                        userId = currentRoute.userId,
                        onBack = { popRoute() },
                        onOpenUserProfile = { userId ->
                            navigateTo(AppRoute.UserProfile(userId))
                        },
                        onOpenClubProfile = { clubId ->
                            navigateTo(AppRoute.ClubProfile(clubId))
                        },
                        onOpenEventProfile = { eventId ->
                            navigateTo(
                                AppRoute.EventProfile(
                                    eventId = eventId,
                                    ownerUserId = currentRoute.userId,
                                ),
                            )
                        },
                    )
                }

                is AppRoute.ClubProfile -> {
                    ClubProfileScreen(
                        clubId = currentRoute.clubId,
                        isMember = true,
                        onBack = { popRoute() },
                        onJoinClub = { /* Handle join club logic */ },
                        onLeaveClub = { /* Handle leave club logic */ },
                        onViewEvents = { /* Handle view events logic */ },
                        onOpenUserProfile = { userId ->
                            navigateTo(AppRoute.UserProfile(userId))
                        },
                        onOpenClubProfile = { clubId ->
                            navigateTo(AppRoute.ClubProfile(clubId))
                        },
                        onOpenEventProfile = { eventId ->
                            navigateTo(AppRoute.EventProfile(eventId))
                        },
                    )
                }

                is AppRoute.UserGroupProfile -> {
                    UserGroupProfileScreen(
                        userGroupId = currentRoute.userGroupId,
                        onBack = { popRoute() },
                        onOpenUserProfile = { userId -> navigateTo(AppRoute.UserProfile(userId)) },
                    )
                }

                is AppRoute.EventProfile -> {
                    EventProfileScreen(
                        eventId = currentRoute.eventId,
                        ownerUserId = currentRoute.ownerUserId,
                        onBack = { popRoute() },
                        onOpenUserProfile = { userId ->
                            navigateTo(AppRoute.UserProfile(userId))
                        },
                        onOpenClubProfile = { clubId ->
                            navigateTo(AppRoute.ClubProfile(clubId))
                        },
                        onOpenEventProfile = { eventId ->
                            navigateTo(AppRoute.EventProfile(eventId))
                        },
                    )
                }
            }
        }
    }
}
