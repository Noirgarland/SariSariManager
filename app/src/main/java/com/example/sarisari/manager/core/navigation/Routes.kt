package com.example.sarisari.manager.core.navigation

import kotlinx.serialization.Serializable

@Serializable
data object DashboardRoute

@Serializable
data object CashierRoute

@Serializable
data object ProductsRoute

@Serializable
data object InventoryRoute

@Serializable
data object ReportsRoute

@Serializable
data object MoreRoute

@Serializable
data class ProductEditRoute(val productId: Long? = null)

@Serializable
data class StockEntryRoute(val productId: Long)

@Serializable
data class StockHistoryRoute(val productId: Long? = null)

@Serializable
data object SalesHistoryRoute

@Serializable
data class SaleDetailRoute(val saleId: Long)

@Serializable
data object ExpensesRoute

@Serializable
data object SettingsRoute

@Serializable
data object UtangRoute

@Serializable
data class CustomerLedgerRoute(val customerId: Long)
