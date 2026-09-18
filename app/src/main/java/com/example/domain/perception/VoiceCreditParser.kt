package com.example.domain.perception

import com.example.domain.model.Money
import com.example.domain.reconciliation.NameNormalizer
import java.util.Locale

/**
 * Parsed credit obligation extracted from voice transcript.
 */
data class ParsedCreditEntry(
    val rawTranscript: String,
    val customerName: String,
    val normalizedCustomerName: String,
    val amount: Money,
    val optionalNote: String? = null
)

/**
 * Deterministic local parser for retail kirana voice credit commands.
 * Handles shopkeeper phrases in English, Hindi, and Hinglish.
 */
object VoiceCreditParser {

    private val ACTION_KEYWORDS = setOf(
        "udhar", "credit", "khata", "baaki", "baki", "diya", "dena", "de", "diye",
        "likho", "likh", "daalo", "add", "karo", "rupaye", "rupee", "rupees", "rs",
        "inr", "ka", "ki", "ke", "ko", "se", "ne", "to", "for", "hai", "tha"
    )

    private val FILLER_WORDS = setOf(
        "ko", "ka", "ki", "ke", "se", "to", "for", "please", "bhai", "ji", "sahab"
    )

    // Pre-mapped spoken phrases to numeric values (multi-word patterns handled first)
    private val SPOKEN_NUMBER_REPLACEMENTS = listOf(
        // Compound Hindi numbers
        "dhai hazaar" to "2500",
        "dhayi hazaar" to "2500",
        "paanch hazaar" to "5000",
        "panch hazaar" to "5000",
        "chaar hazaar" to "4000",
        "teen hazaar" to "3000",
        "do hazaar" to "2000",
        "ek hazaar" to "1000",
        "hazaar" to "1000",
        "pandrah sau" to "1500",
        "baarah sau" to "1200",
        "barah sau" to "1200",
        "gyarah sau" to "1100",
        "nau sau" to "900",
        "aath sau" to "800",
        "saat sau" to "700",
        "chhe sau" to "600",
        "che sau" to "600",
        "paanch sau" to "500",
        "panch sau" to "500",
        "chaar sau" to "400",
        "teen sau" to "300",
        "dhai sau" to "250",
        "dhayi sau" to "250",
        "do sau pachas" to "250",
        "do sau" to "200",
        "dedh sau" to "150",
        "deedh sau" to "150",
        "ek sau" to "100",
        "sau" to "100",

        // Compound English numbers
        "five thousand" to "5000",
        "two thousand five hundred" to "2500",
        "twenty five hundred" to "2500",
        "two thousand" to "2000",
        "one thousand two hundred" to "1200",
        "twelve hundred" to "1200",
        "one thousand" to "1000",
        "five hundred" to "500",
        "four hundred" to "400",
        "three hundred" to "300",
        "two hundred fifty" to "250",
        "two hundred and fifty" to "250",
        "two hundred" to "200",
        "one hundred fifty" to "150",
        "one hundred and fifty" to "150",
        "one hundred" to "100"
    )

    private val DIGIT_AMOUNT_REGEX = Regex("(?i)(?:₹|rs\\.?|inr)?\\s*(\\d+(?:\\.\\d{1,2})?)\\s*(?:₹|rs\\.?|rupaye|rupees)?")

    /**
     * Parses a spoken transcript into a structured credit entry.
     * Returns Result.success(ParsedCreditEntry) or Result.failure(Exception).
     */
    fun parse(transcript: String?): Result<ParsedCreditEntry> {
        val raw = transcript?.trim() ?: ""
        if (raw.isBlank()) {
            return Result.failure(IllegalArgumentException("Voice transcript is empty"))
        }

        // 1. Convert spoken number words into digit tokens
        var processedText = raw.lowercase(Locale.ROOT)
        for ((phrase, digitStr) in SPOKEN_NUMBER_REPLACEMENTS) {
            val regex = Regex("\\b$phrase\\b", RegexOption.IGNORE_CASE)
            if (regex.containsMatchIn(processedText)) {
                processedText = regex.replace(processedText, digitStr)
            }
        }

        // 2. Extract numeric amount
        val match = DIGIT_AMOUNT_REGEX.find(processedText)
            ?: return Result.failure(IllegalArgumentException("Could not detect credit amount in transcript: \"$raw\""))

        val amountStr = match.groups[1]?.value
            ?: return Result.failure(IllegalArgumentException("Could not parse credit amount"))

        val amountValue = amountStr.toDoubleOrNull()
            ?: return Result.failure(IllegalArgumentException("Invalid number format: $amountStr"))

        if (amountValue <= 0.0) {
            return Result.failure(IllegalArgumentException("Credit amount must be greater than zero"))
        }

        // Convert safely to integer paise without floating point inaccuracy
        val amountPaise = (amountValue * 100.0 + 0.5).toLong()
        val money = Money.fromPaise(amountPaise)

        // 3. Extract Customer Name and Optional Note
        // Text before the amount match is typically the customer name + prepositions
        val startIndex = match.range.first
        val endIndex = match.range.last + 1

        val beforeAmount = processedText.substring(0, startIndex).trim()
        val afterAmount = processedText.substring(endIndex).trim()

        // Extract Customer Name tokens
        var rawCustomerTokens = beforeAmount
            .split("\\s+".toRegex())
            .map { it.replace("[^a-zA-Z0-9]".toRegex(), "").trim() }
            .filter { it.isNotBlank() }

        // Remove trailing prepositions/fillers like "ko", "ka", "to"
        rawCustomerTokens = rawCustomerTokens.filterNot { it in FILLER_WORDS }

        var customerName = ""
        var note: String? = null

        if (rawCustomerTokens.isNotEmpty()) {
            customerName = rawCustomerTokens.joinToString(" ") { token ->
                token.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
            }
            // Parse after-amount words for optional notes (e.g. "doodh", "ration")
            val afterTokens = afterAmount
                .split("\\s+".toRegex())
                .map { it.replace("[^a-zA-Z0-9]".toRegex(), "").trim() }
                .filter { it.isNotBlank() && it !in ACTION_KEYWORDS }

            if (afterTokens.isNotEmpty()) {
                note = afterTokens.joinToString(" ")
            }
        } else {
            // Case where customer name appeared after amount: "500 credit Ramesh" or "500 Ramesh doodh"
            val afterTokens = afterAmount
                .split("\\s+".toRegex())
                .map { it.replace("[^a-zA-Z0-9]".toRegex(), "").trim() }
                .filter { it.isNotBlank() }

            val nonActionTokens = afterTokens.filterNot { it in ACTION_KEYWORDS }
            if (nonActionTokens.isNotEmpty()) {
                customerName = nonActionTokens.first().replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()
                }
                if (nonActionTokens.size > 1) {
                    note = nonActionTokens.drop(1).joinToString(" ")
                }
            }
        }

        if (customerName.isBlank()) {
            return Result.failure(IllegalArgumentException("Could not detect customer name in transcript: \"$raw\""))
        }

        val normalized = NameNormalizer.normalize(customerName)

        return Result.success(
            ParsedCreditEntry(
                rawTranscript = raw,
                customerName = customerName,
                normalizedCustomerName = normalized,
                amount = money,
                optionalNote = note?.ifBlank { null }
            )
        )
    }
}
