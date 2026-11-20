package com.example.sarisari.manager.feature.cashier

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.sarisari.manager.core.navigation.CashierRoute
import com.example.sarisari.manager.core.navigation.SaleDetailRoute
import com.example.sarisari.manager.core.navigation.SalesHistoryRoute
import com.example.sarisari.manager.feature.cashier.history.SaleDetailScreen
import com.example.sarisari.manager.feature.cashier.history.SalesHistoryScreen

fun NavGraphBuilder.cashierScreen() {
    composable<CashierRoute> { CashierScreen() }
}

fun NavGraphBuilder.salesHistoryScreen(onBack: () -> Unit, onOpenSale: (Long) -> Unit) {
    composable<SalesHistoryRoute> { SalesHistoryScreen(onBack = onBack, onOpenSale = onOpenSale) }
}

fun NavGraphBuilder.saleDetailScreen(onBack: () -> Unit) {
    composable<SaleDetailRoute> { SaleDetailScreen(onBack = onBack) }
}
