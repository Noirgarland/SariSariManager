package com.example.sarisari.manager.domain.usecase

import com.example.sarisari.manager.domain.model.CompletedSale
import com.example.sarisari.manager.domain.model.InsufficientCheckoutStockException
import com.example.sarisari.manager.domain.model.ProductUnavailableException
import com.example.sarisari.manager.domain.model.QuantityLimits
import com.example.sarisari.manager.domain.model.SaleLineInput
import com.example.sarisari.manager.domain.model.StockShortage
import com.example.sarisari.manager.domain.model.totalCentavos
import com.example.sarisari.manager.domain.repository.SalesRepository
import javax.inject.Inject

enum class CheckoutError {
    EMPTY_CART,
    INVALID_QUANTITY,
    INVALID_PRICE,
    INSUFFICIENT_PAYMENT,
    PRODUCT_UNAVAILABLE,
}

sealed interface CheckoutResult {
    data class Completed(val sale: CompletedSale) : CheckoutResult
    data class Rejected(val error: CheckoutError) : CheckoutResult

    /** The stock on hand, read inside the checkout transaction, cannot cover the cart. Nothing was saved. */
    data class InsufficientStock(val shortages: List<StockShortage>) : CheckoutResult
}

/** Change owed to the customer. Never negative. */
fun changeCentavos(totalCentavos: Long, receivedCentavos: Long): Long = maxOf(0L, receivedCentavos - totalCentavos)

/**
 * Validates a cash sale, then saves it atomically with its stock deductions.
 * The stock check itself runs inside the database transaction, so the sale is refused
 * if stock is insufficient at the moment it would be written.
 */
class CompleteSaleUseCase @Inject constructor(
    private val sales: SalesRepository,
) {
    suspend operator fun invoke(lines: List<SaleLineInput>, amountReceivedCentavos: Long): CheckoutResult {
        if (lines.isEmpty()) return CheckoutResult.Rejected(CheckoutError.EMPTY_CART)
        if (lines.any { !it.hasValidQuantity() }) return CheckoutResult.Rejected(CheckoutError.INVALID_QUANTITY)
        val perProduct = lines.groupBy { it.productId }.values.map { group -> group.sumOf { it.baseQuantity } }
        if (perProduct.any { it > QuantityLimits.MAX_MOVEMENT_QUANTITY }) {
            return CheckoutResult.Rejected(CheckoutError.INVALID_QUANTITY)
        }
        if (lines.any { it.unitPriceCentavos < 1 || it.unitPriceCentavos > MAX_UNIT_PRICE_CENTAVOS }) {
            return CheckoutResult.Rejected(CheckoutError.INVALID_PRICE)
        }
        val total = try {
            lines.totalCentavos()
        } catch (_: ArithmeticException) {
            return CheckoutResult.Rejected(CheckoutError.INVALID_PRICE)
        }
        if (total > MAX_SALE_TOTAL_CENTAVOS) return CheckoutResult.Rejected(CheckoutError.INVALID_PRICE)
        if (amountReceivedCentavos < total) return CheckoutResult.Rejected(CheckoutError.INSUFFICIENT_PAYMENT)
        return try {
            CheckoutResult.Completed(sales.completeSale(lines, amountReceivedCentavos))
        } catch (_: ProductUnavailableException) {
            CheckoutResult.Rejected(CheckoutError.PRODUCT_UNAVAILABLE)
        } catch (e: InsufficientCheckoutStockException) {
            CheckoutResult.InsufficientStock(e.shortages)
        }
    }

    private fun SaleLineInput.hasValidQuantity(): Boolean =
        quantity in 1..MAX_QUANTITY &&
            unitFactor in 1..QuantityLimits.MAX_UNIT_FACTOR &&
            baseQuantity <= QuantityLimits.MAX_MOVEMENT_QUANTITY

    companion object {
        const val MAX_QUANTITY = 10_000
        const val MAX_UNIT_PRICE_CENTAVOS = 100_000_000L

        /** Ten thousand lines at the maximum line value. Keeps every sale total far from Long overflow. */
        const val MAX_SALE_TOTAL_CENTAVOS = 10_000_000_000_000_000L
    }
}
