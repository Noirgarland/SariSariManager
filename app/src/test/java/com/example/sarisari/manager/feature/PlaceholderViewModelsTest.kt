package com.example.sarisari.manager.feature

import com.example.sarisari.manager.R
import com.example.sarisari.manager.feature.cashier.CashierUiState
import com.example.sarisari.manager.feature.cashier.CashierViewModel
import com.example.sarisari.manager.feature.dashboard.DashboardUiState
import com.example.sarisari.manager.feature.dashboard.DashboardViewModel
import com.example.sarisari.manager.feature.inventory.InventoryUiState
import com.example.sarisari.manager.feature.inventory.InventoryViewModel
import com.example.sarisari.manager.feature.products.ProductsUiState
import com.example.sarisari.manager.feature.products.ProductsViewModel
import com.example.sarisari.manager.feature.reports.ReportsUiState
import com.example.sarisari.manager.feature.reports.ReportsViewModel
import com.example.sarisari.manager.testing.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Validates: Requirements 8.9, 9.2 */
class PlaceholderViewModelsTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun dashboardInitialState() {
        val state = DashboardViewModel().uiState.value
        assertEquals(DashboardUiState(), state)
        assertEquals(R.string.destination_dashboard, state.titleRes)
    }

    @Test
    fun cashierInitialState() {
        val state = CashierViewModel().uiState.value
        assertEquals(CashierUiState(), state)
        assertEquals(R.string.destination_cashier, state.titleRes)
    }

    @Test
    fun productsInitialState() {
        val state = ProductsViewModel().uiState.value
        assertEquals(ProductsUiState(), state)
        assertEquals(R.string.destination_products, state.titleRes)
    }

    @Test
    fun inventoryInitialState() {
        val state = InventoryViewModel().uiState.value
        assertEquals(InventoryUiState(), state)
        assertEquals(R.string.destination_inventory, state.titleRes)
    }

    @Test
    fun reportsInitialState() {
        val state = ReportsViewModel().uiState.value
        assertEquals(ReportsUiState(), state)
        assertEquals(R.string.destination_reports, state.titleRes)
    }
}
