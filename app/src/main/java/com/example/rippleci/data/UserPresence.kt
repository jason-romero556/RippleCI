package com.example.rippleci.data

import androidx.compose.ui.graphics.Color
import com.example.rippleci.data.models.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

object UserPresence {
    const val ONLINE = "online"
    const val MINIMIZED = "minimized"
    const val CLOSED = "closed"

    private const val ONLINE_STALE_AFTER_MS = 90_000L
    private const val MINIMIZED_STALE_AFTER_MS = 10 * 60 * 1000L

    fun resolveStatus(
        rawStatus: String,
        updatedAt: Long,
        now: Long = System.currentTimeMillis(),
    ): String {
        if (updatedAt <= 0L) return CLOSED

        return when (rawStatus) {
            ONLINE -> if (now - updatedAt <= ONLINE_STALE_AFTER_MS) ONLINE else CLOSED
            MINIMIZED -> if (now - updatedAt <= MINIMIZED_STALE_AFTER_MS) MINIMIZED else CLOSED
            CLOSED -> CLOSED
            else -> CLOSED
        }
    }

    fun resolveStatus(user: UserProfile, now: Long = System.currentTimeMillis()): String =
        resolveStatus(
            rawStatus = user.presenceStatus,
            updatedAt = user.presenceUpdatedAt,
            now = now,
        )

    fun statusColor(status: String): Color =
        when (status) {
            ONLINE -> Color(0xFF35C759)
            MINIMIZED -> Color(0xFFFFCC00)
            else -> Color(0xFF8E8E93)
        }

    fun statusLabel(status: String): String =
        when (status) {
            ONLINE -> "Online"
            MINIMIZED -> "Idle"
            else -> "Offline"
        }

    fun update(userId: String, status: String) {
        if (userId.isBlank()) return

        Firebase.firestore
            .collection("users")
            .document(userId)
            .update(
                mapOf(
                    "presenceStatus" to status,
                    "presenceUpdatedAt" to System.currentTimeMillis(),
                ),
            )
    }
}
