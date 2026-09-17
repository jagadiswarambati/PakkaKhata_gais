package com.example.domain.model

/**
 * Domain model representing payment evidence captured via camera/phone (UPI screenshot, cash slip, etc.).
 */
data class PaymentEvidence(
    val id: Long = 0,
    val imagePath: String,
    val extractedAmount: Money,
    val extractedSenderName: String? = null,
    val utrNumber: String? = null,
    val ocrRawText: String = "",
    val paymentApp: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
