package com.example.sarisari.manager.feature.cashier

import com.example.sarisari.manager.R
import com.example.sarisari.manager.domain.model.Product
import com.example.sarisari.manager.domain.model.ProductUnavailableException
import com.example.sarisari.manager.domain.model.ProductUnit
import com.example.sarisari.manager.domain.usecase.CompleteSaleUseCase
import com.example.sarisari.manager.testing.FakeCategoryRepository
import com.example.sarisari.manager.testing.FakeProductRepository
import com.example.sarisari.manager.testing.FakeSalesRepository
import com.example.sarisari.manager.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CashierViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val coke = Product(
        id = 1, name = "Coke", categoryId = null, baseUnitName = "bottle", costCentavos = 4500,
        sellingPriceCentavos = 6500, stockQuantity = 2, lowStockThreshold = 5,
        units = listOf(ProductUnit("case", 12, 70_000)),
    )
    private val chips = Product(
        id = 2, name = "Piattos", categoryId = null, baseUnitName = "pack", costCentavos = 1200,
        sellingPriceCentavos = 2000, stockQuantity = 10, lowStockThreshold = 5,
    )
    private val sales = FakeSalesRepository()
    private val viewModel by lazy {
        CashierViewModel(
            FakeProductRepository(listOf(coke, chips)),
            FakeCategoryRepository(),
            CompleteSaleUseCase(sales),
        )
    }

    private lateinit var scope: TestScope

    /** Keeps the WhileSubscribed state flow collected for the whole test. */
    private suspend fun TestScope.collecting(block: suspend () -> Unit) {
        scope = this
        viewModel.uiState.launchIn(backgroundScope)
        advanceUntilIdle()
        block()
    }

    /** Lets pending state updates propagate through the combined flows before reading. */
    private val state: CashierUiState
        get() {
            scope.advanceUntilIdle()
            return viewModel.uiState.value
        }

    @Test
    fun tappingProductsBuildsTheCartAndTotals() = runTest {
        collecting {
            viewModel.uiState.first { !it.isLoading }
            viewModel.onProductTapped(chips)
            viewModel.onProductTapped(chips)
            assertEquals(1, state.cart.size)
            assertEquals(2, state.cart.single().quantity)
            assertEquals(4000L, state.totalCentavos)
            assertEquals(2, state.itemCount)
            viewModel.onIncrease("2:pack")
            assertEquals(6000L, state.totalCentavos)
            viewModel.onDecrease("2:pack")
            viewModel.onDecrease("2:pack")
            viewModel.onDecrease("2:pack")
            assertTrue(state.cart.isEmpty())
        }
    }

    @Test
    fun productWithExtraUnitsAsksWhichUnitToSell() = runTest {
        collecting {
            viewModel.onProductTapped(coke)
            assertEquals(coke, state.unitPickerProduct)
            assertTrue(state.cart.isEmpty())
            viewModel.onUnitChosen(coke, "case", 12, 70_000)
            assertNull(state.unitPickerProduct)
            val line = state.cart.single()
            assertEquals("case", line.unitName)
            assertEquals(12, line.unitFactor)
            assertEquals(70_000L, line.unitPriceCentavos)
            assertTrue(line.exceedsStock)
            viewModel.onUnitChosen(coke, "bottle", 1, 6500)
            assertEquals(2, state.cart.size)
        }
    }

    @Test
    fun priceCanBeChangedAtTheCashierButMustBeValid() = runTest {
        collecting {
            viewModel.onProductTapped(chips)
            assertTrue(viewModel.onPriceChanged("2:pack", "18.50"))
            assertEquals(1850L, state.totalCentavos)
            assertFalse(viewModel.onPriceChanged("2:pack", "abc"))
            assertFalse(viewModel.onPriceChanged("2:pack", "0"))
            assertEquals(1850L, state.totalCentavos)
        }
    }

    @Test
    fun paymentNeedsACartAndEnoughCash() = runTest {
        collecting {
            viewModel.onProceedToPayment()
            assertEquals(CashierStage.SELLING, state.stage)
            viewModel.onProductTapped(chips)
            viewModel.onProceedToPayment()
            assertEquals(CashierStage.PAYMENT, state.stage)
            assertFalse(state.canComplete)
            viewModel.onReceivedChange("19.99")
            assertFalse(state.canComplete)
            assertNull(state.changeCentavos)
            viewModel.onReceivedChange("50")
            assertTrue(state.canComplete)
            assertEquals(3000L, state.changeCentavos)
            viewModel.onQuickCash(2000)
            assertEquals(0L, state.changeCentavos)
            viewModel.onBackToCart()
            assertEquals(CashierStage.SELLING, state.stage)
        }
    }

    @Test
    fun completingASaleSendsActualPricesAndResetsTheCart() = runTest {
        collecting {
            viewModel.onProductTapped(chips)
            viewModel.onPriceChanged("2:pack", "18")
            viewModel.onProceedToPayment()
            viewModel.onReceivedChange("20")
            viewModel.onCompleteSale()
            assertEquals(CashierStage.COMPLETE, state.stage)
            assertTrue(state.cart.isEmpty())
            assertEquals(200L, state.completedSale?.changeCentavos)
            val (lines, received) = sales.completed.single()
            assertEquals(1800L, lines.single().unitPriceCentavos)
            assertEquals(2000L, received)
            viewModel.onNewSale()
            assertEquals(CashierStage.SELLING, state.stage)
            assertNull(state.completedSale)
        }
    }

    @Test
    fun unavailableProductKeepsTheCartAndShowsAMessage() = runTest {
        collecting {
            sales.failWith = ProductUnavailableException(2)
            viewModel.onProductTapped(chips)
            viewModel.onProceedToPayment()
            viewModel.onReceivedChange("20")
            viewModel.onCompleteSale()
            assertEquals(CashierStage.PAYMENT, state.stage)
            assertEquals(1, state.cart.size)
            assertEquals(R.string.payment_error_unavailable, state.errorRes)
            assertFalse(state.isSaving)
        }
    }

    @Test
    fun unexpectedFailureKeepsTheCartAndShowsAGenericMessage() = runTest {
        collecting {
            sales.failWith = IllegalStateException("disk full")
            viewModel.onProductTapped(chips)
            viewModel.onProceedToPayment()
            viewModel.onReceivedChange("20")
            viewModel.onCompleteSale()
            assertEquals(R.string.payment_error_failed, state.errorRes)
            assertEquals(1, state.cart.size)
            assertFalse(state.isSaving)
            sales.failWith = null
            viewModel.onCompleteSale()
            assertEquals(CashierStage.COMPLETE, state.stage)
        }
    }
}
