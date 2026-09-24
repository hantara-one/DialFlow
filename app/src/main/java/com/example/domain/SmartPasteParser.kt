package com.example.domain

object SmartPasteParser {

    /**
     * Extracts individual phone-number candidates from raw text (including Google Sheets paste,
     * HTML/CSS snippets, and concatenated numeric sequences), preserving original ordering.
     */
    fun extractNumbers(rawText: String): List<String> {
        if (rawText.isBlank()) return emptyList()

        // Requirement 7: Prevent duplicate extraction if input contains both HTML table
        // representation and plain-text representation of the same numbers.
        val htmlCellNumbers = extractFromHtmlTableCells(rawText)
        if (htmlCellNumbers.isNotEmpty()) {
            val nonHtmlText = removeHtmlTablePortion(rawText)
            val nonHtmlNumbers = extractFromPlainText(nonHtmlText)
            if (nonHtmlNumbers.isEmpty() || nonHtmlNumbers == htmlCellNumbers) {
                return htmlCellNumbers
            }
            // If the non-HTML portion is identical or a duplicate copy, prefer the clean HTML table cells
            if (isDuplicateSequence(htmlCellNumbers, nonHtmlNumbers)) {
                return htmlCellNumbers
            }
        }

        return extractFromPlainText(rawText)
    }

    private fun extractFromHtmlTableCells(rawText: String): List<String> {
        if (!rawText.contains("<td", ignoreCase = true) && !rawText.contains("<th", ignoreCase = true)) {
            return emptyList()
        }

        val cellRegex = Regex("(?i)<t[dh][^>]*>(.*?)</t[dh]>", RegexOption.DOT_MATCHES_ALL)
        val matches = cellRegex.findAll(rawText).toList()
        if (matches.isEmpty()) return emptyList()

        val results = mutableListOf<String>()
        for (match in matches) {
            val cellContent = match.groupValues[1]
            val cleanedCell = cleanFormattingNoise(cellContent).trim()
            if (cleanedCell.isBlank()) continue

            val numbersFromCell = extractFromPlainText(cleanedCell)
            results.addAll(numbersFromCell)
        }
        return results
    }

    private fun removeHtmlTablePortion(rawText: String): String {
        return rawText.replace(Regex("(?is)<table[^>]*>.*?</table>"), " ")
            .replace(Regex("(?is)<!--StartFragment-->.*?<!--EndFragment-->"), " ")
    }

    private fun isDuplicateSequence(primary: List<String>, secondary: List<String>): Boolean {
        if (primary == secondary) return true
        val normalizedPrimary = primary.map { PhoneNumberNormalizer.normalize(it).normalizedNumber }
        val normalizedSecondary = secondary.map { PhoneNumberNormalizer.normalize(it).normalizedNumber }
        return normalizedPrimary.isNotEmpty() && normalizedPrimary == normalizedSecondary
    }

