package com.example.sarisari.manager.feature.inventory

import com.example.sarisari.manager.domain.model.Product
import com.example.sarisari.manager.domain.model.StockStatus
import com.example.sarisari.manager.testing.FakeProductRepository
import com.example.sarisari.manager.testing.MainDispatcherRule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class InventoryViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun product(id: Long, name: String, stock: Int) = Product(
        id = id, name = name, categoryId = null, baseUnitName = "piece", costCentavos = 100,
        sellingPriceCentavos = 200, stockQuantity = stock, lowStockThreshold = 5,
    )

    private val viewModel = InventoryViewModel(
        FakeProductRepository(listOf(product(1, "Coke", 20), product(2, "Royal", 3), product(3, "Piattos", 0)))
    )

    @Test
    fun listsProductsAndCountsLowAndOutOfStock() = runTest {
        val job = viewModel.uiState.onEach { }.launchIn(CoroutineScope(mainDispatcherRule.testDispatcher))
        val state = viewModel.uiState.first { !it.isLoading }
        assertEquals(3, state.items.size)
        assertEquals(2, state.lowStockCount)
        assertEquals(StockStatus.LOW_STOCK, state.items.first { it.name == "Royal" }.stockStatus)
        job.cancel()
    }

    @Test
    fun lowStockFilterShowsOnlyLowAndOutOfStock() = runTest {
        val job = viewModel.uiState.onEach { }.launchIn(CoroutineScope(mainDispatcherRule.testDispatcher))
        viewModel.onLowStockOnlyChange(true)
        val state = viewModel.uiState.first { it.lowStockOnly && !it.isLoading }
        assertEquals(listOf("Piattos", "Royal"), state.items.map { it.name }.sorted())
        job.cancel()
    }

    @Test
    fun searchFiltersByName() = runTest {
        val job = viewModel.uiState.onEach { }.launchIn(CoroutineScope(mainDispatcherRule.testDispatcher))
        viewModel.onQueryChange("coke")
        val state = viewModel.uiState.first { it.query == "coke" && it.items.map { i -> i.name } == listOf("Coke") }
        assertEquals(1, state.items.size)
        job.cancel()
    }
}
