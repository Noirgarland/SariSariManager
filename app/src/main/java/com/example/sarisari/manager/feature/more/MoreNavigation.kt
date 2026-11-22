package com.example.sarisari.manager.feature.more

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.sarisari.manager.core.navigation.MoreRoute

fun NavGraphBuilder.moreScreen(
    onOpenUtang: () -> Unit,
    onOpenInventory: () -> Unit,
    onOpenSalesHistory: () -> Unit,
    onOpenExpenses: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    composable<MoreRoute> {
        MoreScreen(
            onOpenUtang = onOpenUtang,
            onOpenInventory = onOpenInventory,
            onOpenSalesHistory = onOpenSalesHistory,
            onOpenExpenses = onOpenExpenses,
            onOpenSettings = onOpenSettings,
        )
    }
}
