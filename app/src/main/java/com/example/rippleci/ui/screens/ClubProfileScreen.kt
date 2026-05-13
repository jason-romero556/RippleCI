package com.example.rippleci.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.rippleci.data.models.ClubProfile
import com.example.rippleci.data.toUserProfile
import com.example.rippleci.ui.components.ProfileHeader
import com.example.rippleci.ui.components.UserLinkRow
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

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
    val db = Firebase.firestore

    var clubProfile by remember { mutableStateOf(ClubProfile()) }

    LaunchedEffect(clubId) {
        db
            .collection("clubs")
            .document(clubId)
            .get()
            .addOnSuccessListener { doc ->
                clubProfile = doc.toObject(ClubProfile::class.java)?.copy(id = doc.id) ?: ClubProfile()
            }
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
    ) {
        ProfileHeader(
            title = if (clubProfile.name.isNotBlank()) clubProfile.name else "Unknown Club",
            imageUrl = clubProfile.profilePictureUrl,
            placeholderIcon = Icons.Default.AccountBox,
            subtitle = {
                if (clubProfile.category.isNotBlank()) {
                    Text(clubProfile.category, style = MaterialTheme.typography.bodyMedium)
                }
            },
            actions = {
                if (isMember) {
                    OutlinedButton(onClick = onLeaveClub) {
                        Text("Leave")
                    }
                } else {
                    Button(onClick = onJoinClub) {
                        Text("Join")
                    }
                }
            }
        )

        if (clubProfile.description.isNotBlank()) {
            Text(clubProfile.description, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(24.dp))
        }

        Text("Members", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        clubProfile.memberIds.forEach { memberId ->
            UserLinkRow(
                label = memberId,
                onClick = { onOpenUserProfile(memberId) },
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
