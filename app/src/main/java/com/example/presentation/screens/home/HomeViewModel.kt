package com.example.presentation.screens.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.PakkaKhataDatabase
import com.example.data.perception.AndroidSpeechRecognizerProvider
import com.example.data.repository.LedgerRepository
import com.example.data.repository.LedgerRepositoryImpl
import com.example.domain.model.Customer
import com.example.domain.model.Money
import com.example.domain.model.Obligation
import com.example.domain.perception.SpeechProvider
import com.example.domain.perception.VoiceCreditParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class VoiceEntryState {
    IDLE,
    LISTENING,
    PROCESSING,
    READY_FOR_CONFIRMATION,
    SAVING,
    SUCCESS,
    ERROR
}

data class CreditSuccessSummary(
    val customerName: String,
    val amount: Money,
    val totalOutstanding: Money
)

data class VoiceCreditUiState(
    val state: VoiceEntryState = VoiceEntryState.IDLE,
    val rawTranscript: String = "",
    val customerName: String = "",
    val amountRupees: String = "",
    val optionalNote: String = "",
    val errorMessage: String? = null,
    val isEditing: Boolean = false,
    val successSummary: CreditSuccessSummary? = null
)

enum class LedgerFilter {
    ALL,
    ACTIVE_DUES,
    SETTLED
}

data class RecentSettlementUiModel(
    val reconciliationId: Long,
    val customerName: String,
    val customerId: Long,
    val settledAmount: Money,
    val reconciledAt: Long,
    val outcome: com.example.domain.model.SettlementOutcome,
    val paymentApp: String?
)

data class LedgerItemUiModel(
    val obligation: Obligation,
    val customerName: String,
    val customerId: Long,
    val customerBalance: Money
)

data class HomeUiState(
    val isDatabaseReady: Boolean = false,
    val customerCount: Int = 0,
    val customersWithOutstandingCount: Int = 0,
    val openObligationsCount: Int = 0,
    val paymentEvidenceCount: Int = 0,
    val totalOutstanding: Money = Money.ZERO,
    val totalSettledAmount: Money = Money.ZERO,
    val recentlySettledCount: Int = 0,
    val recentSettlements: List<RecentSettlementUiModel> = emptyList(),
    val recentLedgerItems: List<LedgerItemUiModel> = emptyList(),
    val unreconciledEvidences: List<com.example.domain.model.PaymentEvidence> = emptyList(),
    val voiceCreditState: VoiceCreditUiState = VoiceCreditUiState(),
    val activeFilter: LedgerFilter = LedgerFilter.ALL
)

