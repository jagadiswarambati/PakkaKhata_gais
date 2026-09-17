package com.example.presentation.screens.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.PakkaKhataDatabase
import com.example.data.repository.LedgerRepository
import com.example.data.repository.LedgerRepositoryImpl
import com.example.domain.model.Money
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class HomeUiState(
    val isDatabaseReady: Boolean = false,
    val customerCount: Int = 0,
    val openObligationsCount: Int = 0,
    val totalOutstanding: Money = Money.ZERO
)

class HomeViewModel(
    application: Application,
    private val ledgerRepository: LedgerRepository = LedgerRepositoryImpl(
        PakkaKhataDatabase.getDatabase(application)
    )
) : AndroidViewModel(application) {

    val uiState: StateFlow<HomeUiState> = combine(
        ledgerRepository.customerRepo.getCustomerCount(),
        ledgerRepository.obligationRepo.getOpenObligationCount(),
        ledgerRepository.obligationRepo.getTotalOutstanding()
    ) { customerCount, openCount, totalOutstanding ->
        HomeUiState(
            isDatabaseReady = true,
            customerCount = customerCount,
            openObligationsCount = openCount,
            totalOutstanding = totalOutstanding
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = HomeUiState(isDatabaseReady = true)
    )
}
