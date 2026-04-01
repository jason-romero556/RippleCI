package com.example.rippleci.ui.main

import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.rippleci.ui.screens.HomeScreen
import com.example.rippleci.ui.screens.FriendsScreen
import com.example.rippleci.ui.screens.ProfileScreen
import com.example.rippleci.ui.events.EventsScreen

@Composable
fun MainApp(onSignOut: () -> Unit) {
    var currentDestination by remember { mutableStateOf(AppDestinations.MAP) }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                item(
                    icon = {
                        Icon(it.icon, contentDescription = it.label)
                    },
                    label = { Text(it.label) },
                    selected = it == currentDestination,
                    onClick = { currentDestination = it }
                )
            }
        }
    ) {
        when (currentDestination) {
            AppDestinations.MAP -> HomeScreen()
            AppDestinations.FRIENDS -> FriendsScreen()
            AppDestinations.PROFILE -> ProfileScreen(onSignOut = onSignOut)
            AppDestinations.EVENTS -> EventsScreen()
        }
    }
}
