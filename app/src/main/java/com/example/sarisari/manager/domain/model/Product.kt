package com.example.sarisari.manager.domain.model

enum class StockStatus { OUT_OF_STOCK, LOW_STOCK, IN_STOCK }

/**
 * A sellable product. Money is in centavos. Stock and cost are per base unit.
 * [id] is 0 for a product that has not been saved yet.
 */
data class Product(
    val id: Long = 0,
    val name: String,
    val categoryId: Long?,
    val baseUnitName: String,
    val costCentavos: Long,
    val sellingPriceCentavos: Long,
    val stockQuantity: Int = 0,
    val lowStockThreshold: Int = 0,
    val isArchived: Boolean = false,
    val units: List<ProductUnit> = emptyList(),
) {
    val stockStatus: StockStatus
        get() = when {
            stockQuantity <= 0 -> StockStatus.OUT_OF_STOCK
            stockQuantity <= lowStockThreshold -> StockStatus.LOW_STOCK
            else -> StockStatus.IN_STOCK
        }
}
