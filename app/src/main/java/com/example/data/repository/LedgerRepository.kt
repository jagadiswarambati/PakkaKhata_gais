package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.PakkaKhataDatabase
import com.example.data.local.entities.CustomerEntity
import com.example.data.local.entities.ObligationEntity
import com.example.data.local.entities.PaymentEvidenceEntity
import com.example.data.local.entities.ReconciliationEntity
import com.example.domain.model.Customer
import com.example.domain.model.Money
import com.example.domain.model.Obligation
import com.example.domain.model.ObligationStatus
import com.example.domain.model.PaymentEvidence
import com.example.domain.model.Reconciliation
import com.example.domain.model.SettlementOutcome
import com.example.domain.reconciliation.NameNormalizer
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

    /**
     * Records a new open credit obligation:
     * 1. Resolves customer by normalized name (reuses existing or inserts new).
     * 2. Increments customer outstanding balance by the credit amount.
     * 3. Inserts open credit obligation with original transcript and notes.
     * Everything executes atomically inside a SQLite database transaction.
     */
    suspend fun recordCreditObligation(
        customerName: String,
        amount: Money,
        voiceTranscript: String? = null,
        notes: String? = null
    ): Result<Obligation>

    /**
     * Resets the entire local ledger state for a clean demo demonstration.
     */
    suspend fun resetLedgerForDemo()

    /**
     * Loads the official hackathon demo baseline: Ramesh Kumar ₹500 credit obligation.
     */
    suspend fun loadDemoScenario(): Result<Obligation>
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

            if (obligationEntity.remainingAmountPaise <= 0L || obligationEntity.status == ObligationStatus.FULLY_SETTLED) {
                throw IllegalStateException("Cannot settle an already fully settled obligation")
            }

            // 1. Insert or update PaymentEvidence marking isReconciled = true
            val evidenceWithReconciliation = evidence.copy(isReconciled = true)
            val evidenceEntity = PaymentEvidenceEntity.fromDomain(evidenceWithReconciliation)
            val insertedEvidenceId = if (evidence.id > 0) {
                database.paymentEvidenceDao().updateReconciliationStatus(evidence.id, true)
                evidence.id
            } else {
                database.paymentEvidenceDao().insertEvidence(evidenceEntity)
            }

            // 2. Compute and update Obligation balance & status
            val newRemainingPaise = maxOf(0L, obligationEntity.remainingAmountPaise - settledAmount.paise)
            val newStatus = when {
                outcome == SettlementOutcome.OVERPAID -> ObligationStatus.OVERPAID
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

    override suspend fun recordCreditObligation(
        customerName: String,
        amount: Money,
        voiceTranscript: String?,
        notes: String?
    ): Result<Obligation> = runCatching {
        require(customerName.isNotBlank()) { "Customer name cannot be empty" }
        require(amount.isPositive) { "Credit amount must be greater than zero" }

        database.withTransaction {
            val normalized = NameNormalizer.normalize(customerName)
            val existingCustomer = database.customerDao().getCustomerByNormalizedName(normalized)

            val customerId = if (existingCustomer != null) {
                val updatedBalancePaise = existingCustomer.currentBalancePaise + amount.paise
                database.customerDao().updateCustomerBalance(
                    customerId = existingCustomer.id,
                    balancePaise = updatedBalancePaise,
                    updatedAt = System.currentTimeMillis()
                )
                existingCustomer.id
            } else {
                val newCustomer = CustomerEntity(
                    name = customerName.trim(),
                    normalizedName = normalized,
                    currentBalancePaise = amount.paise,
                    updatedAt = System.currentTimeMillis()
                )
                database.customerDao().insertCustomer(newCustomer)
            }

            val obligationEntity = ObligationEntity(
                customerId = customerId,
                originalAmountPaise = amount.paise,
                remainingAmountPaise = amount.paise,
                voiceTranscript = voiceTranscript,
                notes = notes,
                status = ObligationStatus.OPEN,
                createdAt = System.currentTimeMillis()
            )
            val obligationId = database.obligationDao().insertObligation(obligationEntity)
            obligationEntity.copy(id = obligationId).toDomain()
        }
    }

    override suspend fun resetLedgerForDemo() {
        database.clearAllTables()
    }

    override suspend fun loadDemoScenario(): Result<Obligation> {
        return recordCreditObligation(
            customerName = "Ramesh Kumar",
            amount = Money.fromRupees(500.0),
            voiceTranscript = "Ramesh took 500 rupees credit",
            notes = "Demo Scenario: Grocery items"
        )
    }
}
