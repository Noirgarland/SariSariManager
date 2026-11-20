package com.example.sarisari.manager.domain.repository

import com.example.sarisari.manager.domain.model.StockMovement
import com.example.sarisari.manager.domain.model.StockMovementType
import kotlinx.coroutines.flow.Flow

interface InventoryRepository {
    /** Newest first. [productId] null returns movements for all products. */
    fun observeMovements(productId: Long?, limit: Int = 200): Flow<List<StockMovement>>

    /**
     * Records a receipt and updates the product's stock and cost in one transaction.
     * [newUnitCost] receives the current stock and cost read inside that transaction and returns the new cost.
     * Returns the movement id.
     */
    suspend fun receiveStock(
        productId: Long,
        baseQuantity: Int,
        totalCostCentavos: Long,
        reason: String?,
        newUnitCost: (currentQuantity: Int, currentCostCentavos: Long) -> Long,
    ): Long

    /**
     * Records a stock decrease or increase in one transaction.
     * @throws com.example.sarisari.manager.domain.model.InsufficientStockException if stock would go below zero.
     */
    suspend fun adjustStock(productId: Long, type: StockMovementType, quantityChange: Int, reason: String?): Long
}
