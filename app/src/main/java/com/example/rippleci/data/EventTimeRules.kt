package com.example.rippleci.data

import com.example.rippleci.data.models.PersonalEvent
import com.example.rippleci.data.models.SchoolEvent
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

fun PersonalEvent.isOnDay(dayMillis: Long): Boolean =
    effectiveStartMillis()?.let { isSameLocalDay(it, dayMillis) } ?: false

fun SchoolEvent.stableSchoolEventId(): String {
    if (eventId > 0L) return eventId.toString()
    if (id.isNotBlank()) return id

    val fallback = "${title}_${startDateTime}_${permaLinkUrl}"
    return fallback.hashCode().toUInt().toString(16)
}

fun SchoolEvent.startMillis(): Long? = parseSchoolEventMillis(startDateTime)

fun SchoolEvent.endMillis(): Long? = parseSchoolEventMillis(endDateTime) ?: startMillis()

fun SchoolEvent.isPastSchoolEvent(nowMillis: Long = System.currentTimeMillis()): Boolean =
    endMillis()?.let { it < nowMillis } ?: false

fun SchoolEvent.isOnDay(dayMillis: Long): Boolean =
    startMillis()?.let { isSameLocalDay(it, dayMillis) } ?: false

fun SchoolEvent.schoolEventSortMillis(): Long =
    startMillis() ?: Long.MAX_VALUE

fun SchoolEvent.toFirestoreMap(): Map<String, Any> =
    mapOf(
        "id" to stableSchoolEventId(),
        "eventId" to eventId,
        "title" to title,
        "description" to description,
        "location" to location,
        "startDateTime" to startDateTime,
        "endDateTime" to endDateTime,
        "dateTimeFormatted" to dateTimeFormatted,
        "permaLinkUrl" to permaLinkUrl,
        "markedAt" to System.currentTimeMillis(),
    )

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

private fun parseSchoolEventMillis(value: String): Long? {
    if (value.isBlank()) return null

    val formats =
        listOf(
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm",
            "yyyy-MM-dd",
        )

    formats.forEach { pattern ->
        parseDate(pattern, value)?.let { return it.time }
    }

    return null
}

private fun isSameLocalDay(
    firstMillis: Long,
    secondMillis: Long,
): Boolean {
    val first = Calendar.getInstance().apply { timeInMillis = firstMillis }
    val second = Calendar.getInstance().apply { timeInMillis = secondMillis }

    return first.get(Calendar.YEAR) == second.get(Calendar.YEAR) &&
        first.get(Calendar.DAY_OF_YEAR) == second.get(Calendar.DAY_OF_YEAR)
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
