package com.example.domain

import com.example.models.PhoneNumberEntry
import com.example.models.PhoneNumberFormatPreference

data class NormalizationResult(
    val isValid: Boolean,
    val rawInput: String,
    val normalizedNumber: String,
    val formattedDisplay: String,
    val validationMessage: String? = null
)

object PhoneNumberNormalizer {

    fun normalize(raw: String): NormalizationResult {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) {
            return NormalizationResult(
                isValid = false,
                rawInput = raw,
                normalizedNumber = raw,
                formattedDisplay = raw,
                validationMessage = "Empty phone number"
            )
        }

        // Check for forbidden characters (e.g., letters, illegal symbols)
        // Allowed characters in formatted phone strings: digits, +, -, ( ), ., spaces
        val forbiddenChars = trimmed.filter { char ->
            !char.isDigit() && char != '+' && char != '-' && char != '(' && char != ')' && char != '.' && char != ' '
        }
        if (forbiddenChars.isNotEmpty()) {
            return NormalizationResult(
                isValid = false,
                rawInput = trimmed,
                normalizedNumber = trimmed,
                formattedDisplay = trimmed,
                validationMessage = "Invalid characters detected: $forbiddenChars"
            )
        }

        // Check if '+' appears anywhere other than index 0
        if (trimmed.indexOf('+') > 0 || trimmed.count { it == '+' } > 1) {
            return NormalizationResult(
                isValid = false,
                rawInput = trimmed,
                normalizedNumber = trimmed,
                formattedDisplay = trimmed,
                validationMessage = "Invalid '+' symbol placement"
            )
        }

        // Strip non-digit and non-plus characters
        val clean = trimmed.filter { it.isDigit() || it == '+' }
        val digitsOnly = clean.filter { it.isDigit() }

        if (digitsOnly.length < 7) {
            return NormalizationResult(
                isValid = false,
                rawInput = trimmed,
                normalizedNumber = trimmed,
                formattedDisplay = trimmed,
                validationMessage = "Number too short (${digitsOnly.length} digits)"
            )
        }

        if (digitsOnly.length > 16) {
            return NormalizationResult(
                isValid = false,
                rawInput = trimmed,
                normalizedNumber = trimmed,
                formattedDisplay = trimmed,
                validationMessage = "Number too long (${digitsOnly.length} digits)"
            )
        }

        // Indonesian phone number normalization
        // Case 1: Starts with +62
        if (clean.startsWith("+62")) {
            val nationalDigits = clean.substring(3)
            if (nationalDigits.startsWith("8")) {
                // Indonesian Mobile
                if (nationalDigits.length in 8..13) {
                    val normalized = "+62$nationalDigits"
                    return NormalizationResult(
                        isValid = true,
                        rawInput = trimmed,
                        normalizedNumber = normalized,
                        formattedDisplay = formatIndonesianMobile(nationalDigits),
                        validationMessage = null
                    )
                }
            } else if (nationalDigits.isNotEmpty() && nationalDigits.length in 7..12) {
                // Indonesian Landline or special
                val normalized = "+62$nationalDigits"
                return NormalizationResult(
                    isValid = true,
                    rawInput = trimmed,
                    normalizedNumber = normalized,
                    formattedDisplay = "0$nationalDigits",
                    validationMessage = null
                )
            }
        }

        // Case 2: Starts with 62 without plus
        if (clean.startsWith("62") && !clean.startsWith("+")) {
            val nationalDigits = clean.substring(2)
            if (nationalDigits.startsWith("8")) {
                if (nationalDigits.length in 8..13) {
                    val normalized = "+62$nationalDigits"
                    return NormalizationResult(
                        isValid = true,
                        rawInput = trimmed,
                        normalizedNumber = normalized,
                        formattedDisplay = formatIndonesianMobile(nationalDigits),
                        validationMessage = null
                    )
                }
            } else if (nationalDigits.isNotEmpty() && nationalDigits.length in 7..12) {
                val normalized = "+62$nationalDigits"
                return NormalizationResult(
                    isValid = true,
                    rawInput = trimmed,
                    normalizedNumber = normalized,
                    formattedDisplay = "0$nationalDigits",
                    validationMessage = null
                )
            }
        }

        // Case 3: Starts with 08 (Standard Indonesian National format)
        if (clean.startsWith("08")) {
            val nationalDigits = clean.substring(1) // starts with '8'
            if (nationalDigits.length in 8..13) {
                val normalized = "+62$nationalDigits"
                return NormalizationResult(
                    isValid = true,
                    rawInput = trimmed,
                    normalizedNumber = normalized,
                    formattedDisplay = formatIndonesianMobile(nationalDigits),
                    validationMessage = null
                )
            }
        }

