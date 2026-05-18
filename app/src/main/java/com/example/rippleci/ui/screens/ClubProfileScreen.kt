package com.example.rippleci.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.rippleci.data.models.ClubProfile
import com.example.rippleci.data.models.UserProfile
import com.example.rippleci.data.toUserProfile
import com.example.rippleci.ui.components.ProfileHeader
import com.example.rippleci.ui.components.RippleButton
import com.example.rippleci.ui.components.RippleOutlinedButton
import com.example.rippleci.ui.components.UserActionMenuButton
import com.example.rippleci.ui.components.UserActionMenuItem
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
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
    val currentUserId =
        Firebase.auth.currentUser
            ?.uid
            .orEmpty()

    var clubProfile by remember { mutableStateOf(ClubProfile()) }
    var memberProfiles by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var blockedUserProfiles by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    val currentUserIsMember = clubProfile.memberIds.contains(currentUserId)
    val canManageClub =
        currentUserId.isNotBlank() &&
            (
                clubProfile.ownerUserId == currentUserId ||
                    clubProfile.adminIds.contains(currentUserId) ||
                    clubProfile.officerIds.contains(currentUserId)
            )
    val isBlockedFromClub =
        clubProfile.blockedUserIds.contains(currentUserId) && !canManageClub

    LaunchedEffect(clubId) {
        db
            .collection("clubs")
            .document(clubId)
            .get()
            .addOnSuccessListener { doc ->
                clubProfile = doc.toObject(ClubProfile::class.java)?.copy(id = doc.id) ?: ClubProfile()
            }
    }

    LaunchedEffect(clubProfile.memberIds) {
        memberProfiles = emptyList()

        clubProfile.memberIds.chunked(10).forEach { chunk ->
            db
                .collection("users")
                .whereIn(FieldPath.documentId(), chunk)
                .get()
                .addOnSuccessListener { result ->
                    memberProfiles =
                        (memberProfiles + result.documents.map { it.toUserProfile() })
                            .distinctBy { it.id }
                }
        }
    }

    LaunchedEffect(clubProfile.blockedUserIds) {
        blockedUserProfiles = emptyList()

        clubProfile.blockedUserIds.chunked(10).forEach { chunk ->
            db
                .collection("users")
                .whereIn(FieldPath.documentId(), chunk)
                .get()
                .addOnSuccessListener { result ->
                    blockedUserProfiles =
                        (blockedUserProfiles + result.documents.map { it.toUserProfile() })
                            .distinctBy { it.id }
                }
        }
    }

    fun joinClub() {
        if (currentUserId.isBlank() || isBlockedFromClub) return

        val batch = db.batch()
        batch.update(db.collection("clubs").document(clubId), "memberIds", FieldValue.arrayUnion(currentUserId))
        batch.update(db.collection("users").document(currentUserId), "clubs", FieldValue.arrayUnion(clubId))
        batch.commit().addOnSuccessListener {
            clubProfile = clubProfile.copy(memberIds = (clubProfile.memberIds + currentUserId).distinct())
            onJoinClub()
        }
    }

    fun leaveClub() {
        if (currentUserId.isBlank() || !currentUserIsMember || clubProfile.ownerUserId == currentUserId) return

        val batch = db.batch()
        batch.update(
            db.collection("clubs").document(clubId),
            mapOf(
                "memberIds" to FieldValue.arrayRemove(currentUserId),
                "officerIds" to FieldValue.arrayRemove(currentUserId),
                "adminIds" to FieldValue.arrayRemove(currentUserId),
            ),
        )
        batch.update(db.collection("users").document(currentUserId), "clubs", FieldValue.arrayRemove(clubId))
        batch.commit().addOnSuccessListener {
            clubProfile =
                clubProfile.copy(
                    memberIds = clubProfile.memberIds.filterNot { it == currentUserId },
                    officerIds = clubProfile.officerIds.filterNot { it == currentUserId },
                    adminIds = clubProfile.adminIds.filterNot { it == currentUserId },
                )
            onLeaveClub()
        }
    }

    fun blockFromClub(memberId: String) {
        if (!canManageClub || memberId == clubProfile.ownerUserId || memberId == currentUserId) return

        val batch = db.batch()
        batch.update(
            db.collection("clubs").document(clubId),
            mapOf(
                "blockedUserIds" to FieldValue.arrayUnion(memberId),
                "memberIds" to FieldValue.arrayRemove(memberId),
                "officerIds" to FieldValue.arrayRemove(memberId),
                "adminIds" to FieldValue.arrayRemove(memberId),
            ),
        )
        batch.update(db.collection("users").document(memberId), "clubs", FieldValue.arrayRemove(clubId))
        batch.commit().addOnSuccessListener {
            clubProfile =
                clubProfile.copy(
                    blockedUserIds = (clubProfile.blockedUserIds + memberId).distinct(),
                    memberIds = clubProfile.memberIds.filterNot { it == memberId },
                    officerIds = clubProfile.officerIds.filterNot { it == memberId },
                    adminIds = clubProfile.adminIds.filterNot { it == memberId },
                )
        }
    }

    fun unblockFromClub(blockedUserId: String) {
        if (!canManageClub) return

        db
            .collection("clubs")
            .document(clubId)
            .update("blockedUserIds", FieldValue.arrayRemove(blockedUserId))
            .addOnSuccessListener {
                clubProfile =
                    clubProfile.copy(
                        blockedUserIds = clubProfile.blockedUserIds.filterNot { it == blockedUserId },
                    )
            }
    }

    if (isBlockedFromClub) {
        Text("You cannot access this club.")
        return
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
    ) {
        ProfileHeader(
            title = if (clubProfile.name.isNotBlank()) clubProfile.name else "Unknown Club",
            imageUrl = clubProfile.profilePictureUrl,
            placeholderIcon = Icons.Default.AccountBox,
            subtitle = {
                if (clubProfile.category.isNotBlank()) {
                    Text(clubProfile.category, style = MaterialTheme.typography.bodyMedium)
                }
            },
            actions = {
                if (currentUserIsMember) {
                    RippleOutlinedButton(text = "Leave", onClick = { leaveClub() })
                } else {
                    RippleButton(text = "Join", onClick = { joinClub() })
                }
            }
        )

        if (clubProfile.description.isNotBlank()) {
            Text(clubProfile.description, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(24.dp))
        }

        Text("Members", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        val memberProfileById = memberProfiles.associateBy { it.id }

        clubProfile.memberIds.forEach { memberId ->
            val memberProfile = memberProfileById[memberId]
            val memberName =
                memberProfile
                    ?.name
                    ?.ifBlank { memberProfile.email }
                    ?.ifBlank { memberId }
                    ?: memberId

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RippleOutlinedButton(
                    text = memberName,
                    onClick = { onOpenUserProfile(memberId) },
                    modifier = Modifier.weight(1f),
                )

                if (canManageClub && memberId != currentUserId && memberId != clubProfile.ownerUserId) {
                    Spacer(modifier = Modifier.width(8.dp))

                    UserActionMenuButton(
                        actions =
                            listOf(
                                UserActionMenuItem(
                                    label = "Block from Club",
                                    onClick = { blockFromClub(memberId) },
                                    destructive = true,
                                ),
                            ),
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        if (canManageClub && clubProfile.blockedUserIds.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Blocked from Club", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            val blockedProfileById = blockedUserProfiles.associateBy { it.id }

            clubProfile.blockedUserIds.forEach { blockedUserId ->
                val blockedProfile = blockedProfileById[blockedUserId]
                val blockedName =
                    blockedProfile
                        ?.name
                        ?.ifBlank { blockedProfile.email }
                        ?.ifBlank { blockedUserId }
                        ?: blockedUserId

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RippleOutlinedButton(
                        text = blockedName,
                        onClick = { onOpenUserProfile(blockedUserId) },
                        modifier = Modifier.weight(1f),
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    UserActionMenuButton(
                        actions =
                            listOf(
                                UserActionMenuItem(
                                    label = "Unblock from Club",
                                    onClick = { unblockFromClub(blockedUserId) },
                                ),
                            ),
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}
