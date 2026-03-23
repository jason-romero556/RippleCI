package com.example.rippleci.model

data class Event (
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val clubId: String = "",
    val location: String = "",
    val timestamp: Long = 0L
)