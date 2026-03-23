package com.example.rippleci.logic

import com.example.rippleci.data.FirebaseRepository
import com.example.rippleci.model.Event
import java.util.UUID

object EventManager {

    suspend fun createEvent(title: String, description: String): Event {
        val event = Event(
            id = UUID.randomUUID().toString(),
            title = title,
            description = description,
            clubId = "1",
            timestamp = System.currentTimeMillis()
        )

        FirebaseRepository.addEvent(event)
        return event
    }

    suspend fun loadEvents(): List<Event> {
        return FirebaseRepository.getEvents()
    }
}