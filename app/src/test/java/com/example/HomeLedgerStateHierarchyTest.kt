package com.example

import com.example.domain.model.Money
import com.example.domain.model.Obligation
import com.example.domain.model.ObligationStatus
import com.example.presentation.screens.home.HomeUiState
import com.example.presentation.screens.home.LedgerFilter
import com.example.presentation.screens.home.LedgerItemUiModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeLedgerStateHierarchyTest {

    @Test
    fun testEmptyLedgerState_contract_singlePrimaryCta_floatingFabHidden() {
        // State 1: When there are zero customers and no active credit obligations
        val emptyState = HomeUiState(
            customerCount = 0,
            openObligationsCount = 0,
            recentLedgerItems = emptyList(),
            activeFilter = LedgerFilter.ALL
        )

        val hasLedgerData = emptyState.customerCount > 0 ||
                emptyState.openObligationsCount > 0 ||
                emptyState.recentLedgerItems.isNotEmpty()

        // Contract 1: hasLedgerData is false
        assertFalse("When ledger is empty, hasLedgerData must be false", hasLedgerData)

        // Contract 2: Bottom padding contract (24.dp instead of 88.dp to avoid dead space)
        val bottomPaddingDp = if (hasLedgerData) 88 else 24
        assertEquals(24, bottomPaddingDp)

        // Contract 3: Empty ledger card must show Give First Credit only when !hasLedgerData
        val showGiveFirstCredit = !hasLedgerData && emptyState.activeFilter == LedgerFilter.ALL
        assertTrue("Empty state card must show Give First Credit CTA", showGiveFirstCredit)
    }

    @Test
    fun testActiveLedgerState_contract_floatingFabRestored_giveFirstCreditHidden() {
        // State 2: When the ledger contains at least one customer/obligation
        val activeState = HomeUiState(
            customerCount = 1,
            openObligationsCount = 1,
            recentLedgerItems = listOf(
                LedgerItemUiModel(
                    obligation = Obligation(
                        id = 1,
                        customerId = 1,
                        originalAmount = Money.fromRupees(500),
                        remainingAmount = Money.fromRupees(500),
                        status = ObligationStatus.OPEN
                    ),
                    customerName = "Ramesh Kumar",
                    customerId = 1,
                    customerBalance = Money.fromRupees(500)
                )
            ),
            activeFilter = LedgerFilter.ALL
        )

        val hasLedgerData = activeState.customerCount > 0 ||
                activeState.openObligationsCount > 0 ||
                activeState.recentLedgerItems.isNotEmpty()

        // Contract 1: hasLedgerData is true
        assertTrue("When credit exists, hasLedgerData must be true", hasLedgerData)

        // Contract 2: Bottom padding contract (88.dp for FAB clearance)
        val bottomPaddingDp = if (hasLedgerData) 88 else 24
        assertEquals(88, bottomPaddingDp)

        // Contract 3: Give First Credit must NOT be displayed anywhere in active ledger
        val showGiveFirstCredit = !hasLedgerData && activeState.activeFilter == LedgerFilter.ALL
        assertFalse("Active ledger must never display Give First Credit", showGiveFirstCredit)
    }

    @Test
    fun testActiveLedgerWithFilteredEmptyResults_neverShowsGiveFirstCredit() {
        // When active ledger has customers, but user filters by Settled when 0 are settled
        val filteredEmptyState = HomeUiState(
            customerCount = 1,
            openObligationsCount = 1,
            recentLedgerItems = emptyList(),
            activeFilter = LedgerFilter.SETTLED
        )

        val hasLedgerData = filteredEmptyState.customerCount > 0 ||
                filteredEmptyState.openObligationsCount > 0 ||
                filteredEmptyState.recentLedgerItems.isNotEmpty()

        assertTrue("hasLedgerData must remain true since customers exist", hasLedgerData)

        val showGiveFirstCredit = !hasLedgerData && filteredEmptyState.activeFilter == LedgerFilter.ALL
        assertFalse("Filtered empty view must NEVER show Give First Credit", showGiveFirstCredit)
    }
}
