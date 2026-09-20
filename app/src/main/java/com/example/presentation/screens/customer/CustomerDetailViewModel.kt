package com.example.presentation.screens.customer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.PakkaKhataDatabase
import com.example.data.repository.LedgerRepository
import com.example.data.repository.LedgerRepositoryImpl
import com.example.domain.model.Customer
import com.example.domain.model.Money
import com.example.domain.model.Obligation
import com.example.domain.model.PaymentEvidence
import com.example.domain.model.Reconciliation
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class LinkedSettlementUiModel(
    val reconciliation: Reconciliation,
    val evidence: PaymentEvidence?
)

data class ObligationDetailUiModel(
    val obligation: Obligation,
    val settlements: List<LinkedSettlementUiModel>
)

data class CustomerDetailUiState(
    val isLoading: Boolean = true,
    val customer: Customer? = null,
    val totalCredit: Money = Money.ZERO,
    val totalReceived: Money = Money.ZERO,
    val currentBalance: Money = Money.ZERO,
    val obligations: List<ObligationDetailUiModel> = emptyList(),
    val paymentHistory: List<LinkedSettlementUiModel> = emptyList(),
    val errorMessage: String? = null
)

class CustomerDetailViewModel(
    application: Application,
    private val customerId: Long,
    private val ledgerRepository: LedgerRepository = LedgerRepositoryImpl(
        PakkaKhataDatabase.getDatabase(application)
    )
) : AndroidViewModel(application) {

    val uiState: StateFlow<CustomerDetailUiState> = combine(
        ledgerRepository.customerRepo.getCustomerById(customerId),
        ledgerRepository.obligationRepo.getObligationsByCustomer(customerId),
        ledgerRepository.reconciliationRepo.getReconciliationsByCustomerId(customerId),
        ledgerRepository.evidenceRepo.getAllEvidence()
    ) { customer, obligations, customerReconciliations, allEvidence ->
        if (customer == null) {
            return@combine CustomerDetailUiState(
                isLoading = false,
                customer = null,
                errorMessage = "Customer not found"
            )
        }

        val evidenceMap = allEvidence.associateBy { it.id }

        // Map settlements per obligation
        val reconciliationsByObligation = customerReconciliations.groupBy { it.obligationId }

        val obligationUiModels = obligations.map { ob ->
            val settlements = (reconciliationsByObligation[ob.id] ?: emptyList()).map { rec ->
                LinkedSettlementUiModel(
                    reconciliation = rec,
                    evidence = evidenceMap[rec.evidenceId]
                )
            }
            ObligationDetailUiModel(
                obligation = ob,
                settlements = settlements
            )
        }

        val totalCreditPaise = obligations.sumOf { it.originalAmount.paise }
        val totalReceivedPaise = customerReconciliations.sumOf { it.settledAmount.paise }
        val currentBalancePaise = customer.currentBalance.paise

        val paymentHistory = customerReconciliations.map { rec ->
            LinkedSettlementUiModel(
                reconciliation = rec,
                evidence = evidenceMap[rec.evidenceId]
            )
        }

        CustomerDetailUiState(
            isLoading = false,
            customer = customer,
            totalCredit = Money.fromPaise(totalCreditPaise),
            totalReceived = Money.fromPaise(totalReceivedPaise),
            currentBalance = Money.fromPaise(currentBalancePaise),
            obligations = obligationUiModels,
            paymentHistory = paymentHistory
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = CustomerDetailUiState(isLoading = true)
    )

    companion object {
        fun provideFactory(
            application: Application,
            customerId: Long
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return CustomerDetailViewModel(application, customerId) as T
            }
        }
    }
}
