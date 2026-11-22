package com.example.sarisari.manager.domain.model

/**
 * One product in the cart that asks for more than the stock on hand.
 * [requestedBaseQuantity] is the sum over every cart line for that product. Both quantities are in [baseUnitName].
 */
data class StockShortage(
    val productId: Long,
    val productName: String,
    val baseUnitName: String,
    val availableBaseQuantity: Int,
    val requestedBaseQuantity: Long,
)

/** Thrown inside the checkout transaction when stock is insufficient. The transaction is rolled back. */
class InsufficientCheckoutStockException(val shortages: List<StockShortage>) :
    Exception(
        shortages.joinToString(prefix = "Insufficient stock: ") {
            "${it.productName} (${it.availableBaseQuantity} available, ${it.requestedBaseQuantity} requested)"
        }
    )
