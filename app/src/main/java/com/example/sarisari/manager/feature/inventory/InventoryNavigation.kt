package com.example.sarisari.manager.feature.inventory

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.example.sarisari.manager.core.navigation.InventoryRoute
import com.example.sarisari.manager.core.navigation.StockEntryRoute
import com.example.sarisari.manager.core.navigation.StockHistoryRoute

fun NavGraphBuilder.inventoryScreen(
    onBack: () -> Unit,
    onOpenProduct: (Long) -> Unit,
    onOpenHistory: () -> Unit,
) {
    composable<InventoryRoute> {
        InventoryScreen(onBack = onBack, onOpenProduct = onOpenProduct, onOpenHistory = onOpenHistory)
    }
}

fun NavGraphBuilder.stockEntryScreen(onBack: () -> Unit, onOpenHistory: (Long) -> Unit) {
    composable<StockEntryRoute> { entry ->
        val productId = entry.toRoute<StockEntryRoute>().productId
        StockEntryScreen(onBack = onBack, onOpenHistory = { onOpenHistory(productId) })
    }
}

fun NavGraphBuilder.stockHistoryScreen(onBack: () -> Unit) {
    composable<StockHistoryRoute> { StockHistoryScreen(onBack = onBack) }
}
