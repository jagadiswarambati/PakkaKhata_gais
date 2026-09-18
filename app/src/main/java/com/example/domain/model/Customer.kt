package com.example.domain.model

import com.example.domain.reconciliation.NameNormalizer

/**
 * Domain model representing a customer in the shopkeeper's ledger.
 */
data class Customer(
    val id: Long = 0,
    val name: String,
    val normalizedName: String = NameNormalizer.normalize(name),
    val phoneNumber: String? = null,
    val currentBalance: Money = Money.ZERO,
    val updatedAt: Long = System.currentTimeMillis()
)
