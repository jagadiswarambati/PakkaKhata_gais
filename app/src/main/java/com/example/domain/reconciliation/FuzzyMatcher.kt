package com.example.domain.reconciliation

import kotlin.math.max
import kotlin.math.min

/**
 * Deterministic fuzzy string matcher for customer name resolution.
 * Provides normalized scores between 0.0f (no similarity) and 1.0f (identical match).
 */
object FuzzyMatcher {

    /**
     * Calculates the overall similarity between two names (e.g., sender vs customer).
     * Returns a float between 0.0f and 1.0f.
     */
    fun calculateNameSimilarity(senderName: String?, customerName: String?): Float {
        val normSender = NameNormalizer.normalize(senderName)
        val normCustomer = NameNormalizer.normalize(customerName)

        if (normSender.isBlank() || normCustomer.isBlank()) {
            return 0.0f
        }

        // 1. Exact normalized match
        if (normSender == normCustomer) {
            return 1.0f
        }

        val senderTokens = NameNormalizer.tokenize(normSender)
        val customerTokens = NameNormalizer.tokenize(normCustomer)

        // 2. Token Containment & Sub-token overlap
        val tokenContainmentScore = evaluateTokenContainment(senderTokens, customerTokens)
        if (tokenContainmentScore >= 0.85f) {
            return tokenContainmentScore
        }

        // 3. String-level Levenshtein similarity
        val levenshteinScore = calculateLevenshteinSimilarity(normSender, normCustomer)

        // 4. Token-level best match alignment
        val tokenAlignmentScore = calculateTokenAlignment(senderTokens, customerTokens)

        // Return the highest confidence indicator
        return max(tokenContainmentScore, max(levenshteinScore, tokenAlignmentScore))
    }

    /**
     * Checks if tokens of one name are contained within the other
     * (e.g. "Ramesh" in "Ramesh Kumar", or "Ramesh Kumar" with "Ramesh K").
     */
    private fun evaluateTokenContainment(
        senderTokens: List<String>,
        customerTokens: List<String>
    ): Float {
        if (senderTokens.isEmpty() || customerTokens.isEmpty()) return 0.0f

        val (shorter, longer) = if (senderTokens.size <= customerTokens.size) {
            senderTokens to customerTokens
        } else {
            customerTokens to senderTokens
        }

        // Check if all tokens of shorter are fully in longer
        val allMatch = shorter.all { shortToken ->
            longer.any { longToken ->
                longToken == shortToken ||
                        (shortToken.length >= 3 && longToken.startsWith(shortToken)) ||
                        (shortToken.length == 1 && longToken.startsWith(shortToken))
            }
        }

        if (allMatch) {
            return if (shorter.size == longer.size) {
                1.0f
            } else {
                // e.g. "Ramesh" vs "Ramesh Kumar"
                0.90f
            }
        }

        // Partial token overlap ratio
        var matchedCount = 0
        for (st in shorter) {
            if (longer.any { lt -> lt == st || (st.length >= 3 && lt.startsWith(st)) }) {
                matchedCount++
            }
        }

        return if (matchedCount > 0) {
            (matchedCount.toFloat() / max(shorter.size, longer.size)) * 0.80f
        } else {
            0.0f
        }
    }

    /**
     * Normalized Levenshtein similarity:
     * 1.0 - (distance / max(len1, len2))
     */
    fun calculateLevenshteinSimilarity(s1: String, s2: String): Float {
        if (s1 == s2) return 1.0f
        val maxLen = max(s1.length, s2.length)
        if (maxLen == 0) return 1.0f

        val dist = levenshteinDistance(s1, s2)
        val similarity = 1.0f - (dist.toFloat() / maxLen.toFloat())
        return max(0.0f, min(1.0f, similarity))
    }

    /**
     * Standard Dynamic Programming Levenshtein distance computation.
     */
    fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }

        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j

        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = min(
                    min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + cost
                )
            }
        }
        return dp[s1.length][s2.length]
    }

    /**
     * Measures how closely individual tokens align even if word order varies.
     */
    private fun calculateTokenAlignment(
        tokens1: List<String>,
        tokens2: List<String>
    ): Float {
        if (tokens1.isEmpty() || tokens2.isEmpty()) return 0.0f

        var totalBestScore = 0.0f
        for (t1 in tokens1) {
            var bestForT1 = 0.0f
            for (t2 in tokens2) {
                val score = calculateLevenshteinSimilarity(t1, t2)
                if (score > bestForT1) {
                    bestForT1 = score
                }
            }
            totalBestScore += bestForT1
        }

        return totalBestScore / max(tokens1.size, tokens2.size)
    }
}
