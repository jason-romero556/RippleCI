package com.example.rippleci.ui.screens

import androidx.compose.runtime.*
import com.example.rippleci.data.models.ClubProfile

@Composable
fun ClubProfileScreen(
    clubId: String,
    isMember: Boolean,
    onBack: () -> Unit,
    onJoinClub: () -> Unit,
    onLeaveClub: () -> Unit,
    onViewEvents: () -> Unit,
    onOpenUserProfile: (String) -> Unit,
    onOpenEventProfile: (String) -> Unit,
    onOpenClubProfile: (String) -> Unit,
) {
}
