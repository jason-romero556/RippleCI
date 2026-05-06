package com.example.rippleci.data

fun firstNameOrBlank(value: String?): String {
    val trimmed = value.orEmpty().trim()
    if (trimmed.isBlank() || trimmed.contains("@")) return ""

    return trimmed.split(Regex("\\s+")).firstOrNull().orEmpty()
}

fun firstNameFromCandidates(vararg candidates: String?): String =
    candidates
        .asSequence()
        .map { firstNameOrBlank(it) }
        .firstOrNull { it.isNotBlank() }
        ?: "Someone"
