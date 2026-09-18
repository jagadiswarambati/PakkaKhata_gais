package com.example.presentation.screens.reconciliation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.PakkaKhataDatabase
import com.example.data.repository.LedgerRepository
import com.example.data.repository.LedgerRepositoryImpl
import com.example.domain.model.Customer
import com.example.domain.model.Obligation
import com.example.domain.model.PaymentEvidence
import com.example.domain.reconciliation.MatchCandidate
import com.example.domain.reconciliation.MatchDecision
import com.example.domain.reconciliation.ReconciliationEvaluation
import com.example.domain.reconciliation.SettlementCalculator
import com.example.domain.reconciliation.SettlementPlan
import com.example.domain.usecase.EvaluateReconciliationUseCase
import com.example.domain.usecase.ExecuteSettlementUseCase
import com.example.domain.usecase.ObligationWithCustomer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MatchReviewUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val evidence: PaymentEvidence? = null,
    val evaluation: ReconciliationEvaluation? = null,
    val activeCandidate: MatchCandidate? = null,
    val matchDecision: MatchDecision = MatchDecision.NO_MATCH,
    val availableObligations: List<ObligationWithCustomer> = emptyList(),
    val isManualSelectionOpen: Boolean = false,
    val searchQuery: String = "",
    val isDuplicate: Boolean = false,
    val duplicateWarning: String? = null,
    val isSettling: Boolean = false,
    val settlementError: String? = null,
    val isRejected: Boolean = false,
    val settlementSuccessReconciliationId: Long? = null
)

class MatchReviewViewModel(
    application: Application,
    private val ledgerRepository: LedgerRepository = LedgerRepositoryImpl(
        PakkaKhataDatabase.getDatabase(application)
    ),
    private val evaluateUseCase: EvaluateReconciliationUseCase = EvaluateReconciliationUseCase(
        customerRepository = ledgerRepository.customerRepo,
        obligationRepository = ledgerRepository.obligationRepo,
        evidenceRepository = ledgerRepository.evidenceRepo
    ),
    private val executeSettlementUseCase: ExecuteSettlementUseCase = ExecuteSettlementUseCase(ledgerRepository)
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(MatchReviewUiState())
    val uiState: StateFlow<MatchReviewUiState> = _uiState.asStateFlow()

    fun load(evidenceId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            evaluateUseCase.execute(evidenceId)
                .onSuccess { data ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            evidence = data.evidence,
                            evaluation = data.evaluation,
                            activeCandidate = data.initialCandidate,
                            matchDecision = data.evaluation.decision,
                            availableObligations = data.openObligationsWithCustomers,
                            isDuplicate = data.isDuplicate,
                            duplicateWarning = data.duplicateWarning
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Failed to evaluate reconciliation"
                        )
                    }
                }
        }
    }

    fun openManualSelection() {
        _uiState.update { it.copy(isManualSelectionOpen = true, searchQuery = "") }
    }

    fun closeManualSelection() {
        _uiState.update { it.copy(isManualSelectionOpen = false) }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun selectObligation(item: ObligationWithCustomer) {
        val evidence = _uiState.value.evidence ?: return
        val plan = SettlementCalculator.calculate(
            remainingAmount = item.obligation.remainingAmount,
            paidAmount = evidence.extractedAmount
        )

        val manualCandidate = MatchCandidate(
            customer = item.customer,
            obligation = item.obligation,
            overallScore = 1.0f,
            nameScore = 1.0f,
            amountScore = 1.0f,
            temporalScore = 1.0f,
            matchDecision = MatchDecision.SUGGESTED_MATCH,
            settlementPlan = plan,
            matchReasons = listOf(
                "Manually selected by shopkeeper",
                "Payment of ${evidence.extractedAmount.formatRupees()} will be allocated to ${item.customer.name}'s credit",
                "New balance after settlement: ${plan.newRemainingAmount.formatRupees()}"
            )
        )

        _uiState.update {
            it.copy(
                activeCandidate = manualCandidate,
                matchDecision = MatchDecision.SUGGESTED_MATCH,
                isManualSelectionOpen = false,
                isRejected = false,
                settlementError = null
            )
        }
    }

    fun rejectMatch() {
        _uiState.update {
            it.copy(
                activeCandidate = null,
                matchDecision = MatchDecision.NO_MATCH,
                isRejected = true,
                settlementError = null
            )
        }
    }

    fun confirmSettlement(onSuccess: (Long) -> Unit) {
        val currentState = _uiState.value
        val candidate = currentState.activeCandidate
        val evidence = currentState.evidence

        if (currentState.isDuplicate) {
            _uiState.update {
                it.copy(settlementError = currentState.duplicateWarning ?: "This payment appears to have already been recorded.")
            }
            return
        }

        if (evidence == null || candidate == null) {
            _uiState.update {
                it.copy(settlementError = "No valid match or obligation selected for settlement")
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSettling = true, settlementError = null) }
            executeSettlementUseCase.execute(
                evidence = evidence,
                obligation = candidate.obligation,
                customer = candidate.customer,
                customSettlementPlan = candidate.settlementPlan,
                confidence = candidate.overallScore
            ).onSuccess { result ->
                _uiState.update {
                    it.copy(
                        isSettling = false,
                        settlementSuccessReconciliationId = result.reconciliationId
                    )
                }
                onSuccess(result.reconciliationId)
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isSettling = false,
                        settlementError = error.message ?: "Settlement failed. Ledger remains unchanged."
                    )
                }
            }
        }
    }
}
