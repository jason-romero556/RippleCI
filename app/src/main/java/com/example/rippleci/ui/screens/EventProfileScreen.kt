package com.example.rippleci.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.rippleci.data.models.UserProfile
import com.example.rippleci.data.toUserProfile
import com.example.rippleci.ui.components.ClubLinkRow
import com.example.rippleci.ui.components.ProfileInfoRow
import com.example.rippleci.ui.components.UserLinkRow
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.firestore

@Composable
fun EventProfileScreen(
    eventId: String,
    ownerUserId: String,
    onBack: () -> Unit,
    onOpenUserProfile: (String) -> Unit,
    onOpenClubProfile: (String) -> Unit,
    onOpenEventProfile: (String) -> Unit,
) {
    val db = Firebase.firestore
    val currentUserId =
        Firebase.auth.currentUser
            ?.uid
            .orEmpty()
    val eventOwnerUserId = ownerUserId.ifBlank { currentUserId }

    var ownerUserId by remember { mutableStateOf("") }
    var attendeeIds by remember { mutableStateOf(emptyList<String>()) }
    var invitedUserIds by remember { mutableStateOf(emptyList<String>()) }
    var friendProfiles by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var showInviteDialog by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var startTime by remember { mutableStateOf("") }
    var endTime by remember { mutableStateOf("") }
    var clubId by remember { mutableStateOf("") }
    var creatorProfile by remember { mutableStateOf<UserProfile?>(null) }

    LaunchedEffect(eventId, currentUserId, eventOwnerUserId) {
        if (currentUserId.isBlank()) return@LaunchedEffect

        db
            .collection("users")
            .document(eventOwnerUserId)
            .collection("personalEvents")
            .document(eventId)
            .get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) return@addOnSuccessListener

                title = doc.getString("title").orEmpty()
                description = doc.getString("description").orEmpty()
                location = doc.getString("location").orEmpty()
                date = doc.getString("date").orEmpty()
                startTime = doc.getString("startTime").orEmpty()
                endTime = doc.getString("endTime").orEmpty()
                clubId = doc.getString("clubId").orEmpty()

                ownerUserId = doc.getString("ownerUserId") ?: eventOwnerUserId

                attendeeIds =
                    (doc.get("attendeeIds") as? List<*>)
                        ?.mapNotNull { it as? String }
                        ?: emptyList()

                invitedUserIds =
                    (doc.get("invitedUserIds") as? List<*>)
                        ?.mapNotNull { it as? String }
                        ?: emptyList()

                db
                    .collection("users")
                    .document(ownerUserId)
                    .get()
                    .addOnSuccessListener { userDoc ->
                        creatorProfile = userDoc.toUserProfile()
                    }
            }
    }

    LaunchedEffect(currentUserId) {
        if (currentUserId.isBlank()) return@LaunchedEffect

        db
            .collection("users")
            .document(currentUserId)
            .get()
            .addOnSuccessListener { userDoc ->
                val friendIds =
                    (userDoc.get("friends") as? List<*>)
                        ?.mapNotNull { it as? String }
                        ?: emptyList()

                if (friendIds.isEmpty()) {
                    friendProfiles = emptyList()
                } else {
                    db
                        .collection("users")
                        .whereIn(FieldPath.documentId(), friendIds.take(10))
                        .get()
                        .addOnSuccessListener { result ->
                            friendProfiles = result.documents.map { it.toUserProfile() }
                        }
                }
            }
    }

    fun inviteUser(user: UserProfile) {
        val inviteId = "${ownerUserId}_${eventId}_${user.id}"

        val eventRef =
            db
                .collection("users")
                .document(ownerUserId)
                .collection("personalEvents")
                .document(eventId)

        val inviteRef =
            db
                .collection("eventInvites")
                .document(inviteId)

        val batch = db.batch()

        batch.set(
            inviteRef,
            mapOf(
                "eventId" to eventId,
                "ownerUserId" to ownerUserId,
                "fromUserId" to currentUserId,
                "toUserId" to user.id,
                "eventTitle" to title,
                "status" to "pending",
                "createdAt" to System.currentTimeMillis(),
            ),
        )

        batch.update(eventRef, "invitedUserIds", FieldValue.arrayUnion(user.id))

        batch.commit().addOnSuccessListener {
            invitedUserIds = invitedUserIds + user.id
        }
    }

    if (showInviteDialog) {
        AlertDialog(
            onDismissRequest = { showInviteDialog = false },
            title = { Text("Invite Friends") },
            text = {
                Column {
                    friendProfiles.forEach { friend ->
                        val alreadyInvited = invitedUserIds.contains(friend.id)

                        OutlinedButton(
                            onClick = { inviteUser(friend) },
                            enabled = !alreadyInvited,
                        ) {
                            Text(if (alreadyInvited) "${friend.name} invited" else friend.name)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showInviteDialog = false }) {
                    Text("Done")
                }
            },
        )
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = title.ifBlank { "Untitled Event" },
            style = MaterialTheme.typography.headlineMedium,
        )

        Spacer(modifier = Modifier.height(16.dp))

        ProfileInfoRow("Date", date.ifBlank { "Not set" })
        ProfileInfoRow(
            "Time",
            listOf(startTime, endTime).filter { it.isNotBlank() }.joinToString(" - ").ifBlank { "Not set" },
        )
        ProfileInfoRow("Location", location.ifBlank { "Not set" })

        Spacer(modifier = Modifier.height(16.dp))

        Text("Description", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = description.ifBlank { "No description yet." },
            style = MaterialTheme.typography.bodyMedium,
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (currentUserId == ownerUserId && ownerUserId.isNotBlank()) {
            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = { showInviteDialog = true }) {
                Text("Invite Friends")
            }
        }

        Text("Created By", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        creatorProfile?.let { creator ->
            UserLinkRow(
                label = creator.name.ifBlank { "Unknown User" },
                onClick = { onOpenUserProfile(creator.id) },
            )
        } ?: Text("Creator not found")

        if (clubId.isNotBlank()) {
            Spacer(modifier = Modifier.height(24.dp))
            Text("Linked Club", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            ClubLinkRow(
                label = clubId,
                onClick = { onOpenClubProfile(clubId) },
            )
        }
    }
}
