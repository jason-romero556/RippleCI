package com.example.rippleci.ui.screens

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rippleci.data.models.PersonalEvent
import com.example.rippleci.ui.components.EventCard
import com.example.rippleci.ui.components.PersonalEventCard
import com.example.rippleci.ui.events.EventsUiState
import com.example.rippleci.ui.events.EventsViewModel
import com.example.rippleci.ui.theme.AppTheme
import com.example.rippleci.ui.theme.ThemeViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(
    eventsViewModel: EventsViewModel = viewModel(),
    themeViewModel: ThemeViewModel,
    onAddEvent: () -> Unit
) {
    val db = Firebase.firestore
    val auth = Firebase.auth
    val userId = auth.currentUser?.uid
    var personalEvents by remember { mutableStateOf<List<PersonalEvent>>(emptyList()) }
    val uiState by eventsViewModel.uiState.collectAsState()

    // 1. Fetch Personal Events (Custom Events)
    LaunchedEffect(userId) {
        userId?.let { uid ->
            db.collection("users").document(uid).collection("personalEvents")
                .get()
                .addOnSuccessListener { result ->
                    personalEvents = result.documents.map { doc ->
                        PersonalEvent(
                            id = doc.id,
                            title = doc.getString("title").orEmpty(),
                            description = doc.getString("description").orEmpty(),
                            location = doc.getString("location").orEmpty(),
                            date = doc.getString("date").orEmpty(),
                            startTime = doc.getString("startTime").orEmpty(),
                            endTime = doc.getString("endTime").orEmpty(),
                        )
                    }
                }
        }
    }

    // 2. Helper to filter "Today's" School Events
    val todayDateString = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // --- CUSTOM EVENTS SECTION ---
            Text(
                text = "My Custom Events",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (personalEvents.isEmpty()) {
                Text("No custom events yet.", style = MaterialTheme.typography.bodyMedium)
            } else {
                personalEvents.forEach { event ->
                    PersonalEventCard(event = event)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- TODAY'S SCHOOL EVENTS SECTION ---
            Text(
                text = "Today's School Events",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            when (val state = uiState) {
                is EventsUiState.Loading -> CircularProgressIndicator()
                is EventsUiState.Error -> Text("Could not load school events.")
                is EventsUiState.Success -> {
                    // Filter events that match today's date
                    // Note: Assuming state.events.startDateTime is in a parseable format or contains the date
                    val todaysEvents = state.events.filter { event ->
                        event.startDateTime.contains(todayDateString)
                    }

                    if (todaysEvents.isEmpty()) {
                        Text(
                            "No school events scheduled for today.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        todaysEvents.forEach { event ->
                            EventCard(event = event)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // --- THEME SELECTOR SECTION ---
            ThemeSelector(themeViewModel)
        }

        FloatingActionButton(
            onClick = onAddEvent,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Event")
        }
    }
}

@Composable
fun ThemeSelector(viewModel: ThemeViewModel) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        Text(
            text = "Themes",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))

        Box {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Current Theme: ${viewModel.appTheme.label}")
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth()
            ) {
                AppTheme.entries.forEach { theme ->
                    DropdownMenuItem(
                        text = { Text(theme.label) },
                        onClick = {
                            viewModel.setTheme(theme)
                            expanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Dark Mode", style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.weight(1f))
            Switch(
                checked = viewModel.isDarkTheme ?: isSystemInDarkTheme(),
                onCheckedChange = { viewModel.setDarkMode(it) }
            )
        }
        Text(
            text = if (viewModel.isDarkTheme == null) "Following System" else "Manual Override",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (viewModel.isDarkTheme != null) {
            TextButton(onClick = { viewModel.setDarkMode(null) }) {
                Text("Reset to System")
            }
        }
    }
}
