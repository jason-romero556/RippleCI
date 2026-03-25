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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import com.example.rippleci.ui.theme.RippleCITheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import androidx.compose.foundation.layout.* //Sameen added these 5 imports (one was redundant)//
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import com.google.maps.android.compose.GoogleMap //Sameen added these 6 imports//
import com.google.maps.android.compose.rememberCameraPositionState
import androidx.compose.material.icons.filled.DateRange
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.MapProperties //Sameen added this import to enable "current location" in map//
import com.google.accompanist.permissions.* //Sameen added these two imports to enable permission asking
import android.Manifest
import com.google.maps.android.compose.MapUiSettings //Sameen added this import to fix an error

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
            AppDestinations.EVENTS -> com.example.rippleci.screens.EventsScreen()
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
        // This is the "Content" area of your app
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                // Logic to switch screens based on the button clicked
                when (currentDestination) {
                    AppDestinations.MAP -> {
                        // Call your Map function here
                        MyGoogleMap()
                    }
                    AppDestinations.HOME -> {
                        Greeting(name = "Home Screen")
                    }
                }
            }
        }
    }
}



enum class AppDestinations(
    val label: String,
    val icon: ImageVector,
) {
    HOME("Home", Icons.Default.Home),
    MAP("Map", Icons.Default.Place)
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
    EVENTS("Events", Icons.Default.DateRange),

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MyGoogleMap() {
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