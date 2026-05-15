package com.example.rippleci.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import coil.compose.AsyncImage
import com.example.rippleci.data.CsuciClassYears
import com.example.rippleci.data.CsuciClubs
import com.example.rippleci.data.CsuciMajors
import com.example.rippleci.data.UserPresence
import com.example.rippleci.data.models.MESSAGE_PRIVACY_EVERYONE
import com.example.rippleci.data.models.MESSAGE_PRIVACY_FRIENDS
import com.example.rippleci.ui.components.ProfileVisibilityOptions
import com.example.rippleci.ui.components.VisibilitySelector
import com.example.rippleci.ui.theme.AppTheme
import com.example.rippleci.ui.theme.ThemeViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.storage

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    themeViewModel: ThemeViewModel,
    onSignOut: () -> Unit,
) {
    val auth = Firebase.auth
    val db = Firebase.firestore
    val storage = Firebase.storage
    val userId = auth.currentUser?.uid

    var name by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var major by remember { mutableStateOf("") }
    var majorQuery by remember { mutableStateOf("") }
    var selectedClubs by remember { mutableStateOf<Set<String>>(emptySet()) }
    var clubQuery by remember { mutableStateOf("") }
    var classYear by remember { mutableStateOf("") }
    var profilePictureUrl by remember { mutableStateOf("") }
    var isEditing by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }
    var isUploading by remember { mutableStateOf(false) }
    var visibility by remember { mutableStateOf("public") }
    var messagePrivacy by remember { mutableStateOf(MESSAGE_PRIVACY_FRIENDS) }
    var presenceMode by remember { mutableStateOf(UserPresence.AUTOMATIC) }
    var classExpanded by remember { mutableStateOf(false) }
    var messagePrivacyExpanded by remember { mutableStateOf(false) }
    var presenceExpanded by remember { mutableStateOf(false) }

    val messagePrivacyOptions =
        listOf(
            MESSAGE_PRIVACY_FRIENDS to "Friends only",
            MESSAGE_PRIVACY_EVERYONE to "Anyone",
        )

    val presenceModeOptions =
        listOf(
            UserPresence.AUTOMATIC to "Automatic",
            UserPresence.ONLINE to "Online",
            UserPresence.IDLE to "Idle",
            UserPresence.OFFLINE to "Offline",
        )

    val filteredMajors = remember(majorQuery) {
        if (majorQuery.isBlank()) {
            emptyList()
        } else {
            CsuciMajors.filter { it.contains(majorQuery, ignoreCase = true) }
        }
    }

    val filteredClubs = remember(clubQuery, selectedClubs) {
        if (clubQuery.isBlank()) {
            emptyList()
        } else {
            CsuciClubs.filter { it.contains(clubQuery, ignoreCase = true) }
        }
    }

    val imagePickerLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent(),
        ) { uri: Uri? ->
            uri?.let {
                isUploading = true
                val storageRef = storage.reference.child("profile_pictures/$userId.jpg")

                storageRef
                    .putFile(it)
                    .addOnSuccessListener {
                        storageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                            profilePictureUrl = downloadUrl.toString()
                            userId?.let { uid ->
                                db.collection("users").document(uid).update("profilePictureUrl", profilePictureUrl)
                            }
                            isUploading = false
                            statusMessage = "Profile picture updated!"
                        }
                    }.addOnFailureListener { e ->
                        isUploading = false
                        statusMessage = "Upload failed: ${e.message}"
                    }
            }
        }

    LaunchedEffect(userId) {
        userId?.let { uid ->
            db.collection("users").document(uid).get()
                .addOnSuccessListener { doc ->
                    name = doc.getString("name") ?: ""
                    bio = doc.getString("bio") ?: ""
                    major = doc.getString("major") ?: ""
                    majorQuery = major
                    selectedClubs =
                        (doc.get("clubs") as? List<*>)
                            ?.mapNotNull { it as? String }
                            ?.toSet()
                            ?: emptySet()
                    classYear =
                        doc.getString("classYear")
                            ?: (doc.get("classes") as? List<*>)?.firstOrNull() as? String
                            ?: ""
                    profilePictureUrl = doc.getString("profilePictureUrl") ?: ""
                    visibility = doc.getString("visibility") ?: "public"
                    messagePrivacy = doc.getString("messagePrivacy") ?: MESSAGE_PRIVACY_FRIENDS
                    presenceMode = doc.getString("presenceMode") ?: UserPresence.AUTOMATIC
                }
        }
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("My Profile", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(16.dp))

        Box(contentAlignment = Alignment.BottomEnd) {
            if (profilePictureUrl.isNotEmpty()) {
                AsyncImage(
                    model = profilePictureUrl,
                    contentDescription = "Profile Picture",
                    contentScale = ContentScale.Crop,
                    modifier =
                        Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .clickable { imagePickerLauncher.launch("image/*") },
                )
            } else {
                Box(
                    modifier =
                        Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .clickable { imagePickerLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.AccountCircle,
                        contentDescription = "Add Photo",
                        modifier = Modifier.size(100.dp),
                        tint = MaterialTheme.colorScheme.secondary,
                    )
                }
            }

            Icon(
                Icons.Default.Edit,
                contentDescription = "Edit Photo",
                modifier =
                    Modifier
                        .size(24.dp)
                        .clip(CircleShape),
                tint = MaterialTheme.colorScheme.primary,
            )
        }

        if (isUploading) {
            Spacer(modifier = Modifier.height(8.dp))
            CircularProgressIndicator(modifier = Modifier.size(24.dp))
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = auth.currentUser?.email ?: "",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary,
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (isEditing) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = bio,
                onValueChange = { bio = it },
                label = { Text("Bio") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(12.dp))

            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = majorQuery,
                    onValueChange = {
                        majorQuery = it
                        major = ""
                    },
                    label = { Text("Major") },
                    placeholder = { Text("Type to search...") },
                    isError = majorQuery.isNotBlank() && major.isEmpty() && filteredMajors.isEmpty(),
                    supportingText = {
                        if (majorQuery.isNotBlank() && major.isEmpty() && filteredMajors.isEmpty()) {
                            Text("No matching major found")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                DropdownMenu(
                    expanded = majorQuery.isNotBlank() && major.isEmpty() && filteredMajors.isNotEmpty(),
                    onDismissRequest = { },
                    properties = PopupProperties(focusable = false),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    filteredMajors.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                major = option
                                majorQuery = option
                            },
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            ExposedDropdownMenuBox(
                expanded = classExpanded,
                onExpandedChange = { classExpanded = !classExpanded },
            ) {
                OutlinedTextField(
                    value = classYear,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Class Year") },
                    placeholder = { Text("Select your year") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = classExpanded)
                    },
                    modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    singleLine = true,
                )
                ExposedDropdownMenu(
                    expanded = classExpanded,
                    onDismissRequest = { classExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("Not set") },
                        onClick = {
                            classYear = ""
                            classExpanded = false
                        },
                    )
                    CsuciClassYears.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                classYear = option
                                classExpanded = false
                            },
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            if (selectedClubs.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    selectedClubs.sorted().forEach { club ->
                        InputChip(
                            selected = true,
                            onClick = { selectedClubs = selectedClubs - club },
                            label = {
                                Text(club, style = MaterialTheme.typography.labelSmall)
                            },
                            trailingIcon = {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Remove $club",
                                    modifier = Modifier.size(14.dp),
                                )
                            },
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = clubQuery,
                    onValueChange = { clubQuery = it },
                    label = { Text("Clubs") },
                    placeholder = { Text("Type to search and add...") },
                    isError = clubQuery.isNotBlank() && filteredClubs.isEmpty(),
                    supportingText = {
                        if (clubQuery.isNotBlank() && filteredClubs.isEmpty()) {
                            Text("No matching club found")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                DropdownMenu(
                    expanded = clubQuery.isNotBlank() && filteredClubs.isNotEmpty(),
                    onDismissRequest = { },
                    properties = PopupProperties(focusable = false),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    filteredClubs.forEach { club ->
                        val alreadySelected = selectedClubs.contains(club)
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = alreadySelected, onCheckedChange = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(club)
                                }
                            },
                            onClick = {
                                selectedClubs =
                                    if (alreadySelected) {
                                        selectedClubs - club
                                    } else {
                                        selectedClubs + club
                                    }
                                clubQuery = ""
                            },
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            VisibilitySelector(
                title = "Profile Visibility",
                selectedValue = visibility,
                options = ProfileVisibilityOptions,
                onValueChange = { visibility = it },
            )

            Spacer(modifier = Modifier.height(12.dp))

            ExposedDropdownMenuBox(
                expanded = messagePrivacyExpanded,
                onExpandedChange = { messagePrivacyExpanded = !messagePrivacyExpanded },
            ) {
                OutlinedTextField(
                    value = messagePrivacyOptions.firstOrNull { it.first == messagePrivacy }?.second ?: "Friends only",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Messages") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = messagePrivacyExpanded)
                    },
                    modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    singleLine = true,
                )
                ExposedDropdownMenu(
                    expanded = messagePrivacyExpanded,
                    onDismissRequest = { messagePrivacyExpanded = false },
                ) {
                    messagePrivacyOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.second) },
                            onClick = {
                                messagePrivacy = option.first
                                messagePrivacyExpanded = false
                            },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            ExposedDropdownMenuBox(
                expanded = presenceExpanded,
                onExpandedChange = { presenceExpanded = !presenceExpanded },
            ) {
                OutlinedTextField(
                    value = presenceModeOptions.firstOrNull { it.first == presenceMode }?.second ?: "Automatic",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Status") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = presenceExpanded)
                    },
                    modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    singleLine = true,
                )
                ExposedDropdownMenu(
                    expanded = presenceExpanded,
                    onDismissRequest = { presenceExpanded = false },
                ) {
                    presenceModeOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.second) },
                            onClick = {
                                presenceMode = option.first
                                presenceExpanded = false
                            },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (majorQuery.isNotBlank() && major.isEmpty()) {
                        statusMessage = "Please select a valid major from the list."
                        return@Button
                    }

                    userId?.let { uid ->
                        val classValues = if (classYear.isBlank()) emptyList() else listOf(classYear)
                        val profile =
                            hashMapOf(
                                "name" to name,
                                "bio" to bio,
                                "email" to (auth.currentUser?.email ?: ""),
                                "major" to major,
                                "clubs" to selectedClubs.toList(),
                                "classYear" to classYear,
                                "classes" to classValues,
                                "profilePictureUrl" to profilePictureUrl,
                                "visibility" to visibility,
                                "messagePrivacy" to messagePrivacy,
                                "presenceMode" to presenceMode,
                                "presenceStatus" to UserPresence.statusForMode(presenceMode),
                                "presenceUpdatedAt" to System.currentTimeMillis(),
                            )

                        db.collection("users").document(uid)
                            .set(profile, SetOptions.merge())
                            .addOnSuccessListener {
                                statusMessage = "Profile saved!"
                                isEditing = false
                            }.addOnFailureListener { e ->
                                statusMessage = "Error: ${e.message}"
                            }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Save Profile")
            }
        } else {
            Text(
                text = name.ifEmpty { "No name set" },
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = bio.ifEmpty { "No bio yet" },
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(modifier = Modifier.height(16.dp))

            ProfileInfoRow(label = "Major", value = major.ifEmpty { "Not set" })
            Spacer(modifier = Modifier.height(8.dp))
            ProfileInfoRow(label = "Class Year", value = classYear.ifEmpty { "Not set" })
            Spacer(modifier = Modifier.height(8.dp))
            ProfileInfoRow(
                label = "Messages",
                value = messagePrivacyOptions.firstOrNull { it.first == messagePrivacy }?.second ?: "Friends only",
            )
            Spacer(modifier = Modifier.height(8.dp))
            ProfileInfoRow(
                label = "Clubs",
                value =
                    if (selectedClubs.isEmpty()) {
                        "Not set"
                    } else {
                        selectedClubs.sorted().joinToString(", ")
                    },
            )
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { isEditing = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Edit Profile")
            }
        }

        if (statusMessage.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = statusMessage, color = MaterialTheme.colorScheme.primary)
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(24.dp))

        // --- THEME SELECTOR SECTION ---
        ThemeSelector(themeViewModel)

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedButton(
            onClick = onSignOut,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Sign Out")
        }
    }
}

@Composable
fun ThemeSelector(viewModel: ThemeViewModel) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "App Theme",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(8.dp))

        Box {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Current Theme: ${viewModel.appTheme.label}")
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth(),
            ) {
                AppTheme.entries.forEach { theme ->
                    DropdownMenuItem(
                        text = { Text(theme.label) },
                        onClick = {
                            viewModel.setTheme(theme)
                            expanded = false
                        },
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Dark Mode", style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.weight(1f))
            Switch(
                checked = viewModel.isDarkTheme ?: isSystemInDarkTheme(),
                onCheckedChange = { viewModel.setDarkMode(it) },
            )
        }
        Text(
            text = if (viewModel.isDarkTheme == null) "Following System" else "Manual Override",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (viewModel.isDarkTheme != null) {
            TextButton(onClick = { viewModel.setDarkMode(null) }) {
                Text("Reset to System")
            }
        }
    }
}

@Composable
fun ProfileInfoRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = "$label:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}
