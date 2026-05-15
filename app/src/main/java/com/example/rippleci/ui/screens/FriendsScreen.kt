package com.example.rippleci.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.example.rippleci.data.CsuciClassYears
import com.example.rippleci.data.CsuciClubs
import com.example.rippleci.data.CsuciMajors
import com.example.rippleci.data.canViewProfile
import com.example.rippleci.data.firstNameFromCandidates
import com.example.rippleci.data.models.FriendRequest
import com.example.rippleci.data.models.UserGroupInvite
import com.example.rippleci.data.models.UserGroupProfile
import com.example.rippleci.data.models.UserProfile
import com.example.rippleci.data.toFriendRequest
import com.example.rippleci.data.toUserGroupInvite
import com.example.rippleci.data.toUserGroupProfile
import com.example.rippleci.data.toUserProfile
import com.example.rippleci.ui.components.GroupVisibilityOptions
import com.example.rippleci.ui.components.StudentCard
import com.example.rippleci.ui.components.VisibilitySelector
import com.example.rippleci.ui.messages.MessagesViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(
    requestedSelectedTab: Int? = null,
    onSelectedTabRequestHandled: () -> Unit = {},
    onOpenConversation: (String, String) -> Unit = { _, _ -> },
    onOpenUserProfile: (String, String) -> Unit = { _, _ -> },
    onOpenUserGroupProfile: (String) -> Unit = {},
    messagesViewModel: MessagesViewModel,
) {
    val db = Firebase.firestore
    val auth = Firebase.auth
    val currentUserId = auth.currentUser?.uid ?: ""
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var incomingRequests by remember { mutableStateOf<List<FriendRequest>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedMajor by remember { mutableStateOf("") }
    var majorQuery by remember { mutableStateOf("") }
    var selectedClub by remember { mutableStateOf("") }
    var clubQuery by remember { mutableStateOf("") }
    var selectedClass by remember { mutableStateOf("") }
    var classExpanded by remember { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var friendIds by remember { mutableStateOf<List<String>>(emptyList()) }
    var friendProfiles by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var pendingRequestIds by remember { mutableStateOf<List<String>>(emptyList()) }
    var currentUserName by remember { mutableStateOf("") }
    var incomingRequestSenderNames by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var isSearching by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) }
    var pendingGroupInvites by remember { mutableStateOf<List<UserGroupInvite>>(emptyList()) }
    var userGroups by remember { mutableStateOf<List<UserGroupProfile>>(emptyList()) }
    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var groupName by remember { mutableStateOf("") }
    var groupDescription by remember { mutableStateOf("") }
    var groupVisibility by remember { mutableStateOf("public") }
    var blockedUserIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var blockedByUserIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    val hiddenUserIds = blockedUserIds + blockedByUserIds

    LaunchedEffect(requestedSelectedTab) {
        requestedSelectedTab?.let { tabIndex ->
            selectedTab = tabIndex
            onSelectedTabRequestHandled()
        }
    }

    val filteredMajors =
        remember(majorQuery) {
            if (majorQuery.isBlank()) {
                emptyList()
            } else {
                CsuciMajors.filter { it.contains(majorQuery, ignoreCase = true) }
            }
        }

    val filteredClubs =
        remember(clubQuery) {
            if (clubQuery.isBlank()) {
                emptyList()
            } else {
                CsuciClubs.filter { it.contains(clubQuery, ignoreCase = true) }
            }
        }

    LaunchedEffect(currentUserId) {
        currentUserId.let { uid ->
            db
                .collection("users")
                .document(uid)
                .addSnapshotListener { doc, _ ->
                    currentUserName = doc?.getString("name").orEmpty()
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
                                friendProfiles = result.documents.map { it.toUserProfile() }
                            }
                    } else {
                        friendProfiles = emptyList()
                    }
                }

            db
                .collection("blockedUsers")
                .whereEqualTo("blockerUserId", uid)
                .addSnapshotListener { snapshot, _ ->
                    blockedUserIds =
                        snapshot
                            ?.documents
                            ?.mapNotNull { it.getString("blockedUserId") }
                            ?.toSet()
                            ?: emptySet()
                }

            db
                .collection("blockedUsers")
                .whereEqualTo("blockedUserId", uid)
                .addSnapshotListener { snapshot, _ ->
                    blockedByUserIds =
                        snapshot
                            ?.documents
                            ?.mapNotNull { it.getString("blockerUserId") }
                            ?.toSet()
                            ?: emptySet()
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
                    val requests = snapshot?.documents?.map { it.toFriendRequest() } ?: emptyList()
                    val fallbackNames =
                        requests.associate { request ->
                            request.fromUserId to firstNameFromCandidates(request.fromUserName)
                        }

                    incomingRequests = requests
                    incomingRequestSenderNames = fallbackNames

                    requests
                        .map { it.fromUserId }
                        .filter { it.isNotBlank() }
                        .distinct()
                        .chunked(10)
                        .forEach { senderIds ->
                            db
                                .collection("users")
                                .whereIn("__name__", senderIds)
                                .get()
                                .addOnSuccessListener { result ->
                                    val fetchedNames =
                                        result.documents.associate { doc ->
                                            doc.id to
                                                firstNameFromCandidates(
                                                    doc.getString("name"),
                                                    fallbackNames[doc.id],
                                                )
                                        }
                                    incomingRequestSenderNames = incomingRequestSenderNames + fetchedNames
                                }
                        }
                }

            db
                .collection("userGroups")
                .whereArrayContains("memberIds", uid)
                .addSnapshotListener { snapshot, _ ->
                    userGroups = snapshot?.documents?.map { it.toUserGroupProfile() } ?: emptyList()
                }

            db
                .collection("userGroupInvites")
                .whereEqualTo("toUserId", uid)
                .whereEqualTo("status", "pending")
                .addSnapshotListener { snapshot, _ ->
                    pendingGroupInvites = snapshot?.documents?.map { it.toUserGroupInvite() } ?: emptyList()
                }
        }
    }

    fun acceptGroupInvite(invite: UserGroupInvite) {
        if (currentUserId.isBlank() || invite.id.isBlank()) return
        val inviteRef = db.collection("userGroupInvites").document(invite.id)
        if (invite.groupId.isBlank()) {
            inviteRef.update("status", "expired")
            return
        }
        val groupRef = db.collection("userGroups").document(invite.groupId)
        groupRef.get().addOnSuccessListener { groupDoc ->
            if (!groupDoc.exists()) {
                inviteRef.update("status", "expired")
                scope.launch { snackbarHostState.showSnackbar("That group no longer exists.") }
                return@addOnSuccessListener
            }
            val batch = db.batch()
            batch.update(inviteRef, "status", "accepted")
            batch.update(
                groupRef,
                mapOf(
                    "memberIds" to FieldValue.arrayUnion(currentUserId),
                    "invitedUserIds" to FieldValue.arrayRemove(currentUserId),
                ),
            )
            batch.commit()
        }
    }

    fun declineGroupInvite(invite: UserGroupInvite) {
        if (currentUserId.isBlank() || invite.id.isBlank()) return
        val inviteRef = db.collection("userGroupInvites").document(invite.id)
        if (invite.groupId.isBlank()) {
            inviteRef.update("status", "declined")
            return
        }
        val groupRef = db.collection("userGroups").document(invite.groupId)
        groupRef.get().addOnSuccessListener { groupDoc ->
            if (!groupDoc.exists()) {
                inviteRef.update("status", "declined")
                return@addOnSuccessListener
            }
            val batch = db.batch()
            batch.update(inviteRef, "status", "declined")
            batch.update(groupRef, "invitedUserIds", FieldValue.arrayRemove(currentUserId))
            batch.commit()
        }
    }

    fun createGroup() {
        val uid = currentUserId
        if (uid.isBlank() || groupName.isBlank()) return
        db
            .collection("userGroups")
            .add(
                mapOf(
                    "name" to groupName.trim(),
                    "description" to groupDescription.trim(),
                    "ownerUserId" to uid,
                    "memberIds" to listOf(uid),
                    "adminIds" to listOf(uid),
                    "visibility" to groupVisibility,
                    "createdAt" to System.currentTimeMillis(),
                ),
            ).addOnSuccessListener {
                groupName = ""
                groupDescription = ""
                groupVisibility = "public"
                showCreateGroupDialog = false
            }
    }

    fun runStudentSearch() {
        val query = searchQuery.trim().lowercase()
        val hasCriteria =
            query.isNotBlank() || selectedMajor.isNotBlank() ||
                selectedClub.isNotBlank() || selectedClass.isNotBlank()
        if (!hasCriteria) {
            searchResults = emptyList()
            return
        }
        isSearching = true
        db
            .collection("users")
            .get()
            .addOnSuccessListener { result ->
                searchResults =
                    result.documents
                        .filter { doc ->
                            if (doc.id == currentUserId) return@filter false
                            val name = doc.getString("name").orEmpty()
                            val email = doc.getString("email").orEmpty()
                            val major = doc.getString("major").orEmpty()
                            val clubs = (doc.get("clubs") as? List<*>)?.map { it.toString() } ?: emptyList()
                            val classYear = doc.getString("classYear").orEmpty()
                            val classes = (doc.get("classes") as? List<*>)?.map { it.toString() } ?: emptyList()
                            val classValues = (classes + classYear).filter { it.isNotBlank() }
                            val textMatch =
                                query.isBlank() || name.lowercase().contains(query) ||
                                    email.lowercase().contains(query) || major.lowercase().contains(query) ||
                                    clubs.any { it.lowercase().contains(query) } ||
                                    classValues.any { it.lowercase().contains(query) }
                            val majorMatch = selectedMajor.isBlank() || major == selectedMajor
                            val clubMatch = selectedClub.isBlank() || clubs.any { it == selectedClub }
                            val classMatch = selectedClass.isBlank() || classValues.any { it == selectedClass }
                            textMatch && majorMatch && clubMatch && classMatch
                        }.map { it.toUserProfile() }
                        .filter { profile -> profile.id !in hiddenUserIds }
                        .filter { profile -> canViewProfile(profile, currentUserId, friendIds) }
                isSearching = false
            }.addOnFailureListener { isSearching = false }
    }

    if (showCreateGroupDialog) {
        AlertDialog(
            onDismissRequest = { showCreateGroupDialog = false },
            title = { Text("Create Group") },
            text = {
                Column {
                    OutlinedTextField(
                        value = groupName,
                        onValueChange = { groupName = it },
                        label = { Text("Group name") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = groupDescription,
                        onValueChange = { groupDescription = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    VisibilitySelector(
                        title = "Group Visibility",
                        selectedValue = groupVisibility,
                        options = GroupVisibilityOptions,
                        onValueChange = { groupVisibility = it },
                    )
                }
            },
            confirmButton = {
                Button(onClick = { createGroup() }, enabled = groupName.isNotBlank()) {
                    Text("Create")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showCreateGroupDialog = false }) { Text("Cancel") }
            },
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
        ) {
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
                    text = { Text("Groups (${userGroups.size})") },
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = { Text("Find Students") },
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (selectedTab) {
                0 -> {
                    val visibleFriendProfiles =
                        friendProfiles.filter { friend ->
                            friend.id != currentUserId && friend.id !in hiddenUserIds
                        }

                    if (visibleFriendProfiles.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                "No friends yet!\nSearch for students to add.",
                                color = MaterialTheme.colorScheme.secondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            )
                        }
                    } else {
                        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                            visibleFriendProfiles.forEach { user ->
                                StudentCard(
                                    user = user,
                                    isFriend = true,
                                    isPending = false,
                                    onViewProfile = {
                                        onOpenUserProfile(user.id, user.name.ifBlank { user.email })
                                    },
                                    onAddFriend = {},
                                    onRemoveFriend = {
                                        val uid = currentUserId
                                        // Remove from both users' friend lists
                                        db
                                            .collection("users")
                                            .document(uid)
                                            .update("friends", FieldValue.arrayRemove(user.id))
                                        db
                                            .collection("users")
                                            .document(user.id)
                                            .update("friends", FieldValue.arrayRemove(uid))
                                        // Delete DM conversation and all messages
                                        val conversationId = listOf(uid, user.id).sorted().joinToString("_")
                                        val convRef = db.collection("conversations").document(conversationId)
                                        convRef.collection("messages").get().addOnSuccessListener { snapshot ->
                                            val batch = db.batch()
                                            snapshot.documents.forEach { doc -> batch.delete(doc.reference) }
                                            batch.commit().addOnSuccessListener { convRef.delete() }
                                        }
                                    },
                                    onMessage = {
                                        val friendName = user.name.ifBlank { user.email.ifBlank { "Unknown" } }
                                        messagesViewModel.getOrCreateDMConversation(
                                            otherUserId = user.id,
                                            otherUserName = friendName,
                                        ) { convId -> onOpenConversation(convId, friendName) }
                                    },
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }

                1 -> {
                    if (incomingRequests.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No pending requests", color = MaterialTheme.colorScheme.secondary)
                        }
                    } else {
                        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                            incomingRequests.forEach { request ->
                                val senderFirstName =
                                    incomingRequestSenderNames[request.fromUserId]
                                        ?: firstNameFromCandidates(request.fromUserName)

                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
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
                                            Text(senderFirstName, style = MaterialTheme.typography.titleMedium)
                                            Text(
                                                "Wants to be your friend",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.secondary,
                                            )
                                        }
                                        Button(onClick = {
                                            val uid = currentUserId
                                            val batch = db.batch()
                                            batch.update(
                                                db.collection("friendRequests").document(request.id),
                                                "status",
                                                "accepted",
                                            )
                                            batch.update(
                                                db.collection("users").document(uid),
                                                "friends",
                                                FieldValue.arrayUnion(request.fromUserId),
                                            )
                                            batch.update(
                                                db.collection("users").document(request.fromUserId),
                                                "friends",
                                                FieldValue.arrayUnion(uid),
                                            )
                                            batch.commit()
                                        }) { Text("Accept") }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        OutlinedButton(onClick = {
                                            db
                                                .collection("friendRequests")
                                                .document(request.id)
                                                .update("status", "denied")
                                        }) { Text("Deny") }
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        pendingGroupInvites.forEach { invite ->
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        invite.userGroupName.ifBlank { "Group invite" },
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                    Text(
                                        "Invited by ${invite.fromUserName.ifBlank { invite.fromUserId.ifBlank { "Unknown user" } }}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.secondary,
                                    )
                                    Row {
                                        Button(onClick = { acceptGroupInvite(invite) }) { Text("Accept") }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        OutlinedButton(onClick = { declineGroupInvite(invite) }) { Text("Decline") }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        Button(onClick = { showCreateGroupDialog = true }, modifier = Modifier.fillMaxWidth()) {
                            Text("Create Group")
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        if (userGroups.isEmpty()) {
                            Text("No groups yet.")
                        } else {
                            userGroups.forEach { group ->
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(group.name, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            group.description.ifBlank { "No description yet." },
                                            color = MaterialTheme.colorScheme.secondary,
                                        )
                                        Text("${group.memberIds.size} members")
                                        Spacer(modifier = Modifier.height(8.dp))
                                        OutlinedButton(
                                            onClick = { onOpenUserGroupProfile(group.id) },
                                            modifier = Modifier.fillMaxWidth(),
                                        ) { Text("Open Group") }
                                    }
                                }
                            }
                        }
                    }
                }

                3 -> {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp),
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            label = { Text("Search by name or email") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            trailingIcon = {
                                IconButton(onClick = { runStudentSearch() }) {
                                    Icon(Icons.Default.Search, contentDescription = "Search")
                                }
                            },
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = majorQuery,
                                onValueChange = {
                                    majorQuery = it
                                    selectedMajor = ""
                                },
                                label = { Text("Major") },
                                placeholder = { Text("Any major") },
                                isError = majorQuery.isNotBlank() && selectedMajor.isEmpty() && filteredMajors.isEmpty(),
                                supportingText = {
                                    if (majorQuery.isNotBlank() && selectedMajor.isEmpty() && filteredMajors.isEmpty()) {
                                        Text("No matching major found")
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                            )
                            DropdownMenu(
                                expanded = majorQuery.isNotBlank() && selectedMajor.isEmpty() && filteredMajors.isNotEmpty(),
                                onDismissRequest = {},
                                properties = PopupProperties(focusable = false),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                filteredMajors.forEach { major ->
                                    DropdownMenuItem(
                                        text = { Text(major) },
                                        onClick = {
                                            selectedMajor = major
                                            majorQuery = major
                                        },
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = clubQuery,
                                onValueChange = {
                                    clubQuery = it
                                    selectedClub = ""
                                },
                                label = { Text("Club") },
                                placeholder = { Text("Any club") },
                                isError = clubQuery.isNotBlank() && selectedClub.isEmpty() && filteredClubs.isEmpty(),
                                supportingText = {
                                    if (clubQuery.isNotBlank() && selectedClub.isEmpty() && filteredClubs.isEmpty()) {
                                        Text("No matching club found")
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                            )
                            DropdownMenu(
                                expanded = clubQuery.isNotBlank() && selectedClub.isEmpty() && filteredClubs.isNotEmpty(),
                                onDismissRequest = {},
                                properties = PopupProperties(focusable = false),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                filteredClubs.forEach { club ->
                                    DropdownMenuItem(
                                        text = { Text(club) },
                                        onClick = {
                                            selectedClub = club
                                            clubQuery = club
                                        },
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        ExposedDropdownMenuBox(
                            expanded = classExpanded,
                            onExpandedChange = { classExpanded = !classExpanded },
                        ) {
                            OutlinedTextField(
                                value = selectedClass,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Class Year") },
                                placeholder = { Text("Any year") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = classExpanded) },
                                modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                                singleLine = true,
                            )
                            ExposedDropdownMenu(
                                expanded = classExpanded,
                                onDismissRequest = { classExpanded = false },
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Any year") },
                                    onClick = {
                                        selectedClass = ""
                                        classExpanded = false
                                    },
                                )
                                CsuciClassYears.forEach { classYear ->
                                    DropdownMenuItem(
                                        text = { Text(classYear) },
                                        onClick = {
                                            selectedClass = classYear
                                            classExpanded = false
                                        },
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(onClick = { runStudentSearch() }, modifier = Modifier.fillMaxWidth()) {
                            Text("Search")
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        val hasCriteria =
                            searchQuery.isNotBlank() || selectedMajor.isNotBlank() ||
                                selectedClub.isNotBlank() || selectedClass.isNotBlank()
                        val visibleSearchResults =
                            searchResults.filter { user -> user.id !in hiddenUserIds }

                        if (isSearching) {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                        } else if (visibleSearchResults.isEmpty() && hasCriteria) {
                            Text("No students found", color = MaterialTheme.colorScheme.secondary)
                        } else {
                            visibleSearchResults.forEach { user ->
                                val friendName = user.name.ifBlank { user.email.ifBlank { "Unknown" } }
                                StudentCard(
                                    user = user,
                                    isFriend = friendIds.contains(user.id),
                                    isPending = pendingRequestIds.contains(user.id),
                                    onViewProfile = {
                                        onOpenUserProfile(user.id, user.name.ifBlank { user.email })
                                    },
                                    onAddFriend = {
                                        val uid = currentUserId
                                        val request =
                                            hashMapOf(
                                                "fromUserId" to uid,
                                                "fromUserName" to
                                                    firstNameFromCandidates(
                                                        currentUserName,
                                                        auth.currentUser?.displayName,
                                                    ),
                                                "toUserId" to user.id,
                                                "status" to "pending",
                                                "timestamp" to System.currentTimeMillis(),
                                            )
                                        db.collection("friendRequests").add(request)
                                    },
                                    onRemoveFriend = {
                                        val uid = currentUserId
                                        db
                                            .collection("users")
                                            .document(uid)
                                            .update("friends", FieldValue.arrayRemove(user.id))
                                        db
                                            .collection("users")
                                            .document(user.id)
                                            .update("friends", FieldValue.arrayRemove(uid))
                                        val conversationId = listOf(uid, user.id).sorted().joinToString("_")
                                        val convRef = db.collection("conversations").document(conversationId)
                                        convRef.collection("messages").get().addOnSuccessListener { snapshot ->
                                            val batch = db.batch()
                                            snapshot.documents.forEach { doc -> batch.delete(doc.reference) }
                                            batch.commit().addOnSuccessListener { convRef.delete() }
                                        }
                                    },
                                    onMessage = {
                                        messagesViewModel.getOrCreateDMConversation(
                                            otherUserId = user.id,
                                            otherUserName = friendName,
                                        ) { convId -> onOpenConversation(convId, friendName) }
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
