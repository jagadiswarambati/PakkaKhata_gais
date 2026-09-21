package com.example.domain.perception

class VoiceCreditParser {

    data class ParsedCredit(
        val customerName: String,
        val amount: Double,
        val isCredit: Boolean
    )

    fun parse(transcript: String): ParsedCredit? {
        val cleanTranscript = transcript.lowercase().trim()
        if (cleanTranscript.isEmpty()) return null

        val words = cleanTranscript.split("\\s+".toRegex())

        var name: String? = null
        var amount: Double? = null
        val isCredit = cleanTranscript.contains("credit") ||
                cleanTranscript.contains("gave") ||
                cleanTranscript.contains("paid")

        val nameWords = mutableListOf<String>()

        for (word in words) {
            val parsedNumber = word.toDoubleOrNull()
            if (parsedNumber != null) {
                amount = parsedNumber
            } else if (word !in listOf("credit", "rupees", "rs", "gave", "me", "for", "debit", "paid", "amount")) {
                nameWords.add(word.replaceFirstChar { it.uppercase() })
            }
        }

        if (nameWords.isNotEmpty()) {
            name = nameWords.joinToString(" ")
        }

        if (name != null && amount != null) {
            return ParsedCredit(
                customerName = name,
                amount = amount,
                isCredit = isCredit
            )
        }

        return null
    }
}