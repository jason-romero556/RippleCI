package com.example.rippleci.ui.messages

import androidx.lifecycle.ViewModel
import com.example.rippleci.data.Conversation
import com.example.rippleci.data.Message
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MessagesViewModel : ViewModel() {

    private val db = Firebase.firestore
    private val auth = Firebase.auth
    val currentUserId = auth.currentUser?.uid ?: ""
    val currentUserName = auth.currentUser?.email ?: ""

    private val _currentDisplayName = MutableStateFlow("")
    val currentDisplayName: StateFlow<String> = _currentDisplayName

    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations: StateFlow<List<Conversation>> = _conversations

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages

    init {
        loadConversations()
        loadCurrentUserName()
    }

    private fun loadCurrentUserName() {
        if (currentUserId.isEmpty()) return
        db.collection("users").document(currentUserId).get()
            .addOnSuccessListener { doc ->
                _currentDisplayName.value = doc.getString("name")
                    ?: auth.currentUser?.email
                            ?: ""
                // fixMemberNames()
            }
    }

    fun loadConversations() {
        if (currentUserId.isEmpty()) return
        db.collection("conversations")
            .whereArrayContains("members", currentUserId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val convos = snapshot?.documents?.map { doc ->
                    Conversation(
                        conversationId = doc.id,
                        members = (doc.get("members") as? List<*>)
                            ?.mapNotNull { it as? String } ?: emptyList(),
                        memberNames = (doc.get("memberNames") as? Map<*, *>)
                            ?.mapNotNull { (k, v) ->
                                if (k is String && v is String) k to v else null
                            }?.toMap() ?: emptyMap(),
                        isGroup = doc.getBoolean("isGroup") ?: false,
                        groupName = doc.getString("groupName") ?: "",
                        lastMessage = doc.getString("lastMessage") ?: "",
                        lastUpdated = doc.getLong("lastUpdated") ?: 0L
                    )
                } ?: emptyList()
                _conversations.value = convos
            }
    }

    fun loadMessages(conversationId: String) {
        db.collection("conversations").document(conversationId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val msgs = snapshot?.documents?.map { doc ->
                    android.util.Log.d("MSG_DEBUG", "message senderId: ${doc.getString("senderId")} senderName: ${doc.getString("senderName")}")
                    Message(
                        messageId = doc.id,
                        senderId = doc.getString("senderId") ?: "",
                        senderName = doc.getString("senderName") ?: "",
                        text = doc.getString("text") ?: "",
                        timestamp = doc.getLong("timestamp") ?: 0L
                    )
                } ?: emptyList()
                _messages.value = msgs
            }
    }

    fun sendMessage(conversationId: String, text: String) {
        if (text.isBlank()) return
        val message = hashMapOf(
            "senderId" to currentUserId,
            "senderName" to _currentDisplayName.value.ifEmpty { currentUserName },
            "text" to text,
            "timestamp" to System.currentTimeMillis()
        )
        val convRef = db.collection("conversations").document(conversationId)
        convRef.collection("messages").add(message)
        convRef.update(
            mapOf(
                "lastMessage" to text,
                "lastUpdated" to System.currentTimeMillis()
            )
        )
    }

    fun getOrCreateDMConversation(
        otherUserId: String,
        otherUserName: String,
        onReady: (String) -> Unit
    )

    {
        if (otherUserId == currentUserId) {
            android.util.Log.e("MSG_ERROR", "Tried to message self")
            return
        }
        val conversationId = listOf(currentUserId, otherUserId)
            .sorted().joinToString("_")

        val ref = db.collection("conversations").document(conversationId)
        ref.get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                onReady(conversationId)
            } else {
                val myName = _currentDisplayName.value.ifEmpty { currentUserName }
                val convo = hashMapOf(
                    "members" to listOf(currentUserId, otherUserId),
                    "memberNames" to mapOf(
                        currentUserId to myName,
                        otherUserId to otherUserName
                    ),
                    "isGroup" to false,
                    "groupName" to "",
                    "lastMessage" to "",
                    "lastUpdated" to System.currentTimeMillis()
                )
                ref.set(convo).addOnSuccessListener { onReady(conversationId) }
            }
        }
    }

    fun createGroupConversation(
        memberIds: List<String>,
        memberNames: Map<String, String>,
        groupName: String,
        onReady: (String) -> Unit
    ) {
        val allMembers = (memberIds + currentUserId).distinct()
        val allNames = memberNames + (currentUserId to _currentDisplayName.value.ifEmpty { currentUserName })
        val convo = hashMapOf(
            "members" to allMembers,
            "memberNames" to allNames,
            "isGroup" to true,
            "groupName" to groupName,
            "lastMessage" to "",
            "lastUpdated" to System.currentTimeMillis()
        )
        db.collection("conversations").add(convo)
            .addOnSuccessListener { onReady(it.id) }
    }
  /*  fun fixMemberNames() {
        if (currentUserId.isEmpty()) return
        db.collection("conversations")
            .whereArrayContains("members", currentUserId)
            .get()
            .addOnSuccessListener { snapshot ->
                val myName = _currentDisplayName.value.ifEmpty { currentUserName }
                snapshot.documents.forEach { doc ->
                    val members = (doc.get("members") as? List<*>)
                        ?.mapNotNull { it as? String } ?: emptyList()

                    val otherMemberIds = members.filter { it != currentUserId }

                    // Update your own name independently
                    doc.reference.update("memberNames.$currentUserId", myName)

                    // Update each other member's name independently
                    otherMemberIds.forEach { otherId ->
                        db.collection("users").document(otherId).get()
                            .addOnSuccessListener { userDoc ->
                                val realName = userDoc.getString("name")
                                    ?: userDoc.getString("email")
                                    ?: "Unknown"
                                doc.reference.update("memberNames.$otherId", realName)
                            }
                    }
                }
            }
    } */
    fun clearChatHistory(conversationId: String, onComplete: () -> Unit) {
        db.collection("conversations").document(conversationId)
            .collection("messages")
            .get()
            .addOnSuccessListener { snapshot ->
                val batch = db.batch()
                snapshot.documents.forEach { doc ->
                    batch.delete(doc.reference)
                }
                batch.commit().addOnSuccessListener {
                    _messages.value = emptyList()
                    db.collection("conversations").document(conversationId)
                        .update(
                            mapOf(
                                "lastMessage" to "",
                                "lastUpdated" to System.currentTimeMillis()
                            )
                        )
                    onComplete()
                }
            }
    }
}