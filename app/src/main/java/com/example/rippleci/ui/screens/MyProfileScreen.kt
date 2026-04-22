package com.example.rippleci.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.storage

@Composable
fun ProfileScreen(onSignOut: () -> Unit) {
    val auth = Firebase.auth
    val db = Firebase.firestore
    val storage = Firebase.storage
    val userId = auth.currentUser?.uid

    var name by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var major by remember { mutableStateOf("") }
    var clubs by remember { mutableStateOf("") }
    var classes by remember { mutableStateOf("") }
    var profilePictureUrl by remember { mutableStateOf("") }
    var isEditing by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }
    var isUploading by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            isUploading = true
            val storageRef = storage.reference
                .child("profile_pictures/$userId.jpg")

            storageRef.putFile(it)
                .addOnSuccessListener {
                    storageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                        profilePictureUrl = downloadUrl.toString()
                        userId?.let { uid ->
                            db.collection("users").document(uid)
                                .update("profilePictureUrl", profilePictureUrl)
                        }
                        isUploading = false
                        statusMessage = "Profile picture updated!"
                    }
                }
                .addOnFailureListener { e ->
                    isUploading = false
                    statusMessage = "Upload failed: ${e.message}"
                }
        }
    }

    LaunchedEffect(userId) {
        userId?.let {
            db.collection("users").document(it).get()
                .addOnSuccessListener { doc ->
                    name = doc.getString("name") ?: ""
                    bio = doc.getString("bio") ?: ""
                    major = doc.getString("major") ?: ""
                    clubs = (doc.get("clubs") as? List<*>)?.joinToString(", ") ?: ""
                    classes = (doc.get("classes") as? List<*>)?.joinToString(", ") ?: ""
                    profilePictureUrl = doc.getString("profilePictureUrl") ?: ""
                }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        Text("My Profile", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(16.dp))

        // Profile picture
        Box(contentAlignment = Alignment.BottomEnd) {
            if (profilePictureUrl.isNotEmpty()) {
                AsyncImage(
                    model = profilePictureUrl,
                    contentDescription = "Profile Picture",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .clickable { imagePickerLauncher.launch("image/*") }
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .clickable { imagePickerLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.AccountCircle,
                        contentDescription = "Add Photo",
                        modifier = Modifier.size(100.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            Icon(
                Icons.Default.Edit,
                contentDescription = "Edit Photo",
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape),
                tint = MaterialTheme.colorScheme.primary
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
            color = MaterialTheme.colorScheme.secondary
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (isEditing) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = bio,
                onValueChange = { bio = it },
                label = { Text("Bio") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = major,
                onValueChange = { major = it },
                label = { Text("Major") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = clubs,
                onValueChange = { clubs = it },
                label = { Text("Clubs (comma separated)") },
                placeholder = { Text("e.g. ACM, Robotics Club") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = classes,
                onValueChange = { classes = it },
                label = { Text("Classes (comma separated)") },
                placeholder = { Text("e.g. CS 101, MATH 150") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    userId?.let {
                        val profile = hashMapOf(
                            "name" to name,
                            "bio" to bio,
                            "email" to (auth.currentUser?.email ?: ""),
                            "major" to major,
                            "clubs" to clubs.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                            "classes" to classes.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                            "profilePictureUrl" to profilePictureUrl
                        )
                        db.collection("users").document(it)
                            .set(profile)
                            .addOnSuccessListener {
                                statusMessage = "Profile saved!"
                                isEditing = false
                            }
                            .addOnFailureListener { e ->
                                statusMessage = "Error: ${e.message}"
                            }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Profile")
            }

        } else {
            Text(
                text = if (name.isNotEmpty()) name else "No name set",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (bio.isNotEmpty()) bio else "No bio yet",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(16.dp))

            ProfileInfoRow(label = "Major", value = if (major.isNotEmpty()) major else "Not set")
            Spacer(modifier = Modifier.height(8.dp))
            ProfileInfoRow(label = "Clubs", value = if (clubs.isNotEmpty()) clubs else "Not set")
            Spacer(modifier = Modifier.height(8.dp))
            ProfileInfoRow(label = "Classes", value = if (classes.isNotEmpty()) classes else "Not set")
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { isEditing = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Edit Profile")
            }
        }

        if (statusMessage.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = statusMessage, color = MaterialTheme.colorScheme.primary)
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedButton(
            onClick = onSignOut,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Sign Out")
        }
    }
}

@Composable
fun ProfileInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = "$label:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}
