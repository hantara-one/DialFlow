package com.example.domain

import com.example.models.CallStatus
import com.example.models.PhoneNumberEntry

data class ParseBatchResult(
    val entries: List<PhoneNumberEntry>,
    val totalCount: Int,
    val validCount: Int,
    val invalidCount: Int
)

object NumberParser {

    fun parse(rawText: String, startIndex: Int = 0): ParseBatchResult {
        if (rawText.isBlank()) {
            return ParseBatchResult(emptyList(), 0, 0, 0)
        }

        // Split by newlines, commas, or semicolons
        val lines = rawText.split(Regex("[\\r\\n,;]+"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        val entries = mutableListOf<PhoneNumberEntry>()
        var validCount = 0
        var invalidCount = 0

        lines.forEachIndexed { index, line ->
            val norm = PhoneNumberNormalizer.normalize(line)
            if (norm.isValid) {
                validCount++
            } else {
                invalidCount++
            }

            entries.add(
                PhoneNumberEntry(
                    id = 0L, // will be assigned by database auto-generate
                    originalInput = norm.rawInput,
                    normalizedNumber = norm.normalizedNumber,
                    formattedDisplay = norm.formattedDisplay,
                    status = CallStatus.PENDING,
                    isValid = norm.isValid,
                    validationMessage = norm.validationMessage,
                    orderIndex = startIndex + index,
                    callTimestamp = null,
                    notes = null
                )
            )
        }

        return ParseBatchResult(
            entries = entries,
            totalCount = entries.size,
            validCount = validCount,
            invalidCount = invalidCount
        )
    }
}
