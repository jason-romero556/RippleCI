package com.example.rippleci.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun ProfileInfoRow(
    label: String,
    value: String,
    maxValueLines: Int = Int.MAX_VALUE,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End,
            maxLines = maxValueLines,
            overflow = TextOverflow.Ellipsis,
        )
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
