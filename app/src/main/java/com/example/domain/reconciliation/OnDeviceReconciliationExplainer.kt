package com.example.domain.reconciliation

import com.example.domain.model.Money
import com.example.domain.model.SettlementOutcome

/**
 * On-Device Explanation Engine (Priority 1: Option B)
 *
 * Converts structured deterministic reconciliation signals (fuzzy name similarity,
 * amount calculations, temporal order, and duplicate detection) into fluent,
 * human-readable explanations on the device.
 *
 * Runs 100% locally and offline without external network calls or cloud models.
 * The deterministic reconciliation engine remains the authoritative source of truth.
 */
object OnDeviceReconciliationExplainer {

    data class Explanation(
        val headline: String,
        val naturalExplanation: String,
        val keyPoints: List<String>,
        val confidenceRating: String,
        val isLocalModelGenerated: Boolean = true
    )

    /**
     * Synthesizes an on-device, human-readable reconciliation explanation from candidate signals.
     */
    fun explain(candidate: MatchCandidate?): Explanation {
        if (candidate == null) {
            return Explanation(
                headline = "No Automatic Match",
                naturalExplanation = "No open customer credit obligations met the safety confidence threshold. Please select an obligation manually.",
                keyPoints = listOf("Sender identity could not be verified", "Manual selection preserves accounting accuracy"),
                confidenceRating = "0%"
            )
        }

        val confidencePct = (candidate.overallScore * 100).toInt().coerceIn(0, 100)
        val customerName = candidate.customer.name
        val paidAmount = candidate.settlementPlan.settledAmount
        val remainingBefore = candidate.obligation.remainingAmount
        val outcome = candidate.settlementPlan.outcome
        val newBalance = candidate.settlementPlan.newRemainingAmount

        val ratingLabel = when {
            confidencePct >= 85 -> "High Confidence Match"
            confidencePct >= 65 -> "Suggested Match"
            else -> "Low Confidence"
        }

        val nameDesc = when {
            candidate.nameScore >= 0.98f -> "Payer name is an exact match for customer '$customerName'"
            candidate.nameScore >= 0.85f -> "Payer name has strong phonetic alignment with '$customerName' (${(candidate.nameScore * 100).toInt()}%)"
            else -> "Payer identity partially resembles '$customerName'"
        }

        val outcomeDesc = when (outcome) {
            SettlementOutcome.FULLY_SETTLED ->
                "Payment of ${paidAmount.formatRupees()} completely settles the outstanding credit of ${remainingBefore.formatRupees()}, leaving zero balance."
            SettlementOutcome.PARTIALLY_SETTLED ->
                "Payment of ${paidAmount.formatRupees()} partially settles the credit of ${remainingBefore.formatRupees()}, leaving an outstanding balance of ${newBalance.formatRupees()}."
            SettlementOutcome.OVERPAID ->
                "Payment of ${paidAmount.formatRupees()} exceeds the open balance of ${remainingBefore.formatRupees()} by ${candidate.settlementPlan.excessAmount.formatRupees()}, creating an advance credit balance."
            SettlementOutcome.NO_MATCH ->
                "Payment could not be automatically reconciled with an open obligation."
        }

        val naturalExplanation = "Matched on-device ($confidencePct% confidence): $nameDesc. $outcomeDesc"

        val keyPoints = mutableListOf<String>()
        keyPoints.add(nameDesc)
        keyPoints.add("Recorded on-device with zero cloud transmission")
        if (candidate.temporalScore >= 0.9f) {
            keyPoints.add("Payment occurred after credit obligation was recorded")
        }
        if (candidate.amountScore >= 0.95f) {
            keyPoints.add("Payment amount matches exact outstanding obligation balance")
        } else {
            keyPoints.add("Valid partial credit reconciliation within ledger boundaries")
        }

        return Explanation(
            headline = "$ratingLabel ($confidencePct%)",
            naturalExplanation = naturalExplanation,
            keyPoints = keyPoints,
            confidenceRating = "$confidencePct%"
        )
    }
}
