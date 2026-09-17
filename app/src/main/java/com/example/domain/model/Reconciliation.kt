package com.example.domain.model

/**
 * Domain model representing the critical link between payment evidence and original credit obligation.
 */
data class Reconciliation(
    val id: Long = 0,
    val obligationId: Long,
    val evidenceId: Long,
    val settledAmount: Money,
    val matchConfidence: Float,
    val matchType: SettlementOutcome,
    val reconciledAt: Long = System.currentTimeMillis()
)
