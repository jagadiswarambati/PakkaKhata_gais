package com.example.presentation.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home_ledger")
    data object PaymentCapture : Screen("payment_capture")
    data object MatchReview : Screen("match_review/{evidenceId}") {
        fun createRoute(evidenceId: Long): String = "match_review/$evidenceId"
    }
    data object SettlementResult : Screen("settlement_result/{reconciliationId}") {
        fun createRoute(reconciliationId: Long): String = "settlement_result/$reconciliationId"
    }
    data object CustomerDetail : Screen("customer_detail/{customerId}") {
        fun createRoute(customerId: Long): String = "customer_detail/$customerId"
    }
}
