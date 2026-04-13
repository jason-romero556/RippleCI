package com.example.rippleci.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.rippleci.data.models.FriendRequest
import com.example.rippleci.data.models.UserProfile
import com.example.rippleci.data.toFriendRequest
import com.example.rippleci.data.toUserProfile
import com.example.rippleci.ui.components.ProfileInfoRow
import com.example.rippleci.ui.components.StudentCard
import com.example.rippleci.ui.messages.MessagesViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.launch

@Composable
fun FriendsScreen(
    onOpenConversation: (String, String) -> Unit = { _, _ -> },
    messagesViewModel: MessagesViewModel,
) {
    val db = Firebase.firestore
    val auth = Firebase.auth
    val currentUserId = auth.currentUser?.uid ?: ""
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var incomingRequests by remember { mutableStateOf<List<FriendRequest>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var friendIds by remember { mutableStateOf<List<String>>(emptyList()) }
    var friendProfiles by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var pendingRequestIds by remember { mutableStateOf<List<String>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) }

    LaunchedEffect(currentUserId) {
        currentUserId?.let { uid ->
            db
                .collection("users")
                .document(uid)
                .addSnapshotListener { doc, _ ->
                    val ids =
                        (doc?.get("friends") as? List<*>)
                            ?.mapNotNull { it as? String } ?: emptyList()
                    friendIds = ids

                    if (ids.isNotEmpty()) {
                        db
                            .collection("users")
                            .whereIn("__name__", ids)
                            .get()
                            .addOnSuccessListener { result ->
                                friendProfiles =
                                    result.documents.map { it.toUserProfile() }
                            }
                    } else {
                        friendProfiles = emptyList()
                    }
                }

            db
                .collection("friendRequests")
                .whereEqualTo("fromUserId", uid)
                .whereEqualTo("status", "pending")
                .addSnapshotListener { snapshot, _ ->
                    pendingRequestIds = snapshot
                        ?.documents
                        ?.mapNotNull { it.getString("toUserId") } ?: emptyList()
                }

            db
                .collection("friendRequests")
                .whereEqualTo("toUserId", uid)
                .whereEqualTo("status", "pending")
                .addSnapshotListener { snapshot, _ ->
                    val requests =
                        snapshot?.documents?.map { doc ->
                            doc.toFriendRequest()
                        } ?: emptyList()
                    incomingRequests = requests
                }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        ) {
            // Screen Header that lines up with the top buffer
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
            ) {
                Text(
                    text = "Friends",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(16.dp),
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("My Friends (${friendIds.size})") },
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Requests (${incomingRequests.size})") },
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Find Students") },
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (selectedTab) {
                0 -> {
                    if (friendProfiles.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "No friends yet!\nSearch for students to add.",
                                color = MaterialTheme.colorScheme.secondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier.verticalScroll(rememberScrollState()),
                        ) {
                            friendProfiles.filter { user -> user.id != currentUserId }.forEach { user ->
                                StudentCard(
                                    user = user,
                                    isFriend = true,
                                    isPending = false,
                                    onAddFriend = {},
                                    onRemoveFriend = {
                                        db
                                            .collection("users")
                                            .document(currentUserId)
                                            .update(
                                                "friends",
                                                com.google.firebase.firestore.FieldValue
                                                    .arrayRemove(user.id),
                                            )
                                    },
                                    onMessage = {
                                        val friendName = user.name.ifBlank { user.email.ifBlank { "Unknown" } }

                                        messagesViewModel.getOrCreateDMConversation(
                                            otherUserId = user.id,
                                            otherUserName = friendName,
                                        ) { convId ->
                                            onOpenConversation(convId, friendName)
                                        }
                                    },
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }

                1 -> {
                    if (incomingRequests.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "No pending requests",
                                color = MaterialTheme.colorScheme.secondary,
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier.verticalScroll(rememberScrollState()),
                        ) {
                            incomingRequests.forEach { request ->
                                val fromUserId = request.fromUserId
                                val fromUserName = request.fromUserName

                                Card(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                                ) {
                                    Row(
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Icon(
                                            Icons.Default.AccountCircle,
                                            contentDescription = null,
                                            modifier = Modifier.size(48.dp),
                                            tint = MaterialTheme.colorScheme.secondary,
                                        )

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = fromUserName,
                                                style = MaterialTheme.typography.titleMedium,
                                            )
                                            Text(
                                                text = "Wants to be your friend",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.secondary,
                                            )
                                        }

                                        Button(
                                            onClick = {
                                                currentUserId?.let { uid ->
                                                    val batch = db.batch()
                                                    val requestRef =
                                                        db
                                                            .collection("friendRequests")
                                                            .document(request.id)
                                                    batch.update(requestRef, "status", "accepted")
                                                    val currentUserRef =
                                                        db
                                                            .collection("users")
                                                            .document(uid)
                                                    val otherUserRef =
                                                        db
                                                            .collection("users")
                                                            .document(fromUserId)
                                                    batch.update(
                                                        currentUserRef,
                                                        "friends",
                                                        com.google.firebase.firestore.FieldValue
                                                            .arrayUnion(fromUserId),
                                                    )
                                                    batch.update(
                                                        otherUserRef,
                                                        "friends",
                                                        com.google.firebase.firestore.FieldValue
                                                            .arrayUnion(uid),
                                                    )
                                                    batch.commit()
                                                }
                                            },
                                        ) {
                                            Text("Accept")
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        OutlinedButton(
                                            onClick = {
                                                db
                                                    .collection("friendRequests")
                                                    .document(request.id)
                                                    .update("status", "denied")
                                            },
                                        ) {
                                            Text("Deny")
                                        }
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
                                    db
                                        .collection("users")
                                        .get()
                                        .addOnSuccessListener { result ->
                                            searchResults =
                                                result.documents
                                                    .filter { doc ->
                                                        if (doc.id == currentUserId) return@filter false
                                                        val major = (doc.getString("major") ?: "").lowercase()
                                                        val majorMatch = major.contains(query)
                                                        val clubMatch =
                                                            (doc.get("clubs") as? List<*>)
                                                                ?.any { it.toString().lowercase() == query } ?: false
                                                        val classMatch =
                                                            (doc.get("classes") as? List<*>)
                                                                ?.any { it.toString().lowercase() == query } ?: false
                                                        majorMatch || clubMatch || classMatch
                                                    }.map { doc -> doc.toUserProfile() }
                                            isSearching = false
                                        }.addOnFailureListener { isSearching = false }
                                }
                            }) {
                                Icon(Icons.Default.Search, contentDescription = "Search")
                            }
                        },
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (isSearching) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                    } else if (searchResults.isEmpty() && searchQuery.isNotBlank()) {
                        Text("No students found", color = MaterialTheme.colorScheme.secondary)
                    } else {
                        Column(
                            modifier = Modifier.verticalScroll(rememberScrollState()),
                        ) {
                            searchResults.forEach { user ->
                                val user = user

                                val friendName = user.name.ifBlank { user.email.ifBlank { "Unknown" } }
                                StudentCard(
                                    user = user,
                                    isFriend = friendIds.contains(user.id),
                                    isPending = pendingRequestIds.contains(user.id),
                                    onAddFriend = {
                                        currentUserId?.let { uid ->
                                            val request =
                                                hashMapOf(
                                                    "fromUserId" to uid,
                                                    "fromUserName" to (auth.currentUser?.email ?: ""),
                                                    "toUserId" to user.id,
                                                    "status" to "pending",
                                                    "timestamp" to System.currentTimeMillis(),
                                                )
                                            db.collection("friendRequests").add(request)
                                        }
                                    },
                                    onRemoveFriend = {
                                        currentUserId.let { uid ->
                                            db
                                                .collection("users")
                                                .document(uid)
                                                .update(
                                                    "friends",
                                                    com.google.firebase.firestore.FieldValue.arrayRemove(
                                                        user.id,
                                                    ),
                                                )
                                        }
                                    },
                                    onMessage = {
                                        messagesViewModel.getOrCreateDMConversation(
                                            otherUserId = user.id,
                                            otherUserName = friendName,
                                        ) { convId ->
                                            onOpenConversation(convId, friendName)
                                        }
                                    },
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
