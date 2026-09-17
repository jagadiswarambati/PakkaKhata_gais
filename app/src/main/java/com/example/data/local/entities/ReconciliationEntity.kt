package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.domain.model.Money
import com.example.domain.model.Reconciliation
import com.example.domain.model.SettlementOutcome

@Entity(
    tableName = "reconciliations",
    foreignKeys = [
        ForeignKey(
            entity = ObligationEntity::class,
            parentColumns = ["id"],
            childColumns = ["obligationId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = PaymentEvidenceEntity::class,
            parentColumns = ["id"],
            childColumns = ["evidenceId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["obligationId"]),
        Index(value = ["evidenceId"])
    ]
)
data class ReconciliationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val obligationId: Long,
    val evidenceId: Long,
    val settledAmountPaise: Long,
    val matchConfidence: Float,
    val matchType: SettlementOutcome,
    val reconciledAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): Reconciliation = Reconciliation(
        id = id,
        obligationId = obligationId,
        evidenceId = evidenceId,
        settledAmount = Money.fromPaise(settledAmountPaise),
        matchConfidence = matchConfidence,
        matchType = matchType,
        reconciledAt = reconciledAt
    )

    companion object {
        fun fromDomain(reconciliation: Reconciliation): ReconciliationEntity = ReconciliationEntity(
            id = reconciliation.id,
            obligationId = reconciliation.obligationId,
            evidenceId = reconciliation.evidenceId,
            settledAmountPaise = reconciliation.settledAmount.paise,
            matchConfidence = reconciliation.matchConfidence,
            matchType = reconciliation.matchType,
            reconciledAt = reconciliation.reconciledAt
        )
    }
}
