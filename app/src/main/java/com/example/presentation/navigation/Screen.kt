package com.example.presentation.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home_ledger")
}
