package com.example.domain

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
}
