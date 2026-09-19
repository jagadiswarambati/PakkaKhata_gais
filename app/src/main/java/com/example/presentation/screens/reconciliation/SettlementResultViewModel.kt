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
import com.example.domain.model.Reconciliation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettlementResultUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val reconciliation: Reconciliation? = null,
    val obligation: Obligation? = null,
    val customer: Customer? = null,
    val evidence: PaymentEvidence? = null
)

class SettlementResultViewModel @JvmOverloads constructor(
    application: Application,
    private val ledgerRepository: LedgerRepository = LedgerRepositoryImpl(
        PakkaKhataDatabase.getDatabase(application)
    )
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(SettlementResultUiState())
    val uiState: StateFlow<SettlementResultUiState> = _uiState.asStateFlow()

    fun load(reconciliationId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val reconciliation = ledgerRepository.reconciliationRepo.getReconciliationByIdDirect(reconciliationId)
            if (reconciliation == null) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Settlement record #$reconciliationId not found"
                    )
                }
                return@launch
            }

            val obligation = ledgerRepository.obligationRepo.getObligationByIdDirect(reconciliation.obligationId)
            val evidence = ledgerRepository.evidenceRepo.getEvidenceByIdDirect(reconciliation.evidenceId)
            val customer = if (obligation != null) {
                ledgerRepository.customerRepo.getCustomerByIdDirect(obligation.customerId)
            } else null

            _uiState.update {
                it.copy(
                    isLoading = false,
                    reconciliation = reconciliation,
                    obligation = obligation,
                    customer = customer,
                    evidence = evidence
                )
            }
        }
    }
}
