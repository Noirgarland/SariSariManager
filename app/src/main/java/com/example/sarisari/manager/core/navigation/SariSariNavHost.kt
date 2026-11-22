package com.example.sarisari.manager.core.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.example.sarisari.manager.feature.cashier.cashierScreen
import com.example.sarisari.manager.feature.cashier.saleDetailScreen
import com.example.sarisari.manager.feature.cashier.salesHistoryScreen
import com.example.sarisari.manager.feature.dashboard.dashboardScreen
import com.example.sarisari.manager.feature.expenses.expensesScreen
import com.example.sarisari.manager.feature.inventory.inventoryScreen
import com.example.sarisari.manager.feature.inventory.stockEntryScreen
import com.example.sarisari.manager.feature.inventory.stockHistoryScreen
import com.example.sarisari.manager.feature.more.moreScreen
import com.example.sarisari.manager.feature.products.productEditScreen
import com.example.sarisari.manager.feature.products.productsScreen
import com.example.sarisari.manager.feature.reports.reportsScreen
import com.example.sarisari.manager.feature.settings.settingsScreen
import com.example.sarisari.manager.feature.utang.customerLedgerScreen
import com.example.sarisari.manager.feature.utang.utangScreen

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
        dashboardScreen(
            onNewSale = { navController.navigateToTopLevel(CashierRoute) },
            onAddStock = { navController.navigate(InventoryRoute) },
            onAddExpense = { navController.navigate(ExpensesRoute) },
            onSeeReports = { navController.navigateToTopLevel(ReportsRoute) },
            onOpenUtang = { navController.navigate(UtangRoute) },
        )
        expensesScreen(onBack = { navController.popBackStack() })
        cashierScreen()
        salesHistoryScreen(
            onBack = { navController.popBackStack() },
            onOpenSale = { navController.navigate(SaleDetailRoute(it)) },
        )
        saleDetailScreen(onBack = { navController.popBackStack() })
        productsScreen(
            onAddProduct = { navController.navigate(ProductEditRoute()) },
            onEditProduct = { navController.navigate(ProductEditRoute(it)) },
        )
        productEditScreen(onBack = { navController.popBackStack() })
        inventoryScreen(
            onBack = { navController.popBackStack() },
            onOpenProduct = { navController.navigate(StockEntryRoute(it)) },
            onOpenHistory = { navController.navigate(StockHistoryRoute()) },
        )
        stockEntryScreen(
            onBack = { navController.popBackStack() },
            onOpenHistory = { navController.navigate(StockHistoryRoute(it)) },
        )
        stockHistoryScreen(onBack = { navController.popBackStack() })
        reportsScreen()
        settingsScreen(onBack = { navController.popBackStack() })
        utangScreen(
            onBack = { navController.popBackStack() },
            onOpenCustomer = { navController.navigate(CustomerLedgerRoute(it)) },
        )
        customerLedgerScreen(onBack = { navController.popBackStack() })
        moreScreen(
            onOpenUtang = { navController.navigate(UtangRoute) },
            onOpenInventory = { navController.navigate(InventoryRoute) },
            onOpenSalesHistory = { navController.navigate(SalesHistoryRoute) },
            onOpenExpenses = { navController.navigate(ExpensesRoute) },
            onOpenSettings = { navController.navigate(SettingsRoute) },
        )
    }
}
