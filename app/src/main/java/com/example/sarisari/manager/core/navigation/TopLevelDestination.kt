package com.example.sarisari.manager.core.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.example.sarisari.manager.R

data class TopLevelDestination(
    val route: Any,
    @param:StringRes val labelRes: Int,
    @param:DrawableRes val iconRes: Int,
)

val topLevelDestinations: List<TopLevelDestination> = listOf(
    TopLevelDestination(DashboardRoute, R.string.destination_dashboard, R.drawable.ic_nav_dashboard),
    TopLevelDestination(CashierRoute, R.string.destination_cashier, R.drawable.ic_nav_cashier),
    TopLevelDestination(ProductsRoute, R.string.destination_products, R.drawable.ic_nav_products),
    TopLevelDestination(InventoryRoute, R.string.destination_inventory, R.drawable.ic_nav_inventory),
    TopLevelDestination(ReportsRoute, R.string.destination_reports, R.drawable.ic_nav_reports),
)
