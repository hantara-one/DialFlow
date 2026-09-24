package com.example.domain

import com.example.models.PhoneNumberEntry

/**
 * Utility for searching queue entries by partial number, ending digits,
 * raw number, or formatted display number without altering queue ordering.
 */
object QueueSearchMatcher {

    /**
     * Determines whether a [PhoneNumberEntry] matches a given search [query].
     * Supports substring matching, end-portion (suffix) matching,
     * and matches across formatted display, normalized number, and raw original input.
     */
    fun matches(entry: PhoneNumberEntry, query: String): Boolean {
        val q = query.trim()
        if (q.isEmpty()) return false

        val cleanQuery = q.lowercase()
        val queryDigits = q.filter { it.isDigit() }

        // 1. Substring matching across formatted display, normalized number, and raw input
        if (entry.formattedDisplay.contains(cleanQuery, ignoreCase = true)) return true
        if (entry.normalizedNumber.contains(cleanQuery, ignoreCase = true)) return true
        if (entry.originalInput.contains(cleanQuery, ignoreCase = true)) return true

        // 2. Digits-only substring and end-portion (suffix) matching
        if (queryDigits.isNotEmpty()) {
            val normDigits = entry.normalizedNumber.filter { it.isDigit() }
            val origDigits = entry.originalInput.filter { it.isDigit() }
            val displayDigits = entry.formattedDisplay.filter { it.isDigit() }

            if (normDigits.contains(queryDigits)) return true
            if (origDigits.contains(queryDigits)) return true
            if (displayDigits.contains(queryDigits)) return true

            // Especially effective when typing the end portion of the number
            if (normDigits.endsWith(queryDigits) || origDigits.endsWith(queryDigits)) return true
        }

        return false
    }

    /**
     * Filters a list of queue items while strictly preserving their original queue order.
     */
    fun filterQueue(items: List<PhoneNumberEntry>, query: String): List<PhoneNumberEntry> {
        val q = query.trim()
        if (q.isEmpty()) return emptyList()
        return items.filter { matches(it, q) }
    }
}
