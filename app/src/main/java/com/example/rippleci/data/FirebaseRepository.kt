package com.example.rippleci.data

import com.google.firebase.firestore.FirebaseFirestore
import com.example.rippleci.model.Event
import kotlinx.coroutines.tasks.await

object FirebaseRepository {

    private val db = FirebaseFirestore.getInstance()

    suspend fun addEvent(event: Event) {
        db.collection("events")
            .document(event.id)
            .set(event)
            .await()
    }

    suspend fun getEvents(): List<Event> {
        val snapshot = db.collection("events")
            .get()
            .await()
        return snapshot.documents.mapNotNull {
            it.toObject(Event::class.java)
        }
    }
}