package com.example.rippleci.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.rippleci.data.models.PersonalEvent
import com.example.rippleci.data.models.UserProfile
import com.example.rippleci.data.toPersonalEvent
import com.example.rippleci.data.toUserProfile
import com.example.rippleci.ui.components.ClubLinkRow
import com.example.rippleci.ui.components.FriendListCard
import com.example.rippleci.ui.components.PersonalEventCard
import com.example.rippleci.ui.components.UserLinkRow
import com.google.firebase.Firebase
import com.google.firebase.firestore.auth.User
import com.google.firebase.firestore.firestore

@Composable
fun UserProfileScreen(
    userId: String,
    onBack: () -> Unit,
    onOpenUserProfile: (String) -> Unit,
    onOpenClubProfile: (String) -> Unit,
    onOpenEventProfile: (String) -> Unit,
) {
    val db = Firebase.firestore

    var userProfile by remember { mutableStateOf(UserProfile()) }
    var friendProfiles by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var personalEvents by remember { mutableStateOf<List<PersonalEvent>>(emptyList()) }
    var isFriendListExpanded by remember { mutableStateOf(true) }
    var isClubListExpanded by remember { mutableStateOf(true) }
    var isEventListExpanded by remember { mutableStateOf(true) }

    LaunchedEffect(userId) {
        db
            .collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { doc ->
                userProfile = doc.toUserProfile()

                if (userProfile.friendIds.isNotEmpty()) {
                    db
                        .collection("users")
                        .whereIn("__name__", userProfile.friendIds)
                        .get()
                        .addOnSuccessListener { result ->
                            friendProfiles = result.documents.map { it.toUserProfile() }
                        }
                }

                db
                    .collection("users")
                    .document(userId)
                    .collection("personalEvents")
                    .get()
                    .addOnSuccessListener { result ->
                        personalEvents = result.documents.map { it.toPersonalEvent() }
                    }
            }
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
    ) {
        if (userProfile.profilePictureUrl.isNotBlank()) {
            AsyncImage(
                model = userProfile.profilePictureUrl,
                contentDescription = "Profile Picture",
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier
                        .size(100.dp)
                        .clip(CircleShape),
            )
        } else {
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.secondary,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = userProfile.name.ifBlank { "Unknown User" },
            style = MaterialTheme.typography.headlineMedium,
        )

        Spacer(modifier = Modifier.height(8.dp))

        ProfileInfoRow("Major", userProfile.major.ifBlank { "Unlisted Major" })

        Spacer(modifier = Modifier.height(8.dp))

        ProfileInfoRow("Bio", userProfile.bio.ifBlank { "No bio yet" })

        Spacer(modifier = Modifier.height(24.dp))

        CollapsibleSection(
            title = "Friends List",
            expanded = isFriendListExpanded,
            onToggle = { isFriendListExpanded = !isFriendListExpanded },
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            if (friendProfiles.isEmpty()) {
                Text("No friends yet")
            } else {
                friendProfiles.forEach { friend ->
                    FriendListCard(
                        user = friend,
                        onViewProfile = { onOpenUserProfile(friend.id) },
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        CollapsibleSection(
            title = "Clubs",
            expanded = isClubListExpanded,
            onToggle = { isClubListExpanded = !isClubListExpanded },
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            if (userProfile.clubIds.isEmpty()) {
                Text("No clubs listed.")
            } else {
                userProfile.clubIds.forEach { clubId ->
                    ClubLinkRow(
                        label = clubId,
                        onClick = { onOpenClubProfile(clubId) },
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        CollapsibleSection(
            title = "Events",
            expanded = isEventListExpanded,
            onToggle = { isEventListExpanded = !isEventListExpanded },
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            if (personalEvents.isEmpty()) {
                Text("No personal events yet.")
            } else {
                personalEvents.forEach { event ->
                    PersonalEventCard(event)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}
