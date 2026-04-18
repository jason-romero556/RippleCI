package com.example.rippleci.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.rippleci.data.models.ClubProfile
import com.example.rippleci.ui.components.UserLinkRow
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

@Composable
fun ClubProfileScreen(
    clubId: String,
    isMember: Boolean,
    onBack: () -> Unit,
    onJoinClub: () -> Unit,
    onLeaveClub: () -> Unit,
    onViewEvents: () -> Unit,
    onOpenUserProfile: (String) -> Unit,
    onOpenEventProfile: (String) -> Unit,
    onOpenClubProfile: (String) -> Unit,
) {
    val db = Firebase.firestore

    var clubName by remember { mutableStateOf("") }
    var memberIds by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(clubId) {
        db
            .collection("clubs")
            .document(clubId)
            .get()
            .addOnSuccessListener { doc ->
                clubName = doc.getString("name").orEmpty()
                memberIds = (doc.get("memberIds") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
            }
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp),
    ) {
        OutlinedButton(onClick = onBack) {
            Text("Back")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (clubName.isNotBlank()) clubName else "Unknown Club",
            style = MaterialTheme.typography.headlineMedium,
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (isMember) {
            OutlinedButton(onClick = onLeaveClub) {
                Text("Leave Club")
            }
        } else {
            Button(onClick = onJoinClub) {
                Text("Join Club")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Members", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        memberIds.forEach { memberId ->
            UserLinkRow(
                label = memberId,
                onClick = { onOpenUserProfile(memberId) },
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
