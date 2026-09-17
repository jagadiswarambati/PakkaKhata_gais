package com.example.domain.model

/**
 * Domain model representing a credit obligation (debt).
 */
data class Obligation(
    val id: Long = 0,
    val customerId: Long,
    val originalAmount: Money,
    val remainingAmount: Money,
    val voiceTranscript: String? = null,
    val notes: String? = null,
    val status: ObligationStatus = ObligationStatus.OPEN,
    val createdAt: Long = System.currentTimeMillis()
)