    /**
     * Cleans clipboard formatting noise (HTML tags, CSS declarations, comments, attributes)
     * and segments lines / concatenated phone numbers.
     */
    fun extractFromPlainText(rawText: String): List<String> {
        val hasFormatting = hasHtmlOrCssFormatting(rawText)
        val cleaned = cleanFormattingNoise(rawText)

        // Split by newlines, commas, semicolons, tabs, and pipes
        val rawLines = cleaned.split(Regex("[\\r\\n,;\\t|]+"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        val extractedCandidates = mutableListOf<String>()

        for (line in rawLines) {
            // Check if the line contains multiple '+' signs (e.g., "+628123456789+628987654321")
            if (line.count { it == '+' } > 1) {
                val plusSegments = line.split(Regex("(?<=\\S)(?=\\+)"))
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                for (seg in plusSegments) {
                    processSingleToken(seg, hasFormatting, extractedCandidates)
                }
                continue
            }

            // Check if line as a whole is already a valid phone number
            val wholeNorm = PhoneNumberNormalizer.normalize(line)
            val digitsOnly = line.filter { it.isDigit() }

            if (wholeNorm.isValid && digitsOnly.length <= 16) {
                extractedCandidates.add(line)
                continue
            }

            // If not valid as a whole, attempt concatenated numbers segmentation (Requirements 3 & 4)
            val segments = segmentConcatenatedNumbers(line)
            if (segments != null && segments.size >= 2) {
                extractedCandidates.addAll(segments)
                continue
            }

            // Check if line has multiple space-separated tokens
            val spaceTokens = line.split(Regex("\\s+")).filter { it.isNotBlank() }
            if (spaceTokens.size > 1) {
                for (token in spaceTokens) {
                    processSingleToken(token, hasFormatting, extractedCandidates)
                }
                continue
            }

            // Single token handling with false positive prevention (Requirement 5, Test D, Test E)
            processSingleToken(line, hasFormatting, extractedCandidates)
        }

        return extractedCandidates
    }

    private fun processSingleToken(
        token: String,
        hasFormattingContext: Boolean,
        outList: MutableList<String>
    ) {
        val digitsOnly = token.filter { it.isDigit() }

        // Short fragments (CSS values like "1", "100", short IDs) must never enter the queue
        if (digitsOnly.length < 7) {
            return
        }

        // Sequences substantially longer than 16 digits that failed segmentation must not become giant numbers
        if (digitsOnly.length > 16) {
            val segmented = segmentConcatenatedNumbers(token)
            if (segmented != null && segmented.size >= 2) {
                outList.addAll(segmented)
            }
            return
        }

        val norm = PhoneNumberNormalizer.normalize(token)
        if (norm.isValid) {
            outList.add(token)
        } else if (!hasFormattingContext) {
            // In clean manual paste (without HTML/CSS noise), allow flagged numbers (7-16 digits)
            // so existing manual flagging workflow remains intact
            outList.add(token)
        }
    }

    /**
     * Attempts to reconstruct individual valid phone numbers from a continuous concatenated numeric string.
     * Uses dynamic programming bounded by DialFlow's phone number validation rules (lengths 7 to 16).
     * Prefers segmentations that produce valid DialFlow phone numbers, maximize the number of segments,
     * and match standard Indonesian mobile lengths (Requirements 3, 4, 8).
     */
    fun segmentConcatenatedNumbers(raw: String): List<String>? {
        // Strip out non-digit characters for segmentation, preserving leading '+' if present
        val hasLeadingPlus = raw.trimStart().startsWith("+")
        val digits = raw.filter { it.isDigit() }
        val n = digits.length

        // Any 2 Indonesian phone numbers require at least 18 digits (e.g. two 9-digit landlines)
        // or at least 20 digits (two 10-digit mobile numbers). Below 14 digits can never be two valid numbers.
        if (n < 14) return null

        // If 14-16 digits and already a valid single phone number, do not split
        if (n in 14..16 && PhoneNumberNormalizer.normalize(raw).isValid) {
            return null
        }

        // DP table: dp[i] holds the best segmentation for prefix digits[0 until i]
        val dp = arrayOfNulls<List<String>>(n + 1)
        val dpScore = IntArray(n + 1) { 0 }

        dp[0] = emptyList()

        for (i in 0 until n) {
            val currentSegments = dp[i] ?: continue
            val currentScore = dpScore[i]

            // Phone numbers in DialFlow must have 7 to 16 digits
            val maxLen = minOf(16, n - i)
            for (len in 7..maxLen) {
                val j = i + len
                val candidateDigits = digits.substring(i, j)

                // The first segment may inherit a leading '+' if originally present
                val candidateString = if (i == 0 && hasLeadingPlus) "+$candidateDigits" else candidateDigits

                val norm = PhoneNumberNormalizer.normalize(candidateString)
                if (!norm.isValid) continue

                // Heuristic score: prioritize mobile prefixes and standard lengths
                var score = 100
                if (candidateDigits.startsWith("628") || candidateDigits.startsWith("08")) {
                    score += 50
                    if (candidateDigits.length in 10..13) {
                        score += 30
                    }
                } else if (candidateDigits.startsWith("62") || candidateDigits.startsWith("0")) {
                    score += 20
                }

                val totalCandidateScore = currentScore + score
                val newSegments = currentSegments + candidateString

                val existing = dp[j]
                val shouldUpdate = when {
                    existing == null -> true
                    newSegments.size > existing.size -> true
                    newSegments.size == existing.size && totalCandidateScore > dpScore[j] -> true
                    else -> false
                }

                if (shouldUpdate) {
                    dp[j] = newSegments
                    dpScore[j] = totalCandidateScore
                }
            }
        }

        val bestResult = dp[n]
        return if (bestResult != null && bestResult.size >= 2) bestResult else null
    }

    /**
     * Removes HTML tags, CSS declarations, style blocks, comments, and CSS property metadata.
     */
    fun cleanFormattingNoise(raw: String): String {
        var text = raw

        // 1. Remove style and script blocks completely
        text = text.replace(Regex("(?is)<style[^>]*>.*?</style>"), " ")
        text = text.replace(Regex("(?is)<script[^>]*>.*?</script>"), " ")

        // 2. Remove HTML comments
        text = text.replace(Regex("(?s)<!--.*?-->"), " ")

        // 3. Remove CSS selector + declaration blocks, e.g. "td { border:1px solid #cccccc; }" or "br { }"
        text = text.replace(Regex("(?s)[a-zA-Z0-9_\\-.:#* >\\[\\]=\"']+\\s*\\{[^}]*\\}"), " ")

        // 4. Remove any remaining { ... } blocks
        text = text.replace(Regex("(?s)\\{[^}]*\\}"), " ")

        // 5. Convert cell/row/block line-breaking HTML tags to newlines
        text = text.replace(Regex("(?i)</(?:td|tr|p|div|li)>|<br\\s*/?>"), "\n")

        // 6. Remove remaining HTML tags
        text = text.replace(Regex("<[^>]+>"), " ")

        // 7. Remove CSS properties outside braces (e.g. border: 1px solid #ccc;)
        text = text.replace(
            Regex("(?i)\\b(?:border|color|background|font|margin|padding|width|height|z-index|mso-[a-z\\-]+)\\s*:[^;\\n\\r]+;?"),
            " "
        )

        // 8. Decode HTML entities
        text = text.replace("&nbsp;", " ")
            .replace("&#160;", " ")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")

        // 9. Remove stray CSS tokens like "border:1px" or "1px"
        text = text.replace(Regex("(?i)\\b\\d+px\\b"), " ")

        return text
    }

    /**
     * Checks if the raw text contains HTML or CSS formatting indicators.
     */
    fun hasHtmlOrCssFormatting(rawText: String): Boolean {
        return rawText.contains("{") ||
                rawText.contains("<") ||
                rawText.contains("border:", ignoreCase = true) ||
                rawText.contains("mso-", ignoreCase = true) ||
                rawText.contains("style=", ignoreCase = true) ||
                rawText.contains("td ", ignoreCase = true) ||
                rawText.contains("br {", ignoreCase = true)
    }
}
