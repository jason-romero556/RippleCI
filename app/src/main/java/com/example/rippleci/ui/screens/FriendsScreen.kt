package com.example.rippleci.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.foundation.shape.CircleShape

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Major:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
                Text(text = major, style = MaterialTheme.typography.bodyMedium)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Clubs:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
                Text(text = clubs, style = MaterialTheme.typography.bodyMedium)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Classes:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
                Text(text = classes, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
