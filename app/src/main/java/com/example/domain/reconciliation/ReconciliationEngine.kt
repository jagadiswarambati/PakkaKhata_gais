package com.example.domain.reconciliation

import com.example.domain.model.Customer
import com.example.domain.model.Obligation
import com.example.domain.model.PaymentEvidence

/**
 * Pure Kotlin reconciliation and matching engine interface.
 * Operates strictly on domain models without Android or perception dependencies.
 */
interface ReconciliationEngine {

    /**
     * Complete evaluation of payment evidence against customers and open obligations.
     * Checks for duplicates, scores all valid candidates, and determines match decision.
     */
    fun evaluate(
        evidence: PaymentEvidence,
        customers: List<Customer>,
        openObligations: List<Obligation>,
        existingEvidence: List<PaymentEvidence> = emptyList()
    ): ReconciliationEvaluation

    /**
     * Generates and ranks match candidates for open obligations.
     */
    fun findCandidates(
        evidence: PaymentEvidence,
        customers: List<Customer>,
        openObligations: List<Obligation>
    ): List<MatchCandidate>

    /**
     * Backward-compatible match candidate lookup for Phase 1 contracts.
     */
    suspend fun findMatches(
        evidence: PaymentEvidence,
        openObligations: List<Obligation>
    ): List<MatchCandidate>
}
