package com.example.domain.usecase

import com.example.data.repository.CustomerRepository
import com.example.data.repository.ObligationRepository
import com.example.data.repository.PaymentEvidenceRepository
import com.example.domain.model.Customer
import com.example.domain.model.Obligation
import com.example.domain.model.ObligationStatus
import com.example.domain.model.PaymentEvidence
import com.example.domain.reconciliation.DefaultReconciliationEngine
import com.example.domain.reconciliation.DuplicateDetector
import com.example.domain.reconciliation.MatchCandidate
import com.example.domain.reconciliation.MatchDecision
import com.example.domain.reconciliation.ReconciliationEngine
import com.example.domain.reconciliation.ReconciliationEvaluation
import kotlinx.coroutines.flow.first

data class ObligationWithCustomer(
    val obligation: Obligation,
    val customer: Customer
)

data class ReconciliationReviewData(
    val evidence: PaymentEvidence,
    val evaluation: ReconciliationEvaluation,
    val initialCandidate: MatchCandidate?,
    val openObligationsWithCustomers: List<ObligationWithCustomer>,
    val isDuplicate: Boolean,
    val duplicateWarning: String?
)

class EvaluateReconciliationUseCase(
    private val customerRepository: CustomerRepository,
    private val obligationRepository: ObligationRepository,
    private val evidenceRepository: PaymentEvidenceRepository,
    private val engine: ReconciliationEngine = DefaultReconciliationEngine()
) {

    constructor(repository: com.example.data.repository.LedgerRepository) : this(
        customerRepository = repository.customerRepo,
        obligationRepository = repository.obligationRepo,
        evidenceRepository = repository.evidenceRepo
    )

    suspend fun execute(evidenceId: Long): Result<ReconciliationReviewData> = runCatching {
        val evidence = evidenceRepository.getEvidenceByIdDirect(evidenceId)
            ?: throw IllegalArgumentException("Payment evidence #$evidenceId not found")

        executeWithEvidence(evidence)
    }

    suspend operator fun invoke(evidenceId: Long): Result<ReconciliationReviewData> = execute(evidenceId)

    suspend fun executeWithEvidence(evidence: PaymentEvidence): ReconciliationReviewData {
        val allCustomers = customerRepository.getAllCustomers().first()
        val allObligations = obligationRepository.getAllObligations().first()
        val openObligations = allObligations.filter {
            (it.status == ObligationStatus.OPEN || it.status == ObligationStatus.PARTIALLY_SETTLED) &&
                    it.remainingAmount.isPositive
        }
        val allEvidences = evidenceRepository.getAllEvidence().first()

        val customerMap = allCustomers.associateBy { it.id }

        // Evaluate using deterministic domain engine
        val evaluation = engine.evaluate(
            evidence = evidence,
            customers = allCustomers,
            openObligations = openObligations,
            existingEvidence = allEvidences
        )

        val openWithCustomers = openObligations.mapNotNull { ob ->
            val cust = customerMap[ob.customerId]
            if (cust != null) ObligationWithCustomer(ob, cust) else null
        }

        return ReconciliationReviewData(
            evidence = evidence,
            evaluation = evaluation,
            initialCandidate = evaluation.bestMatch,
            openObligationsWithCustomers = openWithCustomers,
            isDuplicate = evaluation.isDuplicate,
            duplicateWarning = evaluation.duplicateWarning
        )
    }
}
