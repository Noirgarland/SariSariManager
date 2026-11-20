package com.example.sarisari.manager.feature.inventory

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.sarisari.manager.core.navigation.InventoryRoute

fun NavGraphBuilder.inventoryScreen() {
    composable<InventoryRoute> { InventoryScreen() }
}
