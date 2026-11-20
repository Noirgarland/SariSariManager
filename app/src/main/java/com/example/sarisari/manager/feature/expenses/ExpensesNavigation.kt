package com.example.sarisari.manager.feature.expenses

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.sarisari.manager.core.navigation.ExpensesRoute

fun NavGraphBuilder.expensesScreen(onBack: () -> Unit) {
    composable<ExpensesRoute> { ExpensesScreen(onBack = onBack) }
}
