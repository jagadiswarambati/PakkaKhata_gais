package com.example.domain.reconciliation

import java.util.Locale

/**
 * Deterministic name normalizer for Indian retail counter names.
 * Standardizes customer names and payment sender strings (e.g. from UPI / Bank SMS).
 */
object NameNormalizer {

    private val TITLES = setOf(
        "mr", "mr.", "mrs", "mrs.", "ms", "ms.", "shri", "shree", "smt", "smt.",
        "dr", "dr.", "ji", "bhai", "babu", "sahab", "saheb", "kumar", "singh"
    )

    private val PUNCTUATION_REGEX = Regex("[^a-zA-Z0-9\\s]")
    private val MULTI_SPACE_REGEX = Regex("\\s+")

    /**
     * Cleans and normalizes a name string:
     * - Converts to lowercase
     * - Trims whitespace
     * - Removes special characters and dots (e.g. "Ramesh K." -> "ramesh k")
     * - Normalizes multiple spaces into a single space
     */
    fun normalize(input: String?): String {
        if (input.isNullOrBlank()) return ""
        return input.trim()
            .lowercase(Locale.ROOT)
            .replace(PUNCTUATION_REGEX, " ")
            .replace(MULTI_SPACE_REGEX, " ")
            .trim()
    }

    /**
     * Splits a normalized name into distinct alphanumeric tokens.
     */
    fun tokenize(input: String?): List<String> {
        val normalized = normalize(input)
        if (normalized.isEmpty()) return emptyList()
        return normalized.split(" ").filter { it.isNotBlank() }
    }

    /**
     * Extracts significant name tokens by stripping common honorifics/affixes
     * if more distinctive tokens remain.
     */
    fun extractSignificantTokens(input: String?): List<String> {
        val tokens = tokenize(input)
        if (tokens.size <= 1) return tokens
        val filtered = tokens.filterNot { it in TITLES }
        return filtered.ifEmpty { tokens }
    }
}
