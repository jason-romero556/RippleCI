package com.example.rippleci.ui.main

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rippleci.data.AppRoute
import com.example.rippleci.ui.components.HelpfulLinksMenuButton
import com.example.rippleci.ui.components.RippleButton
import com.example.rippleci.ui.components.RippleNavigationBar
import com.example.rippleci.ui.components.RippleNavigationBarItem
import com.example.rippleci.ui.components.RippleOutlinedButton
import com.example.rippleci.ui.events.EventsScreen
import com.example.rippleci.ui.messages.ConversationScreen
import com.example.rippleci.ui.messages.MessagesScreen
import com.example.rippleci.ui.messages.MessagesViewModel
import com.example.rippleci.ui.notifications.NotificationNavigationTarget
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
    notificationNavigationTarget: NotificationNavigationTarget? = null,
    onNotificationNavigationHandled: () -> Unit = {},
    onSignOut: () -> Unit,
) {
    var currentDestination by remember { mutableStateOf(AppDestinations.HOME) }
    val currentUserId = Firebase.auth.currentUser?.uid ?: "logged_out"
    val messagesViewModel: MessagesViewModel = viewModel(key = "messages_$currentUserId")
    var routeStack by remember { mutableStateOf<List<AppRoute>>(emptyList()) }
    var profileTitlesByUserId by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var isBackNavigation by remember { mutableStateOf(false) }
    var requestedFriendsTab by remember { mutableStateOf<Int?>(null) }
    var showClearChatDialog by remember { mutableStateOf(false) }
    val route = routeStack.lastOrNull() ?: AppRoute.MainTabs

    fun navigateTo(nextRoute: AppRoute) {
        isBackNavigation = false
        routeStack = routeStack + nextRoute
    }

    fun navigateToUserProfile(
        userId: String,
        displayName: String = "",
    ) {
        navigateTo(AppRoute.UserProfile(userId, displayName))
    }

    fun popRoute() {
        isBackNavigation = true
        routeStack = routeStack.dropLast(1)
    }

    fun resetToTabs() {
        isBackNavigation = false
        routeStack = emptyList()
    }

    LaunchedEffect(notificationNavigationTarget) {
        val target = notificationNavigationTarget ?: return@LaunchedEffect
        isBackNavigation = false

        when (target.navigateTo) {
            "friends" -> {
                routeStack = emptyList()
                currentDestination = AppDestinations.FRIENDS
                requestedFriendsTab = 1
            }

            "messages" -> {
                currentDestination = AppDestinations.MESSAGES
                if (target.conversationId.isBlank()) {
                    routeStack = emptyList()
                } else {
                    routeStack =
                        listOf(
                            AppRoute.Conversation(
                                conversationId = target.conversationId,
                                title = target.title.ifBlank { "Conversation" },
                            ),
                        )
                }
            }
        }

        onNotificationNavigationHandled()
    }

    val usesAppPalette = themeViewModel.appTheme != AppTheme.DYNAMIC
    val topBarContentColor =
        if (usesAppPalette) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurface
        }

    Scaffold(
        bottomBar = {
            RippleNavigationBar {
                AppDestinations.entries.forEach { destination ->
                    RippleNavigationBarItem(
                        selected = currentDestination == destination,
                        onClick = {
                            resetToTabs()
                            currentDestination = destination
                        },
                        icon = destination.icon,
                        label = destination.label
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(bottom = innerPadding.calculateBottomPadding()),
        ) {
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
                            ).statusBarsPadding()
                            .height(56.dp)
                            .fillMaxWidth(),
                )
                {
                    if (routeStack.isEmpty()) {
                        HelpfulLinksMenuButton(
                            modifier =
                                Modifier
                                    .align(Alignment.CenterStart)
                                    .padding(start = 8.dp),
                            tint = topBarContentColor,
                        )
                    } else {
                        IconButton(
                            onClick = { popRoute() },
                            modifier =
                                Modifier
                                    .align(Alignment.CenterStart)
                                    .padding(start = 8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = topBarContentColor,
                            )
                        }
                    }

                    val title =
                        when (val currentRoute = route) {
                            is AppRoute.Conversation -> {
                                currentRoute.title
                            }

                            is AppRoute.Events -> {
                                "Events"
                            }

                            is AppRoute.UserProfile -> {
                                val displayName =
                                    profileTitlesByUserId[currentRoute.userId]
                                        .orEmpty()
                                        .ifBlank { currentRoute.displayName }

                                if (displayName.isBlank()) "User Profile" else "$displayName's Profile"
                            }

                            is AppRoute.ClubProfile -> {
                                "Club Profile"
                            }

                            is AppRoute.UserGroupProfile -> {
                                "Group Profile"
                            }

                            is AppRoute.EventProfile -> {
                                "Event Profile"
                            }

                            AppRoute.MainTabs -> {
                                currentDestination.label
                            }
                        }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.align(Alignment.Center),
                        color = topBarContentColor,
                    )

                    if (route is AppRoute.Conversation) {
                        IconButton(
                            onClick = { showClearChatDialog = true },
                            modifier =
                                Modifier
                                    .align(Alignment.CenterEnd)
                                    .padding(end = 8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Clear Chat",
                                tint = topBarContentColor,
                            )
                        }
                    }
                }
            }

            if (showClearChatDialog && route is AppRoute.Conversation) {
                AlertDialog(
                    onDismissRequest = { showClearChatDialog = false },
                    title = { Text("Clear Chat History") },
                    text = { Text("This will delete all messages for you. Are you sure?") },
                    confirmButton = {
                        RippleButton(
                            text = "Clear",
                            onClick = {
                                messagesViewModel.clearChatHistory(route.conversationId) {
                                    showClearChatDialog = false
                                }
                            },
                        )
                    },
                    dismissButton = {
                        RippleOutlinedButton(
                            text = "Cancel",
                            onClick = { showClearChatDialog = false },
                        )
                    },
                )
            }

            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                when (val currentRoute = route) {
                    AppRoute.MainTabs -> {
                        when (currentDestination) {
                            AppDestinations.HOME -> {
                                HomeScreen(
                                    onOpenEventProfile = { ownerUserId, eventId, groupId ->
                                        navigateTo(AppRoute.EventProfile(eventId, ownerUserId, groupId))
                                    },
                                    onAddEvent = { navigateTo(AppRoute.Events) },
                                )
                            }

                            AppDestinations.MAP -> {
                                MapScreen()
                            }

                            AppDestinations.FRIENDS -> {
                                FriendsScreen(
                                    requestedSelectedTab = requestedFriendsTab,
                                    onSelectedTabRequestHandled = {
                                        requestedFriendsTab = null
                                    },
                                    onOpenConversation = { conversationId, convName ->
                                        navigateTo(AppRoute.Conversation(conversationId, convName))
                                    },
                                    onOpenUserProfile = { userId, displayName ->
                                        navigateToUserProfile(userId, displayName)
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
                                ProfileScreen(
                                    themeViewModel = themeViewModel,
                                    onSignOut = onSignOut,
                                )
                            }
                        }
                    }

                    is AppRoute.Events -> {
                        EventsScreen(
                            onOpenEventProfile = { ownerUserId, eventId, groupId ->
                                navigateTo(
                                    AppRoute.EventProfile(
                                        eventId = eventId,
                                        ownerUserId = ownerUserId,
                                        groupId = groupId,
                                    ),
                                )
                            },
                        )
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
                        AnimatedContent(
                            targetState = currentRoute,
                            label = "UserProfileTransition",
                            transitionSpec = {
                                val direction = if (isBackNavigation) -1 else 1

                                (
                                    slideInHorizontally(animationSpec = tween(220)) { width -> direction * width / 4 } +
                                        fadeIn(animationSpec = tween(220))
                                ).togetherWith(
                                    slideOutHorizontally(animationSpec = tween(180)) { width -> -direction * width / 4 } +
                                        fadeOut(animationSpec = tween(180)),
                                ).using(SizeTransform(clip = false))
                            },
                        ) { profileRoute ->
                            UserProfileScreen(
                                userId = profileRoute.userId,
                                onBack = { popRoute() },
                                onProfileLoaded = { loadedUserId, loadedName ->
                                    if (loadedName.isNotBlank()) {
                                        profileTitlesByUserId = profileTitlesByUserId + (loadedUserId to loadedName)
                                    }
                                },
                                onOpenConversation = { conversationId, conversationName ->
                                    navigateTo(AppRoute.Conversation(conversationId, conversationName))
                                },
                                onOpenUserProfile = { userId, displayName ->
                                    navigateToUserProfile(userId, displayName)
                                },
                                onOpenClubProfile = { clubId ->
                                    navigateTo(AppRoute.ClubProfile(clubId))
                                },
                                onOpenEventProfile = { eventId, eventOwnerUserId, eventGroupId ->
                                    navigateTo(
                                        AppRoute.EventProfile(
                                            eventId = eventId,
                                            ownerUserId = eventOwnerUserId,
                                            groupId = eventGroupId,
                                        ),
                                    )
                                },
                                messagesViewModel = messagesViewModel,
                            )
                        }
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
                                navigateToUserProfile(userId)
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
                            onOpenUserProfile = { userId ->
                                navigateToUserProfile(userId)
                            },
                            onOpenEventProfile = { eventId, ownerUserId, groupId ->
                                navigateTo(AppRoute.EventProfile(eventId, ownerUserId, groupId))
                            },
                        )
                    }

                    is AppRoute.EventProfile -> {
                        EventProfileScreen(
                            eventId = currentRoute.eventId,
                            ownerUserId = currentRoute.ownerUserId,
                            groupId = currentRoute.groupId,
                            onBack = { popRoute() },
                            onOpenUserProfile = { userId ->
                                navigateToUserProfile(userId)
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
}
