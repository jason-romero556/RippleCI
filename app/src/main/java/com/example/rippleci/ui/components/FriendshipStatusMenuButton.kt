package com.example.rippleci.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun FriendshipStatusMenuButton(
    isFriend: Boolean,
    isPending: Boolean,
    hasBlockedUser: Boolean,
    onAddFriend: () -> Unit,
    onRemoveFriend: () -> Unit,
    onBlockUser: () -> Unit,
    onUnblockUser: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    var expanded by remember { mutableStateOf(false) }
    val label =
        when {
            hasBlockedUser -> "Blocked"
            isFriend -> "Friends"
            isPending -> "Pending"
            else -> "Not Friends"
        }

    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier =
                if (compact) {
                    Modifier.heightIn(min = 32.dp)
                } else {
                    Modifier
                },
            contentPadding =
                if (compact) {
                    PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                } else {
                    ButtonDefaults.ContentPadding
                },
        ) {
            Text(
                text = label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style =
                    if (compact) {
                        MaterialTheme.typography.labelSmall
                    } else {
                        MaterialTheme.typography.labelLarge
                    },
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = "Manage friendship",
                modifier = Modifier.size(if (compact) 16.dp else 24.dp),
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            when {
                hasBlockedUser -> {
                    DropdownMenuItem(
                        text = { Text("Unblock") },
                        onClick = {
                            expanded = false
                            onUnblockUser()
                        },
                    )
                }

                isFriend -> {
                    DropdownMenuItem(
                        text = { Text("Remove Friend") },
                        onClick = {
                            expanded = false
                            onRemoveFriend()
                        },
                    )
                }

                isPending -> {
                    DropdownMenuItem(
                        text = { Text("Friend Request Pending") },
                        onClick = {},
                        enabled = false,
                    )
                }

                else -> {
                    DropdownMenuItem(
                        text = { Text("Send Friend Request") },
                        onClick = {
                            expanded = false
                            onAddFriend()
                        },
                    )
                }
            }

            if (!hasBlockedUser) {
                HorizontalDivider()
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "Block User",
                            color = MaterialTheme.colorScheme.error,
                        )
                    },
                    onClick = {
                        expanded = false
                        onBlockUser()
                    },
                )
            }
        }
    }
}
