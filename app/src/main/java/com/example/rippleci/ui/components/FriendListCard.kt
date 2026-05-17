package com.example.rippleci.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.rippleci.data.models.UserProfile
import com.example.rippleci.ui.components.RippleOutlinedButton

@Composable
fun FriendListCard(
    user: UserProfile,
    isFriend: Boolean = true,
    isPending: Boolean = false,
    hasBlockedUser: Boolean = false,
    onViewProfile: (() -> Unit)? = null,
    onAddFriend: () -> Unit = {},
    onRemoveFriend: () -> Unit = {},
    onBlockUser: () -> Unit = {},
    onUnblockUser: () -> Unit = {},
) {
    val clubsText = user.clubIds.joinToString(", ").ifBlank { "No clubs listed" }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (user.profilePictureUrl.isNotBlank()) {
                    AsyncImage(
                        model = user.profilePictureUrl,
                        contentDescription = "Profile Picture",
                        contentScale = ContentScale.Crop,
                        modifier =
                            Modifier
                                .size(48.dp)
                                .clip(CircleShape),
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.secondary,
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        user.name.ifBlank { "Unknown User" },
                        style = MaterialTheme.typography.titleMedium,
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    UserPresenceIndicator(user = user)

                    if (user.bio.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = user.bio,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary,
                            maxLines = 2,
                        )
                    }

                    Text(
                        user.major.ifBlank { "Unknown" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                FriendshipStatusMenuButton(
                    isFriend = isFriend,
                    isPending = isPending,
                    hasBlockedUser = hasBlockedUser,
                    onAddFriend = onAddFriend,
                    onRemoveFriend = onRemoveFriend,
                    onBlockUser = onBlockUser,
                    onUnblockUser = onUnblockUser,
                    compact = true,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Clubs: $clubsText",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (onViewProfile != null) {
                RippleOutlinedButton(
                    text = "View Profile",
                    onClick = onViewProfile,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
