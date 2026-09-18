package com.example.domain.usecase

import com.example.data.repository.AtomicSettlementResult
import com.example.data.repository.LedgerRepository
import com.example.domain.model.Customer
import com.example.domain.model.Obligation
import com.example.domain.model.ObligationStatus
import com.example.domain.model.PaymentEvidence
import com.example.domain.reconciliation.DuplicateDetector
import com.example.domain.reconciliation.SettlementCalculator
import com.example.domain.reconciliation.SettlementPlan
import kotlinx.coroutines.flow.first

class DuplicatePaymentException(message: String) : Exception(message)

class ExecuteSettlementUseCase(
    private val ledgerRepository: LedgerRepository
) {

    suspend fun execute(
        evidence: PaymentEvidence,
        obligation: Obligation,
        customer: Customer,
        customSettlementPlan: SettlementPlan? = null,
        confidence: Float = 1.0f
    ): Result<AtomicSettlementResult> = runCatching {
        // 1. Guard against already-reconciled evidence
        if (evidence.isReconciled) {
            throw DuplicatePaymentException("This payment evidence has already been reconciled.")
        }

        // 2. Guard against duplicate payment allocation
        val allEvidences = ledgerRepository.evidenceRepo.getAllEvidence().first()
        val duplicateCheck = DuplicateDetector.checkForDuplicate(evidence, allEvidences)
        if (duplicateCheck.isDuplicate) {
            throw DuplicatePaymentException(
                duplicateCheck.reason ?: "This payment appears to have already been recorded."
            )
        }

        // 3. Guard against settling an already settled obligation
        val latestObligation = ledgerRepository.obligationRepo.getObligationByIdDirect(obligation.id)
            ?: throw IllegalArgumentException("Obligation #${obligation.id} not found")

        if (latestObligation.status == ObligationStatus.FULLY_SETTLED || latestObligation.remainingAmount.isZero) {
            throw IllegalStateException("Obligation #${obligation.id} is already fully settled.")
        }

        // 4. Calculate deterministic settlement plan using existing SettlementCalculator
        val plan = customSettlementPlan ?: SettlementCalculator.calculate(
            remainingAmount = latestObligation.remainingAmount,
            paidAmount = evidence.extractedAmount
        )

        // 5. Execute atomic settlement in single SQLite transaction
        val result = ledgerRepository.executeAtomicSettlement(
            evidence = evidence,
            obligationId = latestObligation.id,
            settledAmount = plan.settledAmount,
            matchConfidence = confidence,
            outcome = plan.outcome
        )

        result.getOrThrow()
    }

    suspend fun execute(
        evidenceId: Long,
        obligationId: Long,
        confidence: Float = 1.0f
    ): Result<AtomicSettlementResult> = runCatching {
        val evidence = ledgerRepository.evidenceRepo.getEvidenceByIdDirect(evidenceId)
            ?: throw IllegalArgumentException("Payment evidence #$evidenceId not found")
        val obligation = ledgerRepository.obligationRepo.getObligationByIdDirect(obligationId)
            ?: throw IllegalArgumentException("Obligation #$obligationId not found")
        val customer = ledgerRepository.customerRepo.getCustomerByIdDirect(obligation.customerId)
            ?: throw IllegalArgumentException("Customer #${obligation.customerId} not found")

        execute(evidence, obligation, customer, confidence = confidence).getOrThrow()
    }

    suspend operator fun invoke(
        evidence: PaymentEvidence,
        obligation: Obligation,
        customer: Customer,
        customSettlementPlan: SettlementPlan? = null,
        confidence: Float = 1.0f
    ): Result<AtomicSettlementResult> = execute(evidence, obligation, customer, customSettlementPlan, confidence)

    suspend operator fun invoke(
        evidenceId: Long,
        obligationId: Long,
        confidence: Float = 1.0f
    ): Result<AtomicSettlementResult> = execute(evidenceId, obligationId, confidence)
}
