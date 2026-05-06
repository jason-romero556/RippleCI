package com.example.rippleci.data

import com.example.rippleci.data.models.PersonalEvent
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

fun PersonalEvent.effectiveStartMillis(): Long? =
    startAtMillis
        .takeIf { it > 0L }
        ?: parseLegacyEventMillis(date, startTime)

fun PersonalEvent.effectiveEndMillis(): Long? =
    endAtMillis
        .takeIf { it > 0L }
        ?: parseLegacyEventMillis(date, endTime)
        ?: effectiveStartMillis()

fun PersonalEvent.isPastEvent(nowMillis: Long = System.currentTimeMillis()): Boolean =
    effectiveEndMillis()?.let { it < nowMillis } ?: false

fun PersonalEvent.eventSortMillis(): Long =
    effectiveStartMillis() ?: Long.MAX_VALUE

private fun parseLegacyEventMillis(
    date: String,
    time: String,
): Long? {
    if (date.isBlank()) return null

    val dateFormats = listOf("MMM d, yyyy", "yyyy-MM-dd", "M/d/yyyy", "MM/dd/yyyy")
    val timeFormats = listOf("h:mm a", "HH:mm", "H:mm")

    dateFormats.forEach { datePattern ->
        val parsedDate = parseDate(datePattern, date) ?: return@forEach

        if (time.isBlank()) {
            return parsedDate.time
        }

        timeFormats.forEach { timePattern ->
            val parsedTime = parseDate(timePattern, time) ?: return@forEach
            val dateCalendar = Calendar.getInstance().apply { this.time = parsedDate }
            val timeCalendar = Calendar.getInstance().apply { this.time = parsedTime }

            dateCalendar.set(Calendar.HOUR_OF_DAY, timeCalendar.get(Calendar.HOUR_OF_DAY))
            dateCalendar.set(Calendar.MINUTE, timeCalendar.get(Calendar.MINUTE))
            dateCalendar.set(Calendar.SECOND, 0)
            dateCalendar.set(Calendar.MILLISECOND, 0)

            return dateCalendar.timeInMillis
        }
    }

    return null
}

private fun parseDate(
    pattern: String,
    value: String,
): java.util.Date? =
    try {
        SimpleDateFormat(pattern, Locale.getDefault())
            .apply { isLenient = false }
            .parse(value)
    } catch (_: ParseException) {
        null
    }
