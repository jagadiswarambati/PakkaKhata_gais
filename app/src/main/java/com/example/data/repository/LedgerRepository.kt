package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.PakkaKhataDatabase
import com.example.data.local.entities.PaymentEvidenceEntity
import com.example.data.local.entities.ReconciliationEntity
import com.example.domain.model.Customer
import com.example.domain.model.Money
import com.example.domain.model.Obligation
import com.example.domain.model.ObligationStatus
import com.example.domain.model.PaymentEvidence
import com.example.domain.model.Reconciliation
import com.example.domain.model.SettlementOutcome
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Result data from executing an atomic settlement transaction.
 */
data class AtomicSettlementResult(
    val reconciliationId: Long,
    val evidenceId: Long,
    val obligationId: Long,
    val customerId: Long,
    val settledAmount: Money,
    val remainingAmount: Money,
    val outcome: SettlementOutcome
)

/**
 * High-level repository providing consolidated ledger queries and atomic settlement transactions.
 */
interface LedgerRepository {
    val customerRepo: CustomerRepository
    val obligationRepo: ObligationRepository
    val evidenceRepo: PaymentEvidenceRepository
    val reconciliationRepo: ReconciliationRepository

    /**
     * Executes the atomic settlement transaction required when matching payment evidence with an obligation:
     * 1. Inserts PaymentEvidence record
     * 2. Updates Obligation remaining amount and status
     * 3. Inserts Reconciliation relationship
     * 4. Updates Customer overall balance
     *
     * Entire sequence runs inside a single atomic SQLite transaction.
     */
    suspend fun executeAtomicSettlement(
        evidence: PaymentEvidence,
        obligationId: Long,
        settledAmount: Money,
        matchConfidence: Float,
        outcome: SettlementOutcome
    ): Result<AtomicSettlementResult>
}

class LedgerRepositoryImpl(
    private val database: PakkaKhataDatabase,
    override val customerRepo: CustomerRepository = CustomerRepositoryImpl(database.customerDao()),
    override val obligationRepo: ObligationRepository = ObligationRepositoryImpl(database.obligationDao()),
    override val evidenceRepo: PaymentEvidenceRepository = PaymentEvidenceRepositoryImpl(database.paymentEvidenceDao()),
    override val reconciliationRepo: ReconciliationRepository = ReconciliationRepositoryImpl(database.reconciliationDao())
) : LedgerRepository {

    override suspend fun executeAtomicSettlement(
        evidence: PaymentEvidence,
        obligationId: Long,
        settledAmount: Money,
        matchConfidence: Float,
        outcome: SettlementOutcome
    ): Result<AtomicSettlementResult> = runCatching {
        database.withTransaction {
            val obligationEntity = database.obligationDao().getObligationByIdDirect(obligationId)
                ?: throw IllegalArgumentException("Obligation with id $obligationId not found")

            // 1. Insert PaymentEvidence
            val evidenceEntity = PaymentEvidenceEntity.fromDomain(evidence)
            val insertedEvidenceId = database.paymentEvidenceDao().insertEvidence(evidenceEntity)

            // 2. Compute and update Obligation balance & status
            val newRemainingPaise = maxOf(0L, obligationEntity.remainingAmountPaise - settledAmount.paise)
            val newStatus = when {
                newRemainingPaise == 0L && settledAmount.paise > obligationEntity.remainingAmountPaise -> ObligationStatus.OVERPAID
                newRemainingPaise == 0L -> ObligationStatus.FULLY_SETTLED
                else -> ObligationStatus.PARTIALLY_SETTLED
            }
            database.obligationDao().updateObligationSettlement(
                obligationId = obligationId,
                remainingAmountPaise = newRemainingPaise,
                status = newStatus
            )

            // 3. Record Reconciliation Link
            val reconciliation = Reconciliation(
                obligationId = obligationId,
                evidenceId = insertedEvidenceId,
                settledAmount = settledAmount,
                matchConfidence = matchConfidence,
                matchType = outcome,
                reconciledAt = System.currentTimeMillis()
            )
            val reconciliationId = database.reconciliationDao().insertReconciliation(
                ReconciliationEntity.fromDomain(reconciliation)
            )

            // 4. Update Customer Ledger Balance
            val customer = database.customerDao().getCustomerByIdDirect(obligationEntity.customerId)
            if (customer != null) {
                val updatedCustomerBalancePaise = maxOf(0L, customer.currentBalancePaise - settledAmount.paise)
                database.customerDao().updateCustomerBalance(
                    customerId = customer.id,
                    balancePaise = updatedCustomerBalancePaise,
                    updatedAt = System.currentTimeMillis()
                )
            }

            AtomicSettlementResult(
                reconciliationId = reconciliationId,
                evidenceId = insertedEvidenceId,
                obligationId = obligationId,
                customerId = obligationEntity.customerId,
                settledAmount = settledAmount,
                remainingAmount = Money.fromPaise(newRemainingPaise),
                outcome = outcome
            )
        }
    }
}
