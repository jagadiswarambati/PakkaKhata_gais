package com.example.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.presentation.screens.evidence.PaymentEvidenceCaptureScreen
import com.example.presentation.screens.home.HomeScreen
import com.example.presentation.screens.reconciliation.MatchReviewScreen
import com.example.presentation.screens.reconciliation.SettlementResultScreen

@Composable
fun PakkaKhataNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToPaymentCapture = {
                    navController.navigate(Screen.PaymentCapture.route)
                },
                onNavigateToMatchReview = { evidenceId ->
                    navController.navigate(Screen.MatchReview.createRoute(evidenceId))
                }
            )
        }
        composable(Screen.PaymentCapture.route) {
            PaymentEvidenceCaptureScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToReconcile = { evidenceId ->
                    navController.navigate(Screen.MatchReview.createRoute(evidenceId))
                }
            )
        }
        composable(
            route = Screen.MatchReview.route,
            arguments = listOf(navArgument("evidenceId") { type = NavType.LongType })
        ) { backStackEntry ->
            val evidenceId = backStackEntry.arguments?.getLong("evidenceId") ?: 0L
            MatchReviewScreen(
                evidenceId = evidenceId,
                onNavigateToSettlementResult = { reconciliationId ->
                    navController.navigate(Screen.SettlementResult.createRoute(reconciliationId)) {
                        popUpTo(Screen.Home.route) { inclusive = false }
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        composable(
            route = Screen.SettlementResult.route,
            arguments = listOf(navArgument("reconciliationId") { type = NavType.LongType })
        ) { backStackEntry ->
            val reconciliationId = backStackEntry.arguments?.getLong("reconciliationId") ?: 0L
            SettlementResultScreen(
                reconciliationId = reconciliationId,
                onDone = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }
    }
}
