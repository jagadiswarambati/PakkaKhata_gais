package com.example.domain.reconciliation

import com.example.domain.model.PaymentEvidence

data class DuplicateCheckResult(
    val isDuplicate: Boolean,
    val existingEvidenceId: Long? = null,
    val reason: String? = null
)

/**
 * Domain-level duplicate protection mechanism for payment evidence.
 * Prevents double allocation of the same bank transfer / UPI reference.
 */
object DuplicateDetector {

    /**
     * Checks if the incoming evidence is a duplicate of any previously processed evidence.
     */
    fun checkForDuplicate(
        incoming: PaymentEvidence,
        existingEvidences: List<PaymentEvidence>
    ): DuplicateCheckResult {
        // 1. Check by Bank Reference / UTR Number
        val incomingUtr = incoming.utrNumber?.trim()
        if (!incomingUtr.isNullOrBlank()) {
            val matchedByUtr = existingEvidences.firstOrNull { existing ->
                existing.id != incoming.id &&
                        !existing.utrNumber.isNullOrBlank() &&
                        existing.utrNumber.trim().equals(incomingUtr, ignoreCase = true)
            }
            if (matchedByUtr != null) {
                return DuplicateCheckResult(
                    isDuplicate = true,
                    existingEvidenceId = matchedByUtr.id,
                    reason = "Duplicate UTR/Reference number: $incomingUtr already processed"
                )
            }
        }

        // 2. Fallback check for evidence without UTR: identical amount + timestamp + sender
        if (incoming.extractedAmount.paise > 0L) {
            val matchedByFingerprint = existingEvidences.firstOrNull { existing ->
                existing.id != incoming.id &&
                        existing.extractedAmount == incoming.extractedAmount &&
                        existing.timestamp == incoming.timestamp &&
                        !existing.extractedSenderName.isNullOrBlank() &&
                        existing.extractedSenderName.equals(incoming.extractedSenderName, ignoreCase = true)
            }
            if (matchedByFingerprint != null) {
                return DuplicateCheckResult(
                    isDuplicate = true,
                    existingEvidenceId = matchedByFingerprint.id,
                    reason = "Identical payment fingerprint (amount, sender, timestamp) already exists"
                )
            }
        }

        return DuplicateCheckResult(isDuplicate = false)
    }
}
