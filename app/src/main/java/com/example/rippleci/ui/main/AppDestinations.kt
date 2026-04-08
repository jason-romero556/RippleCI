package com.example.rippleci.ui.main

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.ui.graphics.vector.ImageVector

enum class AppDestinations(
    val label: String,
    val icon: ImageVector,
) {
    MAP("Map", Icons.Default.Place),
    FRIENDS("Friends", Icons.Default.Person),
    MESSAGES("Messages", Icons.Default.Email),
    PROFILE("Profile", Icons.Default.AccountBox),
    EVENTS("Events", Icons.Default.DateRange),

}
