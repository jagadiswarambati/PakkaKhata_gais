package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.domain.model.Money
import com.example.domain.model.Obligation
import com.example.domain.model.ObligationStatus

@Entity(
    tableName = "obligations",
    foreignKeys = [
        ForeignKey(
            entity = CustomerEntity::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["customerId"])]
)
data class ObligationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val customerId: Long,
    val originalAmountPaise: Long,
    val remainingAmountPaise: Long,
    val voiceTranscript: String? = null,
    val notes: String? = null,
    val status: ObligationStatus = ObligationStatus.OPEN,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): Obligation = Obligation(
        id = id,
        customerId = customerId,
        originalAmount = Money.fromPaise(originalAmountPaise),
        remainingAmount = Money.fromPaise(remainingAmountPaise),
        voiceTranscript = voiceTranscript,
        notes = notes,
        status = status,
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(obligation: Obligation): ObligationEntity = ObligationEntity(
            id = obligation.id,
            customerId = obligation.customerId,
            originalAmountPaise = obligation.originalAmount.paise,
            remainingAmountPaise = obligation.remainingAmount.paise,
            voiceTranscript = obligation.voiceTranscript,
            notes = obligation.notes,
            status = obligation.status,
            createdAt = obligation.createdAt
        )
    }
}
