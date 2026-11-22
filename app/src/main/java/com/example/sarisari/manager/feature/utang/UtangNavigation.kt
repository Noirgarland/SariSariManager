package com.example.sarisari.manager.feature.utang

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.sarisari.manager.core.navigation.CustomerLedgerRoute
import com.example.sarisari.manager.core.navigation.UtangRoute

fun NavGraphBuilder.utangScreen(onBack: () -> Unit, onOpenCustomer: (Long) -> Unit) {
    composable<UtangRoute> { UtangScreen(onBack = onBack, onOpenCustomer = onOpenCustomer) }
}

fun NavGraphBuilder.customerLedgerScreen(onBack: () -> Unit) {
    composable<CustomerLedgerRoute> { CustomerLedgerScreen(onBack = onBack) }
}
