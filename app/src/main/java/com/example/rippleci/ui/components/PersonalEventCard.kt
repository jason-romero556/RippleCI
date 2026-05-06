package com.example.rippleci.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.rippleci.data.isPastEvent
import com.example.rippleci.data.models.PersonalEvent

@Composable
fun PersonalEventCard(
    event: PersonalEvent,
    onClick: () -> Unit = {},
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(event.title, style = MaterialTheme.typography.titleMedium)
            if (event.isPastEvent()) {
                Text("Past event", color = MaterialTheme.colorScheme.secondary)
            }
            Text(event.description, style = MaterialTheme.typography.bodyMedium)
            Text("${event.date} - ${event.startTime} to ${event.endTime}")
            if (event.location.isNotBlank()) {
                Text(event.location, color = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}
