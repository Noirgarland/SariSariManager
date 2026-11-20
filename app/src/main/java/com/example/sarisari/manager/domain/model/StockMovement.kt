package com.example.sarisari.manager.domain.model

enum class StockMovementType { RECEIPT, DAMAGED, EXPIRED, ADJUSTMENT, SALE, SALE_VOID }

/** One change to a product's stock. [quantityChange] is signed and in the product's base unit. */
data class StockMovement(
    val id: Long,
    val productId: Long,
    val productName: String,
    val baseUnitName: String,
    val type: StockMovementType,
    val quantityChange: Int,
    val unitCostCentavos: Long?,
    val totalCostCentavos: Long?,
    val reason: String?,
    val createdAtMillis: Long,
)
