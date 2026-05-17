package com.example.rippleci.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.rippleci.ui.components.RippleOutlinedButton
import androidx.compose.ui.unit.dp

@Composable
fun ProfileInfoRow(
    label: String,
    value: String,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary,
        )
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun UserLinkRow(
    label: String,
    onClick: () -> Unit,
) {
    RippleOutlinedButton(
        text = label,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
fun ClubLinkRow(
    label: String,
    onClick: () -> Unit,
) {
    RippleOutlinedButton(
        text = label,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    )
}
