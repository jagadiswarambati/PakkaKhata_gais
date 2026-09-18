package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.model.Money
import com.example.domain.model.PaymentEvidence

@Entity(tableName = "payment_evidences")
data class PaymentEvidenceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val imagePath: String,
    val extractedAmountPaise: Long,
    val extractedSenderName: String? = null,
    val utrNumber: String? = null,
    val ocrRawText: String = "",
    val paymentApp: String? = null,
    val isReconciled: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toDomain(): PaymentEvidence = PaymentEvidence(
        id = id,
        imagePath = imagePath,
        extractedAmount = Money.fromPaise(extractedAmountPaise),
        extractedSenderName = extractedSenderName,
        utrNumber = utrNumber,
        ocrRawText = ocrRawText,
        paymentApp = paymentApp,
        isReconciled = isReconciled,
        timestamp = timestamp
    )

    companion object {
        fun fromDomain(evidence: PaymentEvidence): PaymentEvidenceEntity = PaymentEvidenceEntity(
            id = evidence.id,
            imagePath = evidence.imagePath,
            extractedAmountPaise = evidence.extractedAmount.paise,
            extractedSenderName = evidence.extractedSenderName,
            utrNumber = evidence.utrNumber,
            ocrRawText = evidence.ocrRawText,
            paymentApp = evidence.paymentApp,
            isReconciled = evidence.isReconciled,
            timestamp = evidence.timestamp
        )
    }
}
