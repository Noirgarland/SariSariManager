package com.example.sarisari.manager.core.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.example.sarisari.manager.feature.cashier.cashierScreen
import com.example.sarisari.manager.feature.dashboard.dashboardScreen
import com.example.sarisari.manager.feature.inventory.inventoryScreen
import com.example.sarisari.manager.feature.products.productsScreen
import com.example.sarisari.manager.feature.reports.reportsScreen

@Composable
fun SariSariNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = DashboardRoute,
        modifier = modifier,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None },
    ) {
        dashboardScreen()
        cashierScreen()
        productsScreen()
        inventoryScreen()
        reportsScreen()
    }
}
