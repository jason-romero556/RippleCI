package com.example.rippleci.data

import androidx.compose.ui.graphics.Color
import com.example.rippleci.data.models.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

object UserPresence {
    const val AUTOMATIC = "automatic"
    const val ONLINE = "online"
    const val IDLE = "idle"
    const val OFFLINE = "offline"
    const val MINIMIZED = "minimized"
    const val CLOSED = "closed"

    private const val ONLINE_STALE_AFTER_MS = 90_000L
    private const val MINIMIZED_STALE_AFTER_MS = 10 * 60 * 1000L

    fun resolveStatus(
        rawStatus: String,
        updatedAt: Long,
        presenceMode: String = AUTOMATIC,
        now: Long = System.currentTimeMillis(),
    ): String {
        when (presenceMode) {
            ONLINE -> return ONLINE
            IDLE, MINIMIZED -> return IDLE
            OFFLINE, CLOSED -> return OFFLINE
        }

        if (updatedAt <= 0L) return OFFLINE

        return when (rawStatus) {
            ONLINE -> if (now - updatedAt <= ONLINE_STALE_AFTER_MS) ONLINE else OFFLINE
            IDLE, MINIMIZED -> if (now - updatedAt <= MINIMIZED_STALE_AFTER_MS) IDLE else OFFLINE
            OFFLINE, CLOSED -> OFFLINE
            else -> OFFLINE
        }
    }

    fun resolveStatus(user: UserProfile, now: Long = System.currentTimeMillis()): String =
        resolveStatus(
            rawStatus = user.presenceStatus,
            updatedAt = user.presenceUpdatedAt,
            presenceMode = user.presenceMode,
            now = now,
        )

    fun statusColor(status: String): Color =
        when (status) {
            ONLINE -> Color(0xFF35C759)
            IDLE, MINIMIZED -> Color(0xFFFFCC00)
            else -> Color(0xFF8E8E93)
        }

    fun statusLabel(status: String): String =
        when (status) {
            ONLINE -> "Online"
            IDLE, MINIMIZED -> "Idle"
            else -> "Offline"
        }

    fun statusForMode(presenceMode: String): String =
        when (presenceMode) {
            ONLINE -> ONLINE
            IDLE, MINIMIZED -> IDLE
            OFFLINE, CLOSED -> OFFLINE
            else -> ONLINE
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
