package com.example.domain.reconciliation

import com.example.domain.model.Customer
import com.example.domain.model.Obligation
import com.example.domain.model.PaymentEvidence
import com.example.domain.model.SettlementOutcome

/**
 * Proposed match candidate connecting payment evidence to a customer and an open credit obligation.
 * Fully explainable with decomposed scores and qualitative match reasons.
 */
data class MatchCandidate(
    val customer: Customer,
    val obligation: Obligation,
    val overallScore: Float,
    val nameScore: Float,
    val amountScore: Float,
    val temporalScore: Float,
    val matchDecision: MatchDecision,
    val settlementPlan: SettlementPlan,
    val matchReasons: List<String>,

    // Backward-compatibility properties for Phase 1 code
    val customerName: String = customer.name,
    val confidence: Float = overallScore,
    val proposedOutcome: SettlementOutcome = settlementPlan.outcome,
    val settledAmountPaise: Long = settlementPlan.settledAmount.paise
)

/**
 * Complete evaluation result produced by the reconciliation engine.
 */
data class ReconciliationEvaluation(
    val evidence: PaymentEvidence,
    val bestMatch: MatchCandidate?,
    val decision: MatchDecision,
    val allCandidates: List<MatchCandidate>,
    val isDuplicate: Boolean = false,
    val duplicateWarning: String? = null,
    val summaryExplanation: String
)
