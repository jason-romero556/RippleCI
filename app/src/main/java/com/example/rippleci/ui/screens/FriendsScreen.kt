package com.example.rippleci.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
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
    onOpenUserProfile: (String) -> Unit = {},
    onOpenUserGroupProfile: (String) -> Unit = {},
    messagesViewModel: MessagesViewModel,
) {
    val db = Firebase.firestore
    val auth = Firebase.auth
    val currentUserId = auth.currentUser?.uid ?: ""
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val csuciMajors = listOf(
        "Anthropology",
        "Applied Physics",
        "Art – Art History",
        "Art – Art Studio",
        "Biology (B.A.)",
        "Biology (B.S.)",
        "Biotechnology & Bioinformatics",
        "Business Administration",
        "Chemistry",
        "Communication",
        "Computer Science",
        "Early Childhood Studies",
        "Economics",
        "English",
        "Environmental Science & Resource Management",
        "Health Science",
        "History",
        "Kinesiology",
        "Liberal Studies",
        "Mathematics",
        "Music",
        "Nursing",
        "Political Science",
        "Psychology",
        "Social Work",
        "Sociology",
        "Spanish",
        "Theatre"
    )

    val csuciClubs = listOf(
        "Active Minds Chapter",
        "Alpha Delta Psi",
        "American Marketing Association",
        "American Medical Student Association CI",
        "American Society for Microbiology",
        "Anthropology Club",
        "Beta Gamma Nu Fraternity",
        "Bicycle Kitchen",
        "Black Student Union (BSU)",
        "Channel Islands Audubon",
        "Channel Islands Endurance Club",
        "Channel Islands Ice Hockey",
        "Channel Islands Sociology Club",
        "CI Bee Club",
        "CI Biology Club",
        "CI Business Club",
        "CI Car Club",
        "CI Cheer Club",
        "CI Dance Club",
        "CI Finance Club",
        "CI Line Dance Club",
        "CI Math Club",
        "CI Neuroscience Society",
        "CI Pre-Dental Society",
        "CI Women in Tech",
        "Circle K International",
        "Conservation Robotics & Engineering Club",
        "CSUCI Surf Club",
        "CSUCI Surfrider Foundation",
        "Delta Alpha Pi Honor Society",
        "El Club de Español",
        "English Club",
        "Everyone is Our Priority Club",
        "Free Radicals Chemistry Club",
        "Gamma Beta Phi National Honor Society",
        "Green Generation Club",
        "Health & Wellness Club",
        "Hillel",
        "I.D.E.A.S.",
        "International Relations",
        "Intervarsity Christian Fellowship",
        "Kappa Rho Delta Sorority",
        "Kilusan Pilipino",
        "LULAC",
        "M.E.Ch.A. de CI",
        "National Society of Collegiate Scholars",
        "Networks and Security (NETSEC)",
        "Physician Assistant Student Club",
        "Pre-Law Society",
        "Pre-Nursing Club",
        "Psi Chi Honor Society in Psychology",
        "Psychology Club",
        "Queer Student Alliance",
        "Red Cross Club",
        "SACNAS",
        "Sailing Club",
        "Scuba STEM Fellowship",
        "Sigma Omega Nu Sorority",
        "Men's Soccer Club",
        "Women's Soccer Club",
        "Student Historian Association",
        "Student Nurses' Association",
        "Students for Quality Education",
        "Tabletop Games Club",
        "Tomorrows Teachers",
        "Unión de Hermanos",
        "Volleyball Club",
        "Xi Sigma Sorority",
        "Zeta Pi Omega Sorority"
    )

    val csuciClasses = listOf("Freshman", "Sophomore", "Junior", "Senior", "Graduate")

    var incomingRequests by remember { mutableStateOf<List<FriendRequest>>(emptyList()) }

    // Find Students filter state
    var selectedMajor by remember { mutableStateOf("") }
    var majorQuery by remember { mutableStateOf("") }

    var selectedClub by remember { mutableStateOf("") }
    var clubQuery by remember { mutableStateOf("") }

    var selectedClass by remember { mutableStateOf("") }
    var classExpanded by remember { mutableStateOf(false) }

    val filteredMajors = remember(majorQuery) {
        if (majorQuery.isBlank()) emptyList()
        else csuciMajors.filter { it.contains(majorQuery, ignoreCase = true) }
    }

    val filteredClubs = remember(clubQuery) {
        if (clubQuery.isBlank()) emptyList()
        else csuciClubs.filter { it.contains(clubQuery, ignoreCase = true) }
    }

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

    LaunchedEffect(requestedSelectedTab) {
        requestedSelectedTab?.let { tabIndex ->
            selectedTab = tabIndex
            onSelectedTabRequestHandled()
        }
    }

    val filteredMajors = remember(majorQuery) {
        if (majorQuery.isBlank()) emptyList()
        else CsuciMajors.filter { it.contains(majorQuery, ignoreCase = true) }
    }

    val filteredClubs = remember(clubQuery) {
        if (clubQuery.isBlank()) emptyList()
        else CsuciClubs.filter { it.contains(clubQuery, ignoreCase = true) }
    }

    LaunchedEffect(currentUserId) {
        currentUserId.let { uid ->
            db.collection("users").document(uid).addSnapshotListener { doc, _ ->
                val ids = (doc?.get("friends") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                friendIds = ids
                if (ids.isNotEmpty()) {
                    db.collection("users").whereIn("__name__", ids).get()
                        .addOnSuccessListener { result ->
                            friendProfiles = result.documents.map { it.toUserProfile() }
                        }
                } else {
                    friendProfiles = emptyList()
                }
            }

            db.collection("friendRequests")
                .whereEqualTo("fromUserId", uid)
                .whereEqualTo("status", "pending")
                .addSnapshotListener { snapshot, _ ->
                    pendingRequestIds = snapshot?.documents?.mapNotNull { it.getString("toUserId") } ?: emptyList()
                }

            db.collection("friendRequests")
                .whereEqualTo("toUserId", uid)
                .whereEqualTo("status", "pending")
                .addSnapshotListener { snapshot, _ ->
                    incomingRequests = snapshot?.documents?.map { doc -> doc.toFriendRequest() } ?: emptyList()
                }

            db.collection("userGroups")
                .whereArrayContains("memberIds", uid)
                .addSnapshotListener { snapshot, _ ->
                    userGroups = snapshot?.documents?.map { it.toUserGroupProfile() } ?: emptyList()
                }

            db.collection("userGroupInvites")
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
        if (invite.groupId.isBlank()) { inviteRef.update("status", "expired"); return }
        val groupRef = db.collection("userGroups").document(invite.groupId)
        groupRef.get().addOnSuccessListener { groupDoc ->
            if (!groupDoc.exists()) {
                inviteRef.update("status", "expired")
                scope.launch { snackbarHostState.showSnackbar("That group no longer exists.") }
                return@addOnSuccessListener
            }
            val batch = db.batch()
            batch.update(inviteRef, "status", "accepted")
            batch.update(groupRef, mapOf(
                "memberIds" to FieldValue.arrayUnion(currentUserId),
                "invitedUserIds" to FieldValue.arrayRemove(currentUserId),
            ))
            batch.commit()
        }
    }

    fun declineGroupInvite(invite: UserGroupInvite) {
        if (currentUserId.isBlank() || invite.id.isBlank()) return
        val inviteRef = db.collection("userGroupInvites").document(invite.id)
        if (invite.groupId.isBlank()) { inviteRef.update("status", "declined"); return }
        val groupRef = db.collection("userGroups").document(invite.groupId)
        groupRef.get().addOnSuccessListener { groupDoc ->
            if (!groupDoc.exists()) { inviteRef.update("status", "declined"); return@addOnSuccessListener }
            val batch = db.batch()
            batch.update(inviteRef, "status", "declined")
            batch.update(groupRef, "invitedUserIds", FieldValue.arrayRemove(currentUserId))
            batch.commit()
        }
    }

    fun createGroup() {
        val uid = currentUserId
        if (uid.isBlank() || groupName.isBlank()) return
        val groupData = mapOf(
            "name" to groupName.trim(),
            "description" to groupDescription.trim(),
            "ownerUserId" to uid,
            "memberIds" to listOf(uid),
            "adminIds" to listOf(uid),
            "visibility" to groupVisibility,
            "createdAt" to System.currentTimeMillis(),
        )
        db.collection("userGroups").add(groupData).addOnSuccessListener {
            groupName = ""
            groupDescription = ""
            groupVisibility = "public"
            showCreateGroupDialog = false
        }
    }

    fun runStudentSearch() {
        val query = searchQuery.trim().lowercase()
        val hasCriteria = query.isNotBlank() || selectedMajor.isNotBlank() ||
                selectedClub.isNotBlank() || selectedClass.isNotBlank()
        if (!hasCriteria) { searchResults = emptyList(); return }
        isSearching = true
        db.collection("users").get()
            .addOnSuccessListener { result ->
                searchResults = result.documents
                    .filter { doc ->
                        if (doc.id == currentUserId) return@filter false
                        val name = doc.getString("name").orEmpty()
                        val email = doc.getString("email").orEmpty()
                        val major = doc.getString("major").orEmpty()
                        val clubs = (doc.get("clubs") as? List<*>)?.map { it.toString() } ?: emptyList()
                        val classYear = doc.getString("classYear").orEmpty()
                        val classes = (doc.get("classes") as? List<*>)?.map { it.toString() } ?: emptyList()
                        val classValues = (classes + classYear).filter { it.isNotBlank() }
                        val textMatch = query.isBlank() || name.lowercase().contains(query) ||
                                email.lowercase().contains(query) || major.lowercase().contains(query) ||
                                clubs.any { it.lowercase().contains(query) } ||
                                classValues.any { it.lowercase().contains(query) }
                        val majorMatch = selectedMajor.isBlank() || major == selectedMajor
                        val clubMatch = selectedClub.isBlank() || clubs.any { it == selectedClub }
                        val classMatch = selectedClass.isBlank() || classValues.any { it == selectedClass }
                        textMatch && majorMatch && clubMatch && classMatch
                    }.map { it.toUserProfile() }
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
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("My Friends (${friendIds.size})") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Requests (${incomingRequests.size})") })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Groups (${userGroups.size})") })
                Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }, text = { Text("Find Students") })
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (selectedTab) {
                0 -> {
                    if (friendProfiles.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                "No friends yet!\nSearch for students to add.",
                                color = MaterialTheme.colorScheme.secondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            )
                        }
                    } else {
                        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                            friendProfiles.filter { user -> user.id != currentUserId }.forEach { user ->
                                StudentCard(
                                    user = user,
                                    isFriend = true,
                                    isPending = false,
                                    onViewProfile = { onOpenUserProfile(user.id) },
                                    onAddFriend = {},
                                    onRemoveFriend = {
                                        val uid = currentUserId
                                        db.collection("users").document(uid).update("friends", FieldValue.arrayRemove(user.id))
                                        db.collection("users").document(user.id).update("friends", FieldValue.arrayRemove(uid))
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
                                            Text(text = request.fromUserName, style = MaterialTheme.typography.titleMedium)
                                            Text(
                                                text = "Wants to be your friend",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.secondary,
                                            )
                                        }
                                        Button(onClick = {
                                            val uid = currentUserId
                                            val batch = db.batch()
                                            val requestRef = db.collection("friendRequests").document(request.id)
                                            batch.update(requestRef, "status", "accepted")
                                            val currentUserRef = db.collection("users").document(uid)
                                            val otherUserRef = db.collection("users").document(request.fromUserId)
                                            batch.update(currentUserRef, "friends", FieldValue.arrayUnion(request.fromUserId))
                                            batch.update(otherUserRef, "friends", FieldValue.arrayUnion(uid))
                                            batch.commit()
                                        }) { Text("Accept") }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        OutlinedButton(onClick = {
                                            db.collection("friendRequests").document(request.id).update("status", "denied")
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
                                        Text(group.description.ifBlank { "No description yet." }, color = MaterialTheme.colorScheme.secondary)
                                        Text("${group.memberIds.size} members")
                                        Spacer(modifier = Modifier.height(8.dp))
                                        OutlinedButton(onClick = { onOpenUserGroupProfile(group.id) }, modifier = Modifier.fillMaxWidth()) {
                                            Text("Open Group")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                3 -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp),
                    ) {
                        // ── Major autocomplete ───────────────────────────────
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = majorQuery,
                                onValueChange = { majorQuery = it; selectedMajor = "" },
                                label = { Text("Major") },
                                placeholder = { Text("Type to search...") },
                                isError = majorQuery.isNotBlank() && selectedMajor.isEmpty() && filteredMajors.isEmpty(),
                                supportingText = {
                                    if (majorQuery.isNotBlank() && selectedMajor.isEmpty() && filteredMajors.isEmpty())
                                        Text("No matching major found")
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
                                        onClick = { selectedMajor = major; majorQuery = major },
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // ── Club autocomplete ────────────────────────────────
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = clubQuery,
                                onValueChange = { clubQuery = it; selectedClub = "" },
                                label = { Text("Club") },
                                placeholder = { Text("Type to search...") },
                                isError = clubQuery.isNotBlank() && selectedClub.isEmpty() && filteredClubs.isEmpty(),
                                supportingText = {
                                    if (clubQuery.isNotBlank() && selectedClub.isEmpty() && filteredClubs.isEmpty())
                                        Text("No matching club found")
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
                                        onClick = { selectedClub = club; clubQuery = club },
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // ── Class year dropdown ──────────────────────────────
                        ExposedDropdownMenuBox(
                            expanded = classExpanded,
                            onExpandedChange = { classExpanded = !classExpanded },
                        ) {
                            OutlinedTextField(
                                value = selectedClass,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Class Year") },
                                placeholder = { Text("Any Year") },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = classExpanded)
                                },
                                modifier = Modifier.fillMaxWidth().menuAnchor(),
                                singleLine = true,
                            )
                            ExposedDropdownMenu(
                                expanded = classExpanded,
                                onDismissRequest = { classExpanded = false },
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Any Year") },
                                    onClick = { selectedClass = ""; classExpanded = false },
                                )
                                csuciClasses.forEach { cls ->
                                    DropdownMenuItem(
                                        text = { Text(cls) },
                                        onClick = { selectedClass = cls; classExpanded = false },
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // ── Search button ────────────────────────────────────
                        Button(
                            onClick = {
                                val anyFilterSet = selectedMajor.isNotEmpty() ||
                                        selectedClub.isNotEmpty() ||
                                        selectedClass.isNotEmpty()
                                if (!anyFilterSet) return@Button

                                isSearching = true
                                db.collection("users").get()
                                    .addOnSuccessListener { result ->
                                        searchResults = result.documents
                                            .filter { doc ->
                                                if (doc.id == currentUserId) return@filter false
                                                val majorMatch = selectedMajor.isEmpty() ||
                                                        (doc.getString("major") ?: "") == selectedMajor
                                                val clubMatch = selectedClub.isEmpty() ||
                                                        (doc.get("clubs") as? List<*>)
                                                            ?.any { it.toString() == selectedClub } == true
                                                val classMatch = selectedClass.isEmpty() ||
                                                        (doc.getString("classYear") ?: "") == selectedClass
                                                majorMatch && clubMatch && classMatch
                                            }
                                            .map { it.toUserProfile() }
                                        isSearching = false
                                    }
                                    .addOnFailureListener { isSearching = false }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Search")
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // ── Results ──────────────────────────────────────────
                        if (isSearching) {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                        } else if (searchResults.isEmpty() &&
                            (selectedMajor.isNotEmpty() || selectedClub.isNotEmpty() || selectedClass.isNotEmpty())
                        ) {
                            Text("No students found", color = MaterialTheme.colorScheme.secondary)
                        } else {
                            searchResults.forEach { user ->
                                val friendName = user.name.ifBlank { user.email.ifBlank { "Unknown" } }
                                StudentCard(
                                    user = user,
                                    isFriend = friendIds.contains(user.id),
                                    isPending = pendingRequestIds.contains(user.id),
                                    onViewProfile = { onOpenUserProfile(user.id) },
                                    onAddFriend = {
                                        val uid = currentUserId
                                        val request = hashMapOf(
                                            "fromUserId" to uid,
                                            "fromUserName" to (auth.currentUser?.displayName?.takeIf { it.isNotBlank() }
                                                ?: user.name.takeIf { it.isNotBlank() }
                                                ?: auth.currentUser?.email
                                                ?: "Someone"),
                                            "toUserId" to user.id,
                                            "status" to "pending",
                                            "timestamp" to System.currentTimeMillis(),
                                        )
                                        db.collection("friendRequests").add(request)
                                    },
                                    onRemoveFriend = {
                                        val uid = currentUserId
                                        db.collection("users").document(uid).update("friends", FieldValue.arrayRemove(user.id))
                                        db.collection("users").document(user.id).update("friends", FieldValue.arrayRemove(uid))
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
