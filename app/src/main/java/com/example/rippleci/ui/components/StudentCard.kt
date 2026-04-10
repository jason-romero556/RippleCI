package com.example.rippleci.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.text.HtmlCompat
import coil.compose.AsyncImage
import com.example.rippleci.data.models.SchoolEvent
import com.example.rippleci.data.models.UserProfile

@Composable
fun StudentCard(
    user: UserProfile,
    isFriend: Boolean,
    isPending: Boolean,
    onAddFriend: () -> Unit,
    onRemoveFriend: () -> Unit,
    onMessage: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val clubsText = user.clubs.joinToString(", ").ifBlank { "None" }
    val classesText = user.classes.joinToString(", ").ifBlank { "None" }
    var showRemoveDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
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
                    Text(user.name.ifBlank { "Unknown" }, style = MaterialTheme.typography.titleMedium)

                    if (user.bio.isNotBlank()) {
                        Text(
                            text = user.bio,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                }

                when {
                    isFriend -> {
                        OutlinedButton(onClick = { showRemoveDialog = true }) {
                            Text("Friends")
                        }
                    }

                    isPending -> {
                        OutlinedButton(onClick = {}, enabled = false) {
                            Text("Pending")
                        }
                    }

                    else -> {
                        Button(onClick = onAddFriend) {
                            Text("Add")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            InfoRow("Major", user.major.ifBlank { "Not set" })
            InfoRow("Clubs", clubsText)
            InfoRow("Classes", classesText)

            if (isFriend && onMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onMessage,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Message")
                }
            }
        }
        if (showRemoveDialog) {
            AlertDialog(
                onDismissRequest = { showRemoveDialog = false },
                title = { Text("Remove Friend") },
                text = {
                    Text("Are you sure you want to remove ${user.name.ifBlank { "this user" }} as a friend?")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            onRemoveFriend()
                            showRemoveDialog = false
                        },
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                            ),
                    ) {
                        Text("Remove")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showRemoveDialog = false }) {
                        Text("Cancel")
                    }
                },
            )
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = MaterialTheme.colorScheme.secondary)
        Text(value)
    }
}
