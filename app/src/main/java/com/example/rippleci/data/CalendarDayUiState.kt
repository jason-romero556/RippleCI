package com.example.rippleci.data

import com.example.rippleci.data.models.PersonalEvent

data class CalendarDayUiState(
    val dateLabel: String,
    val events: List<PersonalEvent>,
)
