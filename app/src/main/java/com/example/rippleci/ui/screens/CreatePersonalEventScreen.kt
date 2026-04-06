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
fun CreatePersonalEventScreen(
    // Added default empty lambdas to avoid NullPointerException during Preview rendering
    onSave: (PersonalEvent) -> Unit = {},
    onCancel: () -> Unit = {},
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var dateLabel by remember { mutableStateOf("") }
    var startTimeLabel by remember { mutableStateOf("") }
    var endTimeLabel by remember { mutableStateOf("") }

    Column(modifier = Modifier.padding(20.dp)) {
        TextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Event title") },
        )

        Spacer(modifier = Modifier.height(8.dp))

        TextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Event Description") },
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                val event =
                    PersonalEvent(
                        title = title,
                        description = description,
                        location = location,
                        date = dateLabel,
                        startTime = startTimeLabel,
                        endTime = endTimeLabel,
                    )
                onSave(event)
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = title.isNotBlank(),
        ) {
            Text("Save Event")
        }
        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Cancel")
        }
    }
}
