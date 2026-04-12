package com.example.rippleci.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.rippleci.data.models.PersonalEvent
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore

@Composable
fun PersonalEventScreen(
    events: List<PersonalEvent>,
    onAddEventClick: () -> Unit,
) {
    Column {
        Button(onClick = onAddEventClick) {
            Text("Add Personal Event")
        }

        events.forEach { event ->
            Text(event.title)
            Text(event.date)
        }
    }
}
