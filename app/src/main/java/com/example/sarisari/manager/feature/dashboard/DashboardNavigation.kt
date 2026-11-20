package com.example.sarisari.manager.feature.dashboard

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.sarisari.manager.core.navigation.DashboardRoute

fun NavGraphBuilder.dashboardScreen(
    onNewSale: () -> Unit,
    onAddStock: () -> Unit,
    onAddExpense: () -> Unit,
    onSeeReports: () -> Unit,
) {
    composable<DashboardRoute> {
        DashboardScreen(
            onNewSale = onNewSale,
            onAddStock = onAddStock,
            onAddExpense = onAddExpense,
            onSeeReports = onSeeReports,
        )
    }
}