class HomeViewModel @JvmOverloads constructor(
    application: Application,
    private val ledgerRepository: LedgerRepository = LedgerRepositoryImpl(
        PakkaKhataDatabase.getDatabase(application)
    ),
    private val speechProvider: SpeechProvider = AndroidSpeechRecognizerProvider(application)
) : AndroidViewModel(application) {

    private val _voiceCreditState = MutableStateFlow(VoiceCreditUiState())
    private val _ledgerFilter = MutableStateFlow(LedgerFilter.ALL)

    private val baseDataFlow = combine(
        ledgerRepository.customerRepo.getAllCustomers(),
        ledgerRepository.obligationRepo.getAllObligations(),
        ledgerRepository.evidenceRepo.getAllEvidence()
    ) { customers, obligations, evidences ->
        Triple(customers, obligations, evidences)
    }

    val uiState: StateFlow<HomeUiState> = combine(
        baseDataFlow,
        ledgerRepository.reconciliationRepo.getAllReconciliations(),
        _voiceCreditState,
        _ledgerFilter
    ) { (customers, obligations, evidences), reconciliations, voiceState, filter ->
        val customerMap = customers.associateBy { it.id }
        val obligationMap = obligations.associateBy { it.id }
        val evidenceMap = evidences.associateBy { it.id }

        val openObligations = obligations.filter { it.remainingAmount.isPositive }
        val totalPaise = openObligations.sumOf { it.remainingAmount.paise }
        val customersWithDues = customers.count { it.currentBalance.isPositive }

        val totalSettledPaise = reconciliations.sumOf { it.settledAmount.paise }
        val recentSettlements = reconciliations.sortedByDescending { it.reconciledAt }.take(5).map { rec ->
            val ob = obligationMap[rec.obligationId]
            val custName = ob?.let { customerMap[it.customerId]?.name } ?: "Customer"
            val custId = ob?.customerId ?: 0L
            val ev = evidenceMap[rec.evidenceId]
            RecentSettlementUiModel(
                reconciliationId = rec.id,
                customerName = custName,
                customerId = custId,
                settledAmount = rec.settledAmount,
                reconciledAt = rec.reconciledAt,
                outcome = rec.matchType,
                paymentApp = ev?.paymentApp
            )
        }

        val filteredObligations = when (filter) {
            LedgerFilter.ALL -> obligations.sortedByDescending { it.createdAt }
            LedgerFilter.ACTIVE_DUES -> obligations.filter { it.remainingAmount.isPositive }.sortedByDescending { it.createdAt }
            LedgerFilter.SETTLED -> obligations.filter { !it.remainingAmount.isPositive }.sortedByDescending { it.createdAt }
        }

        val ledgerItems = filteredObligations.map { ob ->
            val cust = customerMap[ob.customerId]
            val custName = cust?.name ?: "Customer #${ob.customerId}"
            val custBalance = cust?.currentBalance ?: ob.remainingAmount
            LedgerItemUiModel(
                obligation = ob,
                customerName = custName,
                customerId = ob.customerId,
                customerBalance = custBalance
            )
        }

        val unreconciled = evidences.filter { !it.isReconciled }

        HomeUiState(
            isDatabaseReady = true,
            customerCount = customers.size,
            customersWithOutstandingCount = customersWithDues,
            openObligationsCount = openObligations.size,
            paymentEvidenceCount = evidences.size,
            totalOutstanding = Money.fromPaise(totalPaise),
            totalSettledAmount = Money.fromPaise(totalSettledPaise),
            recentlySettledCount = reconciliations.size,
            recentSettlements = recentSettlements,
            recentLedgerItems = ledgerItems,
            unreconciledEvidences = unreconciled,
            voiceCreditState = voiceState,
            activeFilter = filter
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = HomeUiState(isDatabaseReady = true)
    )

    fun setLedgerFilter(filter: LedgerFilter) {
        _ledgerFilter.value = filter
    }

    fun startVoiceInput() {
        _voiceCreditState.value = VoiceCreditUiState(
            state = VoiceEntryState.LISTENING
        )

        viewModelScope.launch {
            val result = speechProvider.startListening()
            result.onSuccess { transcript ->
                processTranscript(transcript)
            }.onFailure { error ->
                _voiceCreditState.update { current ->
                    current.copy(
                        state = VoiceEntryState.ERROR,
                        errorMessage = error.message ?: "Could not record speech. Please try again or type manually."
                    )
                }
            }
        }
    }

    fun stopVoiceInput() {
        speechProvider.stopListening()
        if (_voiceCreditState.value.state == VoiceEntryState.LISTENING) {
            _voiceCreditState.value = VoiceCreditUiState(state = VoiceEntryState.IDLE)
        }
    }

    fun processTranscript(transcript: String) {
        _voiceCreditState.update {
            it.copy(
                state = VoiceEntryState.PROCESSING,
                rawTranscript = transcript,
                errorMessage = null
            )
        }

        val parseResult = VoiceCreditParser.parse(transcript)
        parseResult.onSuccess { parsed ->
            val rupeesText = (parsed.amount.paise / 100).toString()
            _voiceCreditState.update {
                it.copy(
                    state = VoiceEntryState.READY_FOR_CONFIRMATION,
                    rawTranscript = transcript,
                    customerName = parsed.customerName,
                    amountRupees = rupeesText,
                    optionalNote = parsed.optionalNote ?: "",
                    isEditing = false,
                    errorMessage = null
                )
            }
        }.onFailure { error ->
            _voiceCreditState.update {
                it.copy(
                    state = VoiceEntryState.ERROR,
                    rawTranscript = transcript,
                    errorMessage = error.message ?: "Could not understand credit details from: \"$transcript\""
                )
            }
        }
    }

    fun startManualEntry() {
        speechProvider.stopListening()
        _voiceCreditState.value = VoiceCreditUiState(
            state = VoiceEntryState.READY_FOR_CONFIRMATION,
            rawTranscript = "Manual Entry",
            isEditing = true
        )
    }

    fun toggleEdit(editing: Boolean) {
        _voiceCreditState.update { it.copy(isEditing = editing) }
    }

    fun updateCustomerName(name: String) {
        _voiceCreditState.update { it.copy(customerName = name) }
    }

    fun updateAmount(amount: String) {
        _voiceCreditState.update { it.copy(amountRupees = amount) }
    }

    fun updateNote(note: String) {
        _voiceCreditState.update { it.copy(optionalNote = note) }
    }

    fun onPermissionDenied() {
        _voiceCreditState.update {
            it.copy(
                state = VoiceEntryState.ERROR,
                errorMessage = "Microphone permission is required for voice credit entry. Please grant permission or type manually."
            )
        }
    }

    fun confirmCredit() {
        val currentState = _voiceCreditState.value
        val name = currentState.customerName.trim()
        val amountStr = currentState.amountRupees.trim()

        if (name.isBlank()) {
            _voiceCreditState.update { it.copy(errorMessage = "Please enter customer name") }
            return
        }

        val amountDouble = amountStr.toDoubleOrNull()
        if (amountDouble == null || amountDouble <= 0.0) {
            _voiceCreditState.update { it.copy(errorMessage = "Please enter a valid amount greater than zero") }
            return
        }

        val money = Money.fromPaise((amountDouble * 100.0 + 0.5).toLong())

        _voiceCreditState.update { it.copy(state = VoiceEntryState.SAVING, errorMessage = null) }

        viewModelScope.launch {
            val recordResult = ledgerRepository.recordCreditObligation(
                customerName = name,
                amount = money,
                voiceTranscript = currentState.rawTranscript.ifBlank { null },
                notes = currentState.optionalNote.ifBlank { null }
            )

            recordResult.onSuccess { obligation ->
                // Fetch updated customer balance
                val customer = ledgerRepository.customerRepo.getCustomerByIdDirect(obligation.customerId)
                val totalCustOutstanding = customer?.currentBalance ?: money

                _voiceCreditState.update {
                    it.copy(
                        state = VoiceEntryState.SUCCESS,
                        successSummary = CreditSuccessSummary(
                            customerName = name,
                            amount = money,
                            totalOutstanding = totalCustOutstanding
                        )
                    )
                }
            }.onFailure { error ->
                _voiceCreditState.update {
                    it.copy(
                        state = VoiceEntryState.ERROR,
                        errorMessage = "Failed to save credit: ${error.message}"
                    )
                }
            }
        }
    }

    fun dismissVoiceEntry() {
        speechProvider.stopListening()
        _voiceCreditState.value = VoiceCreditUiState(state = VoiceEntryState.IDLE)
    }

    /**
     * Loads the official hackathon presentation scenario: Ramesh Kumar ₹500 credit.
     */
    fun loadDemoScenario() {
        viewModelScope.launch {
            ledgerRepository.loadDemoScenario()
        }
    }

    /**
     * Resets all ledger data to provide a clean state for live hackathon demos.
     */
    fun resetLedgerForDemo() {
        viewModelScope.launch {
            ledgerRepository.resetLedgerForDemo()
        }
    }

    /**
     * Exports local store ledger summary to laptop via Office Kit / Android Share Sheet.
     */
    fun exportLedgerReport(context: android.content.Context) {
        viewModelScope.launch {
            val state = uiState.value
            val customers = ledgerRepository.customerRepo.getAllCustomers().first()
            val report = com.example.domain.officebridge.OfficeBridgeService.generateLedgerReport(
                customers = customers,
                totalOutstanding = state.totalOutstanding,
                activeObligationsCount = state.openObligationsCount,
                settledCount = state.recentlySettledCount
            )
            com.example.domain.officebridge.OfficeBridgeService.shareLedgerReport(context, report)
        }
    }
}
