package com.example.sarisari.manager.domain.usecase

import com.example.sarisari.manager.domain.model.CompletedSale
import com.example.sarisari.manager.domain.model.ProductUnavailableException
import com.example.sarisari.manager.domain.model.SaleLineInput
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
}

/** Change owed to the customer. Never negative. */
fun changeCentavos(totalCentavos: Long, receivedCentavos: Long): Long = maxOf(0L, receivedCentavos - totalCentavos)

/** Validates a cash sale, then saves it atomically with its stock deductions. */
class CompleteSaleUseCase @Inject constructor(
    private val sales: SalesRepository,
) {
    suspend operator fun invoke(lines: List<SaleLineInput>, amountReceivedCentavos: Long): CheckoutResult {
        if (lines.isEmpty()) return CheckoutResult.Rejected(CheckoutError.EMPTY_CART)
        if (lines.any { it.quantity < 1 || it.quantity > MAX_QUANTITY || it.unitFactor < 1 }) {
            return CheckoutResult.Rejected(CheckoutError.INVALID_QUANTITY)
        }
        if (lines.any { it.unitPriceCentavos < 1 || it.unitPriceCentavos > MAX_UNIT_PRICE_CENTAVOS }) {
            return CheckoutResult.Rejected(CheckoutError.INVALID_PRICE)
        }
        if (amountReceivedCentavos < lines.totalCentavos()) {
            return CheckoutResult.Rejected(CheckoutError.INSUFFICIENT_PAYMENT)
        }
        return try {
            CheckoutResult.Completed(sales.completeSale(lines, amountReceivedCentavos))
        } catch (_: ProductUnavailableException) {
            CheckoutResult.Rejected(CheckoutError.PRODUCT_UNAVAILABLE)
        }
    }

    companion object {
        const val MAX_QUANTITY = 10_000
        const val MAX_UNIT_PRICE_CENTAVOS = 100_000_000L
    }
}
