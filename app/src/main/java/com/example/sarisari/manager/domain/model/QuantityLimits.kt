package com.example.sarisari.manager.domain.model

/**
 * Upper bounds for quantities, all in a product's base unit unless stated otherwise.
 * They keep every stored quantity well inside the Int columns used by the database,
 * so intermediate math done in Long can be narrowed to Int safely after validation.
 */
object QuantityLimits {
    /** Largest cached stock a product may hold. */
    const val MAX_STOCK_QUANTITY: Int = 1_000_000_000

    /** Largest number of base units in one stock movement or one product's share of a sale. */
    const val MAX_MOVEMENT_QUANTITY: Int = 1_000_000

    /** Largest number of base units one selling unit (pack, box, case) may hold. */
    const val MAX_UNIT_FACTOR: Int = 100_000

    /** True when [current] + [change] stays within 0..[MAX_STOCK_QUANTITY]. Computed in Long, so it cannot overflow. */
    fun isValidStock(current: Int, change: Long): Boolean =
        (current.toLong() + change) in 0L..MAX_STOCK_QUANTITY.toLong()
}

/** Thrown before anything is written when a stock change would push stock above [QuantityLimits.MAX_STOCK_QUANTITY]. */
class StockLimitExceededException(val productId: Long, val currentStock: Int, val change: Long) :
    Exception("Product $productId: stock $currentStock plus $change exceeds ${QuantityLimits.MAX_STOCK_QUANTITY}")
