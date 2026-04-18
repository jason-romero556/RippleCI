package com.example.rippleci.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
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
import com.example.rippleci.ui.components.ClubLinkRow
import com.example.rippleci.ui.components.UserLinkRow
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

@Composable
fun UserProfileScreen(
    userId: String,
    onBack: () -> Unit,
    onOpenUserProfile: (String) -> Unit,
    onOpenClubProfile: (String) -> Unit,
    onOpenEventProfile: (String) -> Unit,
) {
    val db = Firebase.firestore

    var userName by remember { mutableStateOf("") }
    var friendIds by remember { mutableStateOf<List<String>>(emptyList()) }
    var clubIds by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(userId) {
        db
            .collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { doc ->
                userName = doc.getString("name").orEmpty()
                friendIds = (doc.get("friends") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                clubIds = (doc.get("clubs") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
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
            text = if (userName.isNotBlank()) userName else "Unknown User",
            style = MaterialTheme.typography.headlineMedium,
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text("Friends", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        friendIds.forEach { friendId ->
            UserLinkRow(
                label = friendId,
                onClick = { onOpenUserProfile(friendId) },
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Clubs", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        clubIds.forEach { clubId ->
            ClubLinkRow(
                label = clubId,
                onClick = { onOpenClubProfile(clubId) },
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
