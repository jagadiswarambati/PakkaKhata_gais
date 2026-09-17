package com.example.domain.model

/**
 * Domain model representing a customer in the shopkeeper's ledger.
 */
data class Customer(
    val id: Long = 0,
    val name: String,
    val normalizedName: String,
    val phoneNumber: String? = null,
    val currentBalance: Money = Money.ZERO,
    val updatedAt: Long = System.currentTimeMillis()
)
