@file:OptIn(ExperimentalPermissionsApi::class)

package com.example.rippleci

import android.Manifest
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.ui.unit.sp
import com.google.firebase.storage.storage
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.google.firebase.storage.storage
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.filled.Person
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.AccountBox
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.rippleci.ui.theme.RippleCITheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RippleCITheme {
                val auth = Firebase.auth
                var isLoggedIn by remember { mutableStateOf(auth.currentUser != null) }

                if (isLoggedIn) {
                    MainApp(onSignOut = {
                        auth.signOut()
                        isLoggedIn = false
                    })
                } else {
                    LoginScreen(onLoginSuccess = { isLoggedIn = true })
                }
            }
        }
    }
}

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    val auth = Firebase.auth
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var isSignUp by remember { mutableStateOf(false) }

    TextButton(onClick = {
        if (email.isBlank()) {
            errorMessage = "Enter your email above first"
        } else {
            auth.sendPasswordResetEmail(email)
                .addOnSuccessListener {
                    errorMessage = "Password reset email sent!"
                }
                .addOnFailureListener { e ->
                    errorMessage = e.message ?: "Failed to send reset email"
                }
        }
    }) {
        Text("Forgot Password?")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (isSignUp) "Create Account" else "Welcome to RippleCI",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (errorMessage.isNotEmpty()) {
            Text(text = errorMessage, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(8.dp))
        }

        Button(
            onClick = {
                if (email.isBlank() || password.isBlank()) {
                    errorMessage = "Please fill in all fields"
                    return@Button
                }
                if (isSignUp) {
                    auth.createUserWithEmailAndPassword(email, password)
                        .addOnSuccessListener { onLoginSuccess() }
                        .addOnFailureListener { e -> errorMessage = e.message ?: "Signup failed" }
                } else {
                    auth.signInWithEmailAndPassword(email, password)
                        .addOnSuccessListener { onLoginSuccess() }
                        .addOnFailureListener { e -> errorMessage = e.message ?: "Login failed" }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isSignUp) "Sign Up" else "Login")
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(onClick = { isSignUp = !isSignUp }) {
            Text(if (isSignUp) "Already have an account? Login" else "No account? Sign Up")
        }
    }
}

@Composable
fun MainApp(onSignOut: () -> Unit) {
    var currentDestination by remember { mutableStateOf(AppDestinations.MAP) }

    androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                item(
                    icon = {
                        Icon(it.icon, contentDescription = it.label)
                    },
                    label = { Text(it.label) },
                    selected = it == currentDestination,
                    onClick = { currentDestination = it }
                )
            }
        }
    ) {
        when (currentDestination) {
            AppDestinations.MAP -> HomeScreen()
            AppDestinations.FRIENDS -> FriendsScreen()
            AppDestinations.PROFILE -> ProfileScreen(onSignOut = onSignOut)
        }
    }
}

@Composable
fun HomeScreen() {
    // 1. Set up the permission state
    val locationPermissionState = rememberPermissionState(
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    // 2. Zoomed-in starting position (15f is street level)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(34.1621, -119.0435), 15f)
    }

    if (locationPermissionState.status.isGranted) {
        // 3. ONLY show the map with location enabled if permission is granted
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(isMyLocationEnabled = true),
            uiSettings = MapUiSettings(myLocationButtonEnabled = true)
        )
    } else {
        // 4. Show a button to request permission if we don't have it
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
            Text("We need your location to show where you are on the map.")
            Button(onClick = { locationPermissionState.launchPermissionRequest() }) {
                Text("Grant Permission")
            }
        }
    }
}

@Composable
fun FavoritesScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Favorites Screen")
    }
}

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

