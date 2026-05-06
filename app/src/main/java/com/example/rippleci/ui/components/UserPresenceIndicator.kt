package com.example.rippleci.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.rippleci.data.UserPresence
import com.example.rippleci.data.models.UserProfile

@Composable
fun UserPresenceIndicator(
    user: UserProfile,
    modifier: Modifier = Modifier,
) {
    val status = UserPresence.resolveStatus(user)

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Spacer(
            modifier =
                Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(UserPresence.statusColor(status)),
        )
        Text(
            text = UserPresence.statusLabel(status),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary,
        )
    }
}
