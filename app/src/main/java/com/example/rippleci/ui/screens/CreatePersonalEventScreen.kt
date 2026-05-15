package com.example.rippleci.ui.screens

import android.os.Build
import android.net.Uri
import android.widget.NumberPicker
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.rippleci.data.models.PersonalEvent
import com.example.rippleci.ui.components.EventVisibilityOptions
import com.example.rippleci.ui.components.ImageUploadControls
import com.example.rippleci.ui.components.VisibilitySelector
import com.example.rippleci.ui.components.createImageCaptureUri
import com.google.firebase.Firebase
import com.google.firebase.storage.storage
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePersonalEventScreen(
    initialEvent: PersonalEvent? = null,
    saveButtonText: String = if (initialEvent == null) "Save Event" else "Save Changes",
    onSave: (PersonalEvent) -> Unit = {},
    onCancel: () -> Unit = {},
) {
    val context = LocalContext.current
    val storage = Firebase.storage
    val imageStorageKey =
        remember(initialEvent?.id) {
            initialEvent?.id?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()
        }

    var title by remember(initialEvent?.id) { mutableStateOf(initialEvent?.title.orEmpty()) }
    var description by remember(initialEvent?.id) { mutableStateOf(initialEvent?.description.orEmpty()) }
    var location by remember(initialEvent?.id) { mutableStateOf(initialEvent?.location.orEmpty()) }
    var imageUrl by remember(initialEvent?.id) { mutableStateOf(initialEvent?.imageUrl.orEmpty()) }
    var isUploadingImage by remember { mutableStateOf(false) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var selectedDateMillis by remember(initialEvent?.id) {
        mutableStateOf(initialEvent?.startAtMillis?.takeIf { it > 0L }?.let(::localDateStartMillis))
    }
    var selectedStartMinutes by remember(initialEvent?.id) {
        mutableStateOf(initialEvent?.startAtMillis?.takeIf { it > 0L }?.let(::minutesOfDay))
    }
    var selectedEndMinutes by remember(initialEvent?.id) {
        mutableStateOf(initialEvent?.endAtMillis?.takeIf { it > 0L }?.let(::minutesOfDay))
    }
    var showDateDialog by remember { mutableStateOf(false) }
    var showStartTimeDialog by remember { mutableStateOf(false) }
    var showEndTimeDialog by remember { mutableStateOf(false) }
    var visibility by remember(initialEvent?.id) { mutableStateOf(initialEvent?.visibility ?: "public") }
    var statusMessage by remember { mutableStateOf("") }

    val dateLabel = selectedDateMillis?.let { formatEventDate(it) } ?: "Select date"
    val startTimeLabel = selectedStartMinutes?.let { formatEventTime(it) } ?: "Select start time"
    val endTimeLabel = selectedEndMinutes?.let { formatEventTime(it) } ?: "Select end time"
    val startAtMillis = combineEventMillis(selectedDateMillis, selectedStartMinutes)
    val endAtMillis = combineEventMillis(selectedDateMillis, selectedEndMinutes)
    val canAttemptSave =
        title.isNotBlank() &&
            selectedDateMillis != null &&
            selectedStartMinutes != null &&
            selectedEndMinutes != null

    fun uploadEventImage(uri: Uri) {
        isUploadingImage = true
        statusMessage = ""

        val storageRef =
            storage.reference.child("event_images/$imageStorageKey/${System.currentTimeMillis()}.jpg")

        storageRef
            .putFile(uri)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                    imageUrl = downloadUrl.toString()
                    isUploadingImage = false
                }
            }.addOnFailureListener { error ->
                isUploadingImage = false
                statusMessage = "Image upload failed: ${error.message}"
            }
    }

    val imagePickerLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent(),
        ) { uri: Uri? ->
            uri?.let(::uploadEventImage)
        }

    val cameraLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.TakePicture(),
        ) { didCapture ->
            val uri = pendingCameraUri
            if (didCapture && uri != null) {
                uploadEventImage(uri)
            } else {
                statusMessage = "No photo captured."
            }
        }

    if (showDateDialog) {
        val colorScheme = MaterialTheme.colorScheme
        val baseTypography = MaterialTheme.typography
        val calendarTypography =
            baseTypography.copy(
                titleLarge = baseTypography.titleLarge.copy(shadow = null),
                titleMedium = baseTypography.titleMedium.copy(shadow = null),
                bodyLarge = baseTypography.bodyLarge.copy(shadow = null),
                bodyMedium = baseTypography.bodyMedium.copy(shadow = null),
                bodySmall = baseTypography.bodySmall.copy(shadow = null),
                labelLarge = baseTypography.labelLarge.copy(shadow = null),
            )

        val datePickerState =
            rememberDatePickerState(
                initialSelectedDateMillis =
                    datePickerLocalDateMillisToUtcMillis(
                        selectedDateMillis ?: System.currentTimeMillis(),
                    ),
                initialDisplayMode = DisplayMode.Picker,
            )

        MaterialTheme(
            colorScheme = colorScheme,
            typography = calendarTypography,
        ) {
            DatePickerDialog(
                onDismissRequest = { showDateDialog = false },
                colors =
                    DatePickerDefaults.colors(
                        containerColor = colorScheme.surface,
                        titleContentColor = colorScheme.onSurface,
                        headlineContentColor = colorScheme.onSurface,
                        navigationContentColor = colorScheme.primary,
                        selectedDayContainerColor = colorScheme.primary,
                        selectedDayContentColor = colorScheme.onPrimary,
                        todayContentColor = colorScheme.primary,
                        todayDateBorderColor = colorScheme.primary,
                        selectedYearContainerColor = colorScheme.primary,
                        selectedYearContentColor = colorScheme.onPrimary,
                    ),
                confirmButton = {
                    Button(onClick = {
                        datePickerState.selectedDateMillis?.let { utcMillis ->
                            selectedDateMillis = datePickerUtcMillisToLocalDateMillis(utcMillis)
                        }
                        showDateDialog = false
                        statusMessage = ""
                    }) {
                        Text("Done")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDateDialog = false }) {
                        Text("Cancel")
                    }
                },
            ) {
                DatePicker(
                    state = datePickerState,
                    showModeToggle = false,
                    colors =
                        DatePickerDefaults.colors(
                            selectedDayContainerColor = colorScheme.primary,
                            selectedDayContentColor = colorScheme.onPrimary,
                            todayContentColor = colorScheme.primary,
                            todayDateBorderColor = colorScheme.primary,
                        ),
                )
            }
        }
    }

    if (showStartTimeDialog) {
        WheelTimePickerDialog(
            title = "Start time",
            initialMinutes = selectedStartMinutes ?: defaultEventTimeMinutes(),
            onDismiss = { showStartTimeDialog = false },
            onConfirm = {
                selectedStartMinutes = it
                showStartTimeDialog = false
                statusMessage = ""
            },
        )
    }

    if (showEndTimeDialog) {
        WheelTimePickerDialog(
            title = "End time",
            initialMinutes = selectedEndMinutes ?: selectedStartMinutes?.plus(60) ?: defaultEventTimeMinutes().plus(60),
            onDismiss = { showEndTimeDialog = false },
            onConfirm = {
                selectedEndMinutes = it
                showEndTimeDialog = false
                statusMessage = ""
            },
        )
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
    ) {
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Event title") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Event description") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = location,
            onValueChange = { location = it },
            label = { Text("Event location") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        Spacer(modifier = Modifier.height(12.dp))

        ImageUploadControls(
            imageUrl = imageUrl,
            isUploading = isUploadingImage,
            onChooseFromLibrary = { imagePickerLauncher.launch("image/*") },
            onUseCamera = {
                val uri = createImageCaptureUri(context, "event_images")
                pendingCameraUri = uri
                cameraLauncher.launch(uri)
            },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = { showDateDialog = true },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(dateLabel)
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = { showStartTimeDialog = true },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(startTimeLabel)
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = { showEndTimeDialog = true },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(endTimeLabel)
        }

        Spacer(modifier = Modifier.height(12.dp))

        VisibilitySelector(
            title = "Event Visibility",
            selectedValue = visibility,
            options = EventVisibilityOptions,
            onValueChange = { visibility = it },
        )

        if (statusMessage.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(statusMessage, color = MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (isUploadingImage) {
                    statusMessage = "Wait for the image upload to finish."
                    return@Button
                }

                val startMillis = startAtMillis
                val endMillis = endAtMillis
                val nowMillis = System.currentTimeMillis()

                when {
                    startMillis == null || endMillis == null -> {
                        statusMessage = "Choose a date, start time, and end time."
                    }

                    startMillis <= nowMillis -> {
                        statusMessage = "Choose a start time in the future."
                    }

                    endMillis <= startMillis -> {
                        statusMessage = "Choose an end time after the start time."
                    }

                    else -> {
                        onSave(
                            PersonalEvent(
                                id = initialEvent?.id.orEmpty(),
                                title = title.trim(),
                                ownerUserId = initialEvent?.ownerUserId.orEmpty(),
                                description = description.trim(),
                                location = location.trim(),
                                date = formatEventDate(startMillis),
                                startTime = formatEventTime(selectedStartMinutes ?: 0),
                                endTime = formatEventTime(selectedEndMinutes ?: 0),
                                startAtMillis = startMillis,
                                endAtMillis = endMillis,
                                visibility = visibility,
                                groupId = initialEvent?.groupId.orEmpty(),
                                createdByUserId = initialEvent?.createdByUserId.orEmpty(),
                                attendeeIds = initialEvent?.attendeeIds.orEmpty(),
                                invitedUserIds = initialEvent?.invitedUserIds.orEmpty(),
                                blockedUserIds = initialEvent?.blockedUserIds.orEmpty(),
                                imageUrl = imageUrl.trim(),
                            ),
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = canAttemptSave && !isUploadingImage,
        ) {
            Text(saveButtonText)
        }

        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Cancel")
        }
    }
}

@Composable
private fun WheelTimePickerDialog(
    title: String,
    initialMinutes: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    var hour by remember { mutableStateOf(toDisplayHour(initialMinutes / 60)) }
    var minute by remember { mutableStateOf(positiveModulo(initialMinutes, 60)) }
    var periodIndex by remember { mutableStateOf(if (initialMinutes / 60 >= 12) 1 else 0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                WheelNumberPicker(
                    value = hour,
                    minValue = 1,
                    maxValue = 12,
                    onValueChange = { hour = it },
                )
                WheelNumberPicker(
                    value = minute,
                    minValue = 0,
                    maxValue = 59,
                    displayedValues = (0..59).map { it.toString().padStart(2, '0') }.toTypedArray(),
                    onValueChange = { minute = it },
                )
                WheelNumberPicker(
                    value = periodIndex,
                    minValue = 0,
                    maxValue = 1,
                    displayedValues = arrayOf("AM", "PM"),
                    onValueChange = { periodIndex = it },
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(toMinutes(hour, minute, periodIndex))
                },
            ) {
                Text("Done")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun WheelNumberPicker(
    value: Int,
    minValue: Int,
    maxValue: Int,
    displayedValues: Array<String>? = null,
    onValueChange: (Int) -> Unit,
) {
    val contentColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val isDark = isSystemInDarkTheme()

    AndroidView(
        modifier =
            Modifier
                .width(92.dp)
                .height(160.dp),
        factory = { context ->
            val themedContext =
                android.view.ContextThemeWrapper(
                    context,
                    if (isDark) android.R.style.Theme_DeviceDefault_Dialog else android.R.style.Theme_DeviceDefault_Light_Dialog,
                )
            NumberPicker(themedContext).apply {
                wrapSelectorWheel = true
                this.minValue = minValue
                this.maxValue = maxValue
                this.displayedValues = displayedValues
                this.value = value
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    textColor = contentColor
                }
                setOnValueChangedListener { _, _, newValue ->
                    onValueChange(newValue)
                }
            }
        },
        update = { picker ->
            picker.displayedValues = null
            picker.minValue = minValue
            picker.maxValue = maxValue
            picker.displayedValues = displayedValues
            if (picker.value != value) {
                picker.value = value
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                picker.textColor = contentColor
            }
            picker.wrapSelectorWheel = true
        },
    )
}

private fun localDateStartMillis(millis: Long): Long =
    Calendar
        .getInstance()
        .apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

private fun minutesOfDay(millis: Long): Int =
    Calendar.getInstance().apply { timeInMillis = millis }.let {
        it.get(Calendar.HOUR_OF_DAY) * 60 + it.get(Calendar.MINUTE)
    }

private fun combineEventMillis(
    dateMillis: Long?,
    timeMinutes: Int?,
): Long? {
    if (dateMillis == null || timeMinutes == null) return null

    return Calendar
        .getInstance()
        .apply {
            timeInMillis = dateMillis
            set(Calendar.HOUR_OF_DAY, timeMinutes / 60)
            set(Calendar.MINUTE, timeMinutes % 60)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
}

private fun defaultEventTimeMinutes(): Int {
    val calendar = Calendar.getInstance()
    return calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
}

private fun positiveModulo(
    value: Int,
    divisor: Int,
): Int = ((value % divisor) + divisor) % divisor

private fun toDisplayHour(hourOfDay: Int): Int {
    val adjusted = hourOfDay % 12
    return if (adjusted == 0) 12 else adjusted
}

private fun toMinutes(
    hour: Int,
    minute: Int,
    periodIndex: Int,
): Int {
    val hourOfDay =
        when {
            periodIndex == 0 && hour == 12 -> 0
            periodIndex == 1 && hour != 12 -> hour + 12
            else -> hour
        }

    return hourOfDay * 60 + minute
}

private fun datePickerUtcMillisToLocalDateMillis(utcMillis: Long): Long {
    val utcCalendar =
        Calendar
            .getInstance(TimeZone.getTimeZone("UTC"))
            .apply { timeInMillis = utcMillis }

    return Calendar
        .getInstance()
        .apply {
            set(Calendar.YEAR, utcCalendar.get(Calendar.YEAR))
            set(Calendar.MONTH, utcCalendar.get(Calendar.MONTH))
            set(Calendar.DAY_OF_MONTH, utcCalendar.get(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
}

private fun datePickerLocalDateMillisToUtcMillis(localMillis: Long): Long {
    val localCalendar =
        Calendar
            .getInstance()
            .apply { timeInMillis = localMillis }

    return Calendar
        .getInstance(TimeZone.getTimeZone("UTC"))
        .apply {
            set(Calendar.YEAR, localCalendar.get(Calendar.YEAR))
            set(Calendar.MONTH, localCalendar.get(Calendar.MONTH))
            set(Calendar.DAY_OF_MONTH, localCalendar.get(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
}

private fun formatEventDate(millis: Long): String = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(millis))

private fun formatEventTime(minutes: Int): String {
    val calendar =
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, minutes / 60)
            set(Calendar.MINUTE, minutes % 60)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

    return SimpleDateFormat("h:mm a", Locale.getDefault()).format(calendar.time)
}