        // Case 4: Other standard Indonesian 0-prefixed numbers (e.g., landlines like 021...)
        if (clean.startsWith("0") && clean.length in 9..14) {
            val nationalDigits = clean.substring(1)
            val normalized = "+62$nationalDigits"
            return NormalizationResult(
                isValid = true,
                rawInput = trimmed,
                normalizedNumber = normalized,
                formattedDisplay = clean,
                validationMessage = null
            )
        }

        // Case 5: Starts with '+' but not Indonesia (International numbers)
        if (clean.startsWith("+")) {
            return NormalizationResult(
                isValid = true,
                rawInput = trimmed,
                normalizedNumber = clean,
                formattedDisplay = clean,
                validationMessage = null
            )
        }

        // Ambiguous: e.g. starts with '812...' without leading 0 or 62
        if (clean.startsWith("8") && clean.length in 9..12) {
            return NormalizationResult(
                isValid = false,
                rawInput = trimmed,
                normalizedNumber = trimmed,
                formattedDisplay = trimmed,
                validationMessage = "Ambiguous: Missing leading '0' or country code (+62)"
            )
        }

        // Unrecognized format - do not silently modify
        return NormalizationResult(
            isValid = false,
            rawInput = trimmed,
            normalizedNumber = trimmed,
            formattedDisplay = trimmed,
            validationMessage = "Unrecognized phone number format"
        )
    }

    private fun formatIndonesianMobile(nationalDigitsStartingWith8: String): String {
        // e.g. nationalDigits: 81234567890 -> "0812-3456-7890"
        val full = "0$nationalDigitsStartingWith8"
        return when {
            full.length <= 4 -> full
            full.length in 5..8 -> "${full.substring(0, 4)}-${full.substring(4)}"
            full.length in 9..12 -> "${full.substring(0, 4)}-${full.substring(4, 8)}-${full.substring(8)}"
            else -> "${full.substring(0, 4)}-${full.substring(4, 8)}-${full.substring(8, 12)}-${full.substring(12)}"
        }
    }

    fun formatIndonesianMobileInternational(nationalDigitsStartingWith8: String): String {
        val d = nationalDigitsStartingWith8
        val formattedNational = when {
            d.length <= 3 -> d
            d.length in 4..7 -> "${d.substring(0, 3)}-${d.substring(3)}"
            d.length in 8..11 -> "${d.substring(0, 3)}-${d.substring(3, 7)}-${d.substring(7)}"
            else -> "${d.substring(0, 3)}-${d.substring(3, 7)}-${d.substring(7, 11)}-${d.substring(11)}"
        }
        return "+62 $formattedNational"
    }

    /**
     * Resolves the display presentation for a queue entry based on the user's
     * presentation preference. Does not mutate the underlying canonical entry.
     */
    fun getDisplayNumber(
        entry: PhoneNumberEntry,
        preference: PhoneNumberFormatPreference
    ): String {
        if (!entry.isValid) {
            return entry.formattedDisplay.ifBlank { entry.originalInput }
        }
        val normalized = entry.normalizedNumber
        if (!normalized.startsWith("+62")) {
            return entry.formattedDisplay.ifBlank { normalized }
        }
        val nationalDigits = normalized.substring(3)
        return when (preference) {
            PhoneNumberFormatPreference.LOCAL -> {
                if (nationalDigits.startsWith("8")) {
                    formatIndonesianMobile(nationalDigits)
                } else {
                    "0$nationalDigits"
                }
            }
            PhoneNumberFormatPreference.INTERNATIONAL -> {
                if (nationalDigits.startsWith("8")) {
                    formatIndonesianMobileInternational(nationalDigits)
                } else {
                    "+62 $nationalDigits"
                }
            }
        }
    }

    /**
     * Resolves the exact dialing target passed to Android Telecom based on the
     * user's preference. Local: 08..., International: +62...
     */
    fun getDialTarget(
        entry: PhoneNumberEntry,
        preference: PhoneNumberFormatPreference
    ): String {
        if (!entry.isValid) {
            return entry.originalInput
        }
        val normalized = entry.normalizedNumber
        if (!normalized.startsWith("+62")) {
            return normalized
        }
        val nationalDigits = normalized.substring(3)
        return when (preference) {
            PhoneNumberFormatPreference.LOCAL -> "0$nationalDigits"
            PhoneNumberFormatPreference.INTERNATIONAL -> "+62$nationalDigits"
        }
    }
}
