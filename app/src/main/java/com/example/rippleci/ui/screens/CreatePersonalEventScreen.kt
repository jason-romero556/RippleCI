package com.example.rippleci.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.rippleci.data.models.PersonalEvent
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = confirmButton,
        dismissButton = dismissButton,
        text = { content() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePersonalEventScreen(
    onSave: (PersonalEvent) -> Unit = {},
    onCancel: () -> Unit = {},
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(LocalDate.now().toString()) }
    var startTime by remember { mutableStateOf("12:00 PM") }
    var endTime by remember { mutableStateOf("1:00 PM") }

    var showDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    var locationExpanded by remember { mutableStateOf(false) }

    val locations = listOf("Student Union", "Library", "Science Building", "Gym", "Main Quad", "Cafeteria")

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = Instant.now().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        date = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                            .toString()
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showStartTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = 12,
            initialMinute = 0,
            is24Hour = false
        )
        TimePickerDialog(
            onDismissRequest = { showStartTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val hour = if (timePickerState.hour == 0) 12 else if (timePickerState.hour > 12) timePickerState.hour - 12 else timePickerState.hour
                    val minute = String.format(Locale.getDefault(), "%02d", timePickerState.minute)
                    val amPm = if (timePickerState.hour < 12) "AM" else "PM"
                    startTime = "$hour:$minute $amPm"
                    showStartTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showStartTimePicker = false }) { Text("Cancel") }
            }
        ) {
            TimePicker(state = timePickerState)
        }
    }

    if (showEndTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = 13,
            initialMinute = 0,
            is24Hour = false
        )
        TimePickerDialog(
            onDismissRequest = { showEndTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val hour = if (timePickerState.hour == 0) 12 else if (timePickerState.hour > 12) timePickerState.hour - 12 else timePickerState.hour
                    val minute = String.format(Locale.getDefault(), "%02d", timePickerState.minute)
                    val amPm = if (timePickerState.hour < 12) "AM" else "PM"
                    endTime = "$hour:$minute $amPm"
                    showEndTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showEndTimePicker = false }) { Text("Cancel") }
            }
        ) {
            TimePicker(state = timePickerState)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Create New Event",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Event title") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Event Description") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )

        // LOCATION DROPDOWN
        Box {
            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("Event location") },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    IconButton(onClick = { locationExpanded = true }) {
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                }
            )
            DropdownMenu(
                expanded = locationExpanded,
                onDismissRequest = { locationExpanded = false },
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                locations.forEach { loc ->
                    DropdownMenuItem(
                        text = { Text(loc) },
                        onClick = {
                            location = loc
                            locationExpanded = false
                        }
                    )
                }
            }
        }

        // DATE PICKER
        OutlinedTextField(
            value = date,
            onValueChange = { },
            label = { Text("Event date") },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showDatePicker = true },
            readOnly = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            ),
            trailingIcon = {
                IconButton(onClick = { showDatePicker = true }) {
                    Icon(Icons.Default.DateRange, contentDescription = null)
                }
            }
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // START TIME PICKER
            OutlinedTextField(
                value = startTime,
                onValueChange = { },
                label = { Text("Start") },
                modifier = Modifier
                    .weight(1f)
                    .clickable { showStartTimePicker = true },
                readOnly = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                ),
                trailingIcon = {
                    IconButton(onClick = { showStartTimePicker = true }) {
                        Icon(Icons.Outlined.Schedule, contentDescription = null)
                    }
                }
            )

            // END TIME PICKER
            OutlinedTextField(
                value = endTime,
                onValueChange = { },
                label = { Text("End") },
                modifier = Modifier
                    .weight(1f)
                    .clickable { showEndTimePicker = true },
                readOnly = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                ),
                trailingIcon = {
                    IconButton(onClick = { showEndTimePicker = true }) {
                        Icon(Icons.Outlined.Schedule, contentDescription = null)
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                val event = PersonalEvent(
                    title = title,
                    description = description,
                    location = location,
                    date = date,
                    startTime = startTime,
                    endTime = endTime,
                )
                onSave(event)
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = title.isNotBlank() && location.isNotBlank(),
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
