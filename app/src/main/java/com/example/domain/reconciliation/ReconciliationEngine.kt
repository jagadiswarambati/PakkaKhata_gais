package com.example.domain.reconciliation

import com.example.domain.model.Obligation
import com.example.domain.model.PaymentEvidence
import com.example.domain.model.SettlementOutcome

/**
 * Proposed match between payment evidence and an open obligation.
 */
data class MatchCandidate(
    val obligation: Obligation,
    val customerName: String,
    val confidence: Float,
    val proposedOutcome: SettlementOutcome,
    val settledAmountPaise: Long,
    val matchReasons: List<String>
)

/**
 * Intelligent matching engine interface.
 * Connects later payment evidence with earlier open credit obligations.
 * Detailed matching heuristics and algorithms to be implemented in Phase 2.
 */
interface ReconciliationEngine {
    /**
     * Finds and scores matching open obligations for the given payment evidence.
     */
    suspend fun findMatches(
        evidence: PaymentEvidence,
        openObligations: List<Obligation>
    ): List<MatchCandidate>
}
