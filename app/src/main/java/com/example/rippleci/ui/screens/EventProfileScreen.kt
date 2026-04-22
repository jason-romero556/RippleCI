package com.example.rippleci.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
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
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.firestore

@Composable
fun EventProfileScreen(
    eventId: String,
    onBack: () -> Unit,
    onOpenUserProfile: (String) -> Unit,
    onOpenClubProfile: (String) -> Unit,
    onOpenEventProfile: (String) -> Unit,
) {
    val db = Firebase.firestore

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var startTime by remember { mutableStateOf("") }
    var endTime by remember { mutableStateOf("") }
    var clubId by remember { mutableStateOf("") }
    var creatorProfile by remember { mutableStateOf<UserProfile?>(null) }

    LaunchedEffect(eventId) {
        db
            .collectionGroup("personalEvents")
            .whereEqualTo(FieldPath.documentId(), eventId)
            .get()
            .addOnSuccessListener { result ->
                val doc = result.documents.firstOrNull() ?: return@addOnSuccessListener

                title = doc.getString("title").orEmpty()
                description = doc.getString("description").orEmpty()
                location = doc.getString("location").orEmpty()
                date = doc.getString("date").orEmpty()
                startTime = doc.getString("startTime").orEmpty()
                endTime = doc.getString("endTime").orEmpty()
                clubId = doc.getString("clubId").orEmpty()

                val ownerUserId =
                    doc.reference.parent.parent
                        ?.id
                        .orEmpty()

                if (ownerUserId.isNotBlank()) {
                    db
                        .collection("users")
                        .document(ownerUserId)
                        .get()
                        .addOnSuccessListener { userDoc ->
                            creatorProfile = userDoc.toUserProfile()
                        }
                }
            }
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
    ) {
        OutlinedButton(onClick = onBack) {
            Text("Back")
        }

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
