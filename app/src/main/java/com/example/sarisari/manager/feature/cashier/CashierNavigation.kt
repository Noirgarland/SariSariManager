package com.example.sarisari.manager.feature.cashier

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.sarisari.manager.core.navigation.CashierRoute

fun NavGraphBuilder.cashierScreen() {
    composable<CashierRoute> { CashierScreen() }
}
