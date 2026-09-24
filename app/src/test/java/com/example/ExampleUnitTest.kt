package com.example

import com.example.domain.NumberParser
import com.example.domain.PhoneNumberNormalizer
import com.example.domain.SmartPasteParser
import com.example.models.CallStatus
import com.example.models.PhoneNumberEntry
import com.example.models.PhoneNumberFormatPreference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun `Test A - Normal paste creates 2 valid queue entries`() {
        val input = """
            628123456789
            628987654321
        """.trimIndent()

        val result = NumberParser.parse(input)
        assertEquals(2, result.totalCount)
        assertEquals(2, result.validCount)
        assertEquals(0, result.invalidCount)
        assertEquals("+628123456789", result.entries[0].normalizedNumber)
        assertEquals("+628987654321", result.entries[1].normalizedNumber)
    }

    @Test
    fun `Test B - Google Sheets formatting ignores CSS and extracts valid phone number`() {
        val input = "td { border:1px solid #cccccc; } br { } 628123456789"

        val result = NumberParser.parse(input)
        assertEquals(1, result.totalCount)
        assertEquals(1, result.validCount)
        assertEquals(0, result.invalidCount)
        assertEquals("+628123456789", result.entries[0].normalizedNumber)
        assertEquals("628123456789", result.entries[0].originalInput)
    }

    @Test
    fun `Test C - Concatenated numbers are accurately segmented into individual numbers`() {
        val input = "6281234567628565332156287529874"

        val result = NumberParser.parse(input)
        assertEquals(3, result.totalCount)
        assertEquals(3, result.validCount)
        assertEquals(0, result.invalidCount)
        assertEquals("+6281234567", result.entries[0].normalizedNumber)
        assertEquals("+62856533215", result.entries[1].normalizedNumber)
        assertEquals("+6287529874", result.entries[2].normalizedNumber)
    }

    @Test
    fun `Test C2 - Concatenated numbers starting with 08 segment correctly`() {
        val input = "081234567890082345678901083456789012"

        val result = NumberParser.parse(input)
        assertEquals(3, result.totalCount)
        assertEquals(3, result.validCount)
        assertEquals("+6281234567890", result.entries[0].normalizedNumber)
        assertEquals("+6282345678901", result.entries[1].normalizedNumber)
        assertEquals("+6283456789012", result.entries[2].normalizedNumber)
    }

    @Test
    fun `Test D - Very long numeric content does not become one giant phone number`() {
        val input = "98765432109876543210987654321098765432109876543210"

        val result = NumberParser.parse(input)
        assertEquals(0, result.totalCount)
        assertTrue(result.entries.isEmpty())
    }

    @Test
    fun `Test E - Invalid numeric content and CSS metadata are discarded, only valid phone numbers enter`() {
        val input = "td { border:1px solid #cccccc; } br { } 1 2 100 width: 120px; 628123456789"

        val result = NumberParser.parse(input)
        assertEquals(1, result.totalCount)
        assertEquals(1, result.validCount)
        assertEquals(0, result.invalidCount)
        assertEquals("+628123456789", result.entries[0].normalizedNumber)
    }

    @Test
    fun `Test F - Existing manual workflow with newlines, commas, semicolons, and dashes is preserved`() {
        val input = """
            081234567890
            082345678901
            +628567890123
            0812-9999-8888
        """.trimIndent()

        val result = NumberParser.parse(input)
        assertEquals(4, result.totalCount)
        assertEquals(4, result.validCount)
        assertEquals(0, result.invalidCount)
        assertEquals("+6281234567890", result.entries[0].normalizedNumber)
        assertEquals("+6282345678901", result.entries[1].normalizedNumber)
        assertEquals("+628567890123", result.entries[2].normalizedNumber)
        assertEquals("+6281299998888", result.entries[3].normalizedNumber)
    }

    @Test
    fun `Google Sheets combined CSS and concatenated phone numbers paste`() {
        val input = "td { border:1px solid #cccccc; } br { } 6281234567628565332156287529874"

        val result = NumberParser.parse(input)
        assertEquals(3, result.totalCount)
        assertEquals(3, result.validCount)
        assertEquals("+6281234567", result.entries[0].normalizedNumber)
        assertEquals("+62856533215", result.entries[1].normalizedNumber)
        assertEquals("+6287529874", result.entries[2].normalizedNumber)
    }

    @Test
    fun `Requirement 7 - Duplicate extraction prevented when clipboard contains HTML table and plain text duplicate`() {
        val input = """
            <table>
                <tr><td>628111111111</td></tr>
                <tr><td>628222222222</td></tr>
                <tr><td>628333333333</td></tr>
            </table>
            628111111111
            628222222222
            628333333333
        """.trimIndent()

        val result = NumberParser.parse(input)
        assertEquals(3, result.totalCount)
        assertEquals(3, result.validCount)
        assertEquals("+628111111111", result.entries[0].normalizedNumber)
        assertEquals("+628222222222", result.entries[1].normalizedNumber)
        assertEquals("+628333333333", result.entries[2].normalizedNumber)
    }

    @Test
    fun `Requirement 6 - Exact ordering is preserved`() {
        val input = """
            628111111111
            628222222222
            628333333333
        """.trimIndent()

        val result = NumberParser.parse(input)
        assertEquals(3, result.totalCount)
        assertEquals("+628111111111", result.entries[0].normalizedNumber)
        assertEquals("+628222222222", result.entries[1].normalizedNumber)
        assertEquals("+628333333333", result.entries[2].normalizedNumber)
        assertEquals(0, result.entries[0].orderIndex)
        assertEquals(1, result.entries[1].orderIndex)
        assertEquals(2, result.entries[2].orderIndex)
    }

    @Test
    fun `Multiple plus signs in concatenated string split correctly`() {
        val input = "+628123456789+628987654321"

        val result = NumberParser.parse(input)
        assertEquals(2, result.totalCount)
        assertEquals("+628123456789", result.entries[0].normalizedNumber)
        assertEquals("+628987654321", result.entries[1].normalizedNumber)
    }

    @Test
    fun `previewCount returns accurate number count for both normal and concatenated inputs`() {
        assertEquals(0, NumberParser.previewCount(""))
        assertEquals(2, NumberParser.previewCount("628123456789\n628987654321"))
        assertEquals(3, NumberParser.previewCount("td { border:1px solid #cccccc; } br { } 6281234567628565332156287529874"))
    }

    @Test
    fun `Test A - Local Preference Standard Mobile`() {
        val parsed = NumberParser.parse("6281234567890").entries.first()
        val display = PhoneNumberNormalizer.getDisplayNumber(parsed, PhoneNumberFormatPreference.LOCAL)
        val dialTarget = PhoneNumberNormalizer.getDialTarget(parsed, PhoneNumberFormatPreference.LOCAL)

        assertEquals("0812-3456-7890", display)
        assertEquals("081234567890", dialTarget)
        assertEquals("+6281234567890", parsed.normalizedNumber) // canonical preserved
    }

    @Test
    fun `Test B - International Preference Standard Mobile`() {
        val parsed = NumberParser.parse("6281234567890").entries.first()
        val display = PhoneNumberNormalizer.getDisplayNumber(parsed, PhoneNumberFormatPreference.INTERNATIONAL)
        val dialTarget = PhoneNumberNormalizer.getDialTarget(parsed, PhoneNumberFormatPreference.INTERNATIONAL)

        assertEquals("+62 812-3456-7890", display)
        assertEquals("+6281234567890", dialTarget)
        assertEquals("+6281234567890", parsed.normalizedNumber) // canonical preserved
    }

    @Test
    fun `Test C - Dynamic Switch without mutating entry`() {
        val parsed = NumberParser.parse("6281234567890").entries.first()

        // 1. Local
        assertEquals("0812-3456-7890", PhoneNumberNormalizer.getDisplayNumber(parsed, PhoneNumberFormatPreference.LOCAL))
        assertEquals("081234567890", PhoneNumberNormalizer.getDialTarget(parsed, PhoneNumberFormatPreference.LOCAL))

        // 2. Switch to International
        assertEquals("+62 812-3456-7890", PhoneNumberNormalizer.getDisplayNumber(parsed, PhoneNumberFormatPreference.INTERNATIONAL))
        assertEquals("+6281234567890", PhoneNumberNormalizer.getDialTarget(parsed, PhoneNumberFormatPreference.INTERNATIONAL))

        // Canonical number unchanged
        assertEquals("+6281234567890", parsed.normalizedNumber)
    }

    @Test
    fun `Test D - Different Input Formats all resolve correctly`() {
        val inputs = listOf("081234567890", "+6281234567890", "6281234567890")
        for (input in inputs) {
            val parsed = NumberParser.parse(input).entries.first()

            // Local
            assertEquals("0812-3456-7890", PhoneNumberNormalizer.getDisplayNumber(parsed, PhoneNumberFormatPreference.LOCAL))
            assertEquals("081234567890", PhoneNumberNormalizer.getDialTarget(parsed, PhoneNumberFormatPreference.LOCAL))

            // International
            assertEquals("+62 812-3456-7890", PhoneNumberNormalizer.getDisplayNumber(parsed, PhoneNumberFormatPreference.INTERNATIONAL))
            assertEquals("+6281234567890", PhoneNumberNormalizer.getDialTarget(parsed, PhoneNumberFormatPreference.INTERNATIONAL))

            // Canonical
            assertEquals("+6281234567890", parsed.normalizedNumber)
        }
    }

    @Test
    fun `Test E - Duplicate Detection Preserved - both representations share same canonical number`() {
        val input = """
            081234567890
            6281234567890
        """.trimIndent()
        val parsed = NumberParser.parse(input)
        assertEquals(2, parsed.entries.size)
        // Both entries normalize to the exact same internal canonical representation (+6281234567890)
        assertEquals(parsed.entries[0].normalizedNumber, parsed.entries[1].normalizedNumber)
        assertEquals("+6281234567890", parsed.entries[0].normalizedNumber)
        assertEquals("+6281234567890", parsed.entries[1].normalizedNumber)

        // Under Local format preference, both display and dial identically
        assertEquals("0812-3456-7890", PhoneNumberNormalizer.getDisplayNumber(parsed.entries[0], PhoneNumberFormatPreference.LOCAL))
        assertEquals("0812-3456-7890", PhoneNumberNormalizer.getDisplayNumber(parsed.entries[1], PhoneNumberFormatPreference.LOCAL))
        assertEquals("081234567890", PhoneNumberNormalizer.getDialTarget(parsed.entries[0], PhoneNumberFormatPreference.LOCAL))
        assertEquals("081234567890", PhoneNumberNormalizer.getDialTarget(parsed.entries[1], PhoneNumberFormatPreference.LOCAL))

        // Under International format preference, both display and dial identically
        assertEquals("+62 812-3456-7890", PhoneNumberNormalizer.getDisplayNumber(parsed.entries[0], PhoneNumberFormatPreference.INTERNATIONAL))
        assertEquals("+62 812-3456-7890", PhoneNumberNormalizer.getDisplayNumber(parsed.entries[1], PhoneNumberFormatPreference.INTERNATIONAL))
        assertEquals("+6281234567890", PhoneNumberNormalizer.getDialTarget(parsed.entries[0], PhoneNumberFormatPreference.INTERNATIONAL))
        assertEquals("+6281234567890", PhoneNumberNormalizer.getDialTarget(parsed.entries[1], PhoneNumberFormatPreference.INTERNATIONAL))
    }

    @Test
    fun `Test F - Invalid Non-standard Number fallback`() {
        val parsed = PhoneNumberNormalizer.normalize("12345")
        val entry = PhoneNumberEntry(
            id = 1L,
            originalInput = parsed.rawInput,
            normalizedNumber = parsed.normalizedNumber,
            formattedDisplay = parsed.formattedDisplay,
            isValid = parsed.isValid,
            validationMessage = parsed.validationMessage,
            status = CallStatus.PENDING,
            orderIndex = 0
        )

        assertFalse(entry.isValid)
        val displayLocal = PhoneNumberNormalizer.getDisplayNumber(entry, PhoneNumberFormatPreference.LOCAL)
        val displayIntl = PhoneNumberNormalizer.getDisplayNumber(entry, PhoneNumberFormatPreference.INTERNATIONAL)
        val dialLocal = PhoneNumberNormalizer.getDialTarget(entry, PhoneNumberFormatPreference.LOCAL)
        val dialIntl = PhoneNumberNormalizer.getDialTarget(entry, PhoneNumberFormatPreference.INTERNATIONAL)

        assertEquals("12345", displayLocal)
        assertEquals("12345", displayIntl)
        assertEquals("12345", dialLocal)
        assertEquals("12345", dialIntl)
    }
}
