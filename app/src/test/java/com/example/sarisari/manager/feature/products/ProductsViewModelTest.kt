package com.example.sarisari.manager.feature.products

import com.example.sarisari.manager.R
import com.example.sarisari.manager.domain.model.Category
import com.example.sarisari.manager.domain.model.Product
import com.example.sarisari.manager.domain.model.StockStatus
import com.example.sarisari.manager.testing.FakeCategoryRepository
import com.example.sarisari.manager.testing.FakeProductRepository
import com.example.sarisari.manager.testing.MainDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class ProductsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun product(id: Long, name: String, categoryId: Long? = null, stock: Int = 10, archived: Boolean = false) =
        Product(
            id = id, name = name, categoryId = categoryId, baseUnitName = "piece", costCentavos = 1000,
            sellingPriceCentavos = 1500, stockQuantity = stock, lowStockThreshold = 5, isArchived = archived,
        )

    private val drinks = Category(1, "Drinks")
    private val products = FakeProductRepository(
        listOf(
            product(1, "Coke", categoryId = 1, stock = 3),
            product(2, "Piattos", stock = 0),
            product(3, "Old item", archived = true),
        )
    )
    private val categories = FakeCategoryRepository(listOf(drinks))
    private val viewModel = ProductsViewModel(products, categories)

    /** Keeps the WhileSubscribed state flow active while the block runs. */
    private suspend fun <T> collecting(block: suspend () -> T): T {
        val job = viewModel.uiState.onEach { }.launchIn(kotlinx.coroutines.CoroutineScope(mainDispatcherRule.testDispatcher))
        try {
            return block()
        } finally {
            job.cancel()
        }
    }

    @Test
    fun showsActiveProductsWithCategoryNamesAndStockStatus() = runTest {
        collecting {
            val state = viewModel.uiState.first { !it.isLoading }
            assertEquals(listOf("Coke", "Piattos"), state.products.map { it.name }.sorted())
            assertEquals("Drinks", state.products.first { it.name == "Coke" }.categoryName)
            assertNull(state.products.first { it.name == "Piattos" }.categoryName)
            assertEquals(StockStatus.LOW_STOCK, state.products.first { it.name == "Coke" }.stockStatus)
            assertEquals(StockStatus.OUT_OF_STOCK, state.products.first { it.name == "Piattos" }.stockStatus)
        }
    }

    @Test
    fun searchCategoryAndArchivedFiltersApply() = runTest {
        collecting {
            viewModel.onQueryChange("pia")
            assertEquals(listOf("Piattos"), viewModel.uiState.first { it.query == "pia" && it.products.size == 1 }.products.map { it.name })
            viewModel.onQueryChange("")
            viewModel.onCategorySelected(1)
            assertEquals(
                listOf("Coke"),
                viewModel.uiState.first { it.selectedCategoryId == 1L && it.products.map { p -> p.name } == listOf("Coke") }.products.map { it.name },
            )
            viewModel.onCategorySelected(null)
            viewModel.onShowArchivedChange(true)
            assertEquals(3, viewModel.uiState.first { it.showArchived && it.products.size == 3 }.products.size)
        }
    }

    @Test
    fun addCategoryRejectsBlankAndDuplicateNames() = runTest {
        collecting {
            viewModel.addCategory("   ")
            assertEquals(R.string.category_error_blank, viewModel.uiState.first { it.categoryErrorRes != null }.categoryErrorRes)
            viewModel.addCategory("drinks")
            assertEquals(R.string.category_error_exists, viewModel.uiState.first { it.categoryErrorRes == R.string.category_error_exists }.categoryErrorRes)
            viewModel.addCategory("Snacks")
            val state = viewModel.uiState.first { it.categories.size == 2 }
            assertNull(state.categoryErrorRes)
        }
    }

    @Test
    fun renameCategoryUpdatesName() = runTest {
        collecting {
            viewModel.renameCategory(1, "Beverages")
            assertEquals("Beverages", viewModel.uiState.first { it.categories.firstOrNull()?.name == "Beverages" }.categories.first().name)
        }
    }
}
