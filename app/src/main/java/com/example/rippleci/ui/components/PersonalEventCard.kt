package com.example.rippleci.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rippleci.data.models.PersonalEvent
import com.example.rippleci.ui.components.EventCard
import com.example.rippleci.ui.screens.CreatePersonalEventScreen

@Composable
fun PersonalEventCard(event: PersonalEvent) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(event.title, style = MaterialTheme.typography.titleMedium)
            Text(event.description, style = MaterialTheme.typography.bodyMedium)
            Text("${event.date} • ${event.startTime} - ${event.endTime}")
            if (event.location.isNotBlank()) {
                Text(event.location, color = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}