@Composable
fun FriendsScreen() {
    val db = Firebase.firestore
    val auth = Firebase.auth
    val currentUserId = auth.currentUser?.uid
    var incomingRequests by remember { mutableStateOf<List<Pair<String, Map<String, Any>>>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<Pair<String, Map<String, Any>>>>(emptyList()) }
    var friendIds by remember { mutableStateOf<List<String>>(emptyList()) }
    var friendProfiles by remember { mutableStateOf<List<Pair<String, Map<String, Any>>>>(emptyList()) }
    var pendingRequestIds by remember { mutableStateOf<List<String>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) }

    LaunchedEffect(currentUserId) {
        currentUserId?.let { uid ->
            db.collection("users").document(uid)
                .addSnapshotListener { doc, _ ->
                    val ids = (doc?.get("friends") as? List<*>)
                        ?.mapNotNull { it as? String } ?: emptyList()
                    friendIds = ids

                    if (ids.isNotEmpty()) {
                        db.collection("users").whereIn("__name__", ids).get()
                            .addOnSuccessListener { result ->
                                friendProfiles = result.documents.map { d ->
                                    Pair(d.id, d.data ?: emptyMap())
                                }
                            }
                    } else {
                        friendProfiles = emptyList()
                    }
                }

            db.collection("friendRequests")
                .whereEqualTo("fromUserId", uid)
                .whereEqualTo("status", "pending")
                .addSnapshotListener { snapshot, _ ->
                    pendingRequestIds = snapshot?.documents
                        ?.mapNotNull { it.getString("toUserId") } ?: emptyList()
                }

            db.collection("friendRequests")
                .whereEqualTo("toUserId", uid)
                .whereEqualTo("status", "pending")
                .addSnapshotListener { snapshot, _ ->
                    val requests = snapshot?.documents?.map { doc ->
                        Pair(doc.id, doc.data ?: emptyMap())
                    } ?: emptyList()
                    incomingRequests = requests
                }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        Text("Friends", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(16.dp))

        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("My Friends (${friendIds.size})") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Requests (${incomingRequests.size})") }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("Find Students") }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (selectedTab) {
            0 -> {
                if (friendProfiles.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No friends yet!\nSearch for students to add.",
                            color = MaterialTheme.colorScheme.secondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else {
                    friendProfiles.forEach { (friendId, user) ->
                        StudentCard(
                            userId = friendId,
                            user = user,
                            isFriend = true,
                            isPending = false,
                            onAddFriend = {},
                            onRemoveFriend = {
                                currentUserId?.let { uid ->
                                    db.collection("users").document(uid)
                                        .update(
                                            "friends",
                                            com.google.firebase.firestore.FieldValue.arrayRemove(friendId)
                                        )
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }

            1 -> {
                if (incomingRequests.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No pending requests",
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                } else {
                    incomingRequests.forEach { (requestId, request) ->
                        val fromUserId = request["fromUserId"] as? String ?: ""
                        val fromUserName = request["fromUserName"] as? String ?: "Unknown"

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.AccountCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.secondary
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = fromUserName,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "Wants to be your friend",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }

                                Button(
                                    onClick = {
                                        currentUserId?.let { uid ->
                                            val batch = db.batch()
                                            val requestRef = db.collection("friendRequests").document(requestId)
                                            batch.update(requestRef, "status", "accepted")
                                            val currentUserRef = db.collection("users").document(uid)
                                            val otherUserRef = db.collection("users").document(fromUserId)
                                            batch.update(currentUserRef, "friends",
                                                com.google.firebase.firestore.FieldValue.arrayUnion(fromUserId))
                                            batch.update(otherUserRef, "friends",
                                                com.google.firebase.firestore.FieldValue.arrayUnion(uid))
                                            batch.commit()
                                        }
                                    }
                                ) {
                                    Text("Accept")
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                OutlinedButton(
                                    onClick = {
                                        db.collection("friendRequests").document(requestId)
                                            .update("status", "denied")
                                    }
                                ) {
                                    Text("Deny")
                                }
                            }
                        }
                    }
                }
            }

            2 -> {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search by major, club, or class") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = {
                            if (searchQuery.isNotBlank()) {
                                isSearching = true
                                val query = searchQuery.trim().lowercase()
                                db.collection("users").get()
                                    .addOnSuccessListener { result ->
                                        searchResults = result.documents
                                            .filter { doc ->
                                                if (doc.id == currentUserId) return@filter false
                                                val major = (doc.getString("major") ?: "").lowercase()
                                                val majorMatch = major.contains(query)
                                                val clubMatch = (doc.get("clubs") as? List<*>)
                                                    ?.any { it.toString().lowercase() == query } ?: false
                                                val classMatch = (doc.get("classes") as? List<*>)
                                                    ?.any { it.toString().lowercase() == query } ?: false
                                                majorMatch || clubMatch || classMatch
                                            }
                                            .map { doc -> Pair(doc.id, doc.data ?: emptyMap()) }
                                        isSearching = false
                                    }
                                    .addOnFailureListener { isSearching = false }
                            }
                        }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (isSearching) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                } else if (searchResults.isEmpty() && searchQuery.isNotBlank()) {
                    Text("No students found", color = MaterialTheme.colorScheme.secondary)
                } else {
                    searchResults.forEach { (userId, user) ->
                        StudentCard(
                            userId = userId,
                            user = user,
                            isFriend = friendIds.contains(userId),
                            isPending = pendingRequestIds.contains(userId),
                            onAddFriend = {
                                currentUserId?.let { uid ->
                                    val request = hashMapOf(
                                        "fromUserId" to uid,
                                        "fromUserName" to (auth.currentUser?.email ?: ""),
                                        "toUserId" to userId,
                                        "status" to "pending",
                                        "timestamp" to System.currentTimeMillis()
                                    )
                                    db.collection("friendRequests").add(request)
                                }
                            },
                            onRemoveFriend = {
                                currentUserId?.let { uid ->
                                    db.collection("users").document(uid)
                                        .update(
                                            "friends",
                                            com.google.firebase.firestore.FieldValue.arrayRemove(userId)
                                        )
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun StudentCard(
    userId: String,
    user: Map<String, Any>,
    isFriend: Boolean,
    isPending: Boolean,
    onAddFriend: () -> Unit,
    onRemoveFriend: () -> Unit
) {
    val name = user["name"] as? String ?: "Unknown"
    val major = user["major"] as? String ?: "No major set"
    val bio = user["bio"] as? String ?: ""
    val clubs = (user["clubs"] as? List<*>)?.joinToString(", ") ?: "None"
    val classes = (user["classes"] as? List<*>)?.joinToString(", ") ?: "None"
    val profilePictureUrl = user["profilePictureUrl"] as? String ?: ""

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (profilePictureUrl.isNotEmpty()) {
                    AsyncImage(
                        model = profilePictureUrl,
                        contentDescription = "Profile Picture",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                    )
                } else {
                    Icon(
                        Icons.Default.AccountCircle,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(text = name, style = MaterialTheme.typography.titleMedium)
                    if (bio.isNotEmpty()) {
                        Text(
                            text = bio,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                when {
                    isFriend -> OutlinedButton(onClick = onRemoveFriend) {
                        Text("Friends")
                    }
                    isPending -> OutlinedButton(onClick = {}, enabled = false) {
                        Text("Pending")
                    }
                    else -> Button(onClick = onAddFriend) {
                        Text("Add")
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            ProfileInfoRow(label = "Major", value = major)
            ProfileInfoRow(label = "Clubs", value = clubs)
            ProfileInfoRow(label = "Classes", value = classes)
        }
    }
}
enum class AppDestinations(
    val label: String,
    val icon: ImageVector,
) {
    MAP("Map", Icons.Default.Place),
    FRIENDS("Friends", Icons.Default.Person),
    PROFILE("Profile", Icons.Default.AccountBox),
}