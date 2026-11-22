package com.example.sarisari.manager.domain.usecase

import com.example.sarisari.manager.domain.model.InsufficientCheckoutStockException
import com.example.sarisari.manager.domain.model.QuantityLimits
import com.example.sarisari.manager.domain.model.SaleLineInput
import com.example.sarisari.manager.domain.model.StockShortage
import com.example.sarisari.manager.domain.model.totalCentavos
import com.example.sarisari.manager.testing.FakeSalesRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class CompleteSaleUseCaseTest {

    private val sales = FakeSalesRepository()
    private val completeSale = CompleteSaleUseCase(sales)

    private fun line(qty: Int, price: Long, factor: Int = 1, productId: Long = 1) =
        SaleLineInput(productId, "unit", factor, qty, price)

    private suspend fun rejected(error: CheckoutError, vararg lines: SaleLineInput, received: Long = Long.MAX_VALUE) {
        assertEquals(CheckoutResult.Rejected(error), completeSale(lines.toList(), received))
        assertTrue("nothing may reach the repository", sales.completed.isEmpty())
    }

    @Test
    fun baseQuantityIsComputedInLong() {
        // Int multiplication would wrap to a negative number here.
        assertEquals(10_000L * Int.MAX_VALUE, line(10_000, 1, factor = Int.MAX_VALUE).baseQuantity)
    }

    @Test
    fun subtotalsAndTotalsAreExact() {
        val lines = listOf(line(3, 6500), line(2, 1999), line(1, 1))
        assertEquals(listOf(19_500L, 3_998L, 1L), lines.map { it.lineTotalCentavos })
        assertEquals(23_499L, lines.totalCentavos())
        val max = line(CompleteSaleUseCase.MAX_QUANTITY, CompleteSaleUseCase.MAX_UNIT_PRICE_CENTAVOS)
        assertEquals(1_000_000_000_000L, max.lineTotalCentavos)
    }

    @Test
    fun totalOverflowThrowsInsteadOfWrapping() {
        assertThrows(ArithmeticException::class.java) { line(Int.MAX_VALUE, Long.MAX_VALUE).lineTotalCentavos }
        assertThrows(ArithmeticException::class.java) { listOf(line(1, Long.MAX_VALUE), line(1, 1)).totalCentavos() }
    }

    @Test
    fun quantityBoundaries() = runTest {
        rejected(CheckoutError.INVALID_QUANTITY, line(0, 100))
        rejected(CheckoutError.INVALID_QUANTITY, line(-1, 100))
        rejected(CheckoutError.INVALID_QUANTITY, line(CompleteSaleUseCase.MAX_QUANTITY + 1, 100))
        rejected(CheckoutError.INVALID_QUANTITY, line(1, 100, factor = 0))
        rejected(CheckoutError.INVALID_QUANTITY, line(1, 100, factor = QuantityLimits.MAX_UNIT_FACTOR + 1))
        // 10,000 x 101 = 1,010,000 base units, above the per-sale limit of 1,000,000.
        rejected(CheckoutError.INVALID_QUANTITY, line(10_000, 100, factor = 101))
        // Factor and quantity whose Int product would overflow.
        rejected(CheckoutError.INVALID_QUANTITY, line(10_000, 100, factor = Int.MAX_VALUE))
    }

    @Test
    fun quantityLimitAppliesToTheSumOverLinesOfOneProduct() = runTest {
        val half = line(5_000, 100, factor = 100) // 500,000 base units
        val overLimit = line(1, 100) // one more
        rejected(CheckoutError.INVALID_QUANTITY, half, half, overLimit)
        // The same quantities spread over two products are fine.
        val result = completeSale(listOf(half, half, overLimit.copy(productId = 2)), Long.MAX_VALUE)
        assertTrue(result is CheckoutResult.Completed)
    }

    @Test
    fun maximumQuantityAndPriceAreAccepted() = runTest {
        val max = line(CompleteSaleUseCase.MAX_QUANTITY, CompleteSaleUseCase.MAX_UNIT_PRICE_CENTAVOS, factor = 100)
        val result = completeSale(listOf(max), max.lineTotalCentavos) as CheckoutResult.Completed
        assertEquals(1_000_000_000_000L, result.sale.totalCentavos)
        assertEquals(1_000_000L, sales.completed.single().first.single().baseQuantity)
    }

    @Test
    fun priceAndPaymentBoundaries() = runTest {
        rejected(CheckoutError.INVALID_PRICE, line(1, 0))
        rejected(CheckoutError.INVALID_PRICE, line(1, -1))
        rejected(CheckoutError.INVALID_PRICE, line(1, CompleteSaleUseCase.MAX_UNIT_PRICE_CENTAVOS + 1))
        rejected(CheckoutError.INSUFFICIENT_PAYMENT, line(2, 500), received = 999)
        rejected(CheckoutError.INSUFFICIENT_PAYMENT, line(2, 500), received = -1)
        rejected(CheckoutError.EMPTY_CART)
    }

    @Test
    fun insufficientStockFromTheRepositoryBecomesADedicatedResult() = runTest {
        val shortage = StockShortage(1, "Coke", "bottle", availableBaseQuantity = 2, requestedBaseQuantity = 3)
        sales.failWith = InsufficientCheckoutStockException(listOf(shortage))
        assertEquals(CheckoutResult.InsufficientStock(listOf(shortage)), completeSale(listOf(line(3, 100)), 300))
    }
}
