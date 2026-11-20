package com.example.sarisari.manager.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.example.sarisari.manager.data.local.entity.ProductEntity
import com.example.sarisari.manager.data.local.entity.StockMovementEntity
import com.example.sarisari.manager.data.local.entity.StockMovementWithProduct
import com.example.sarisari.manager.domain.model.InsufficientStockException
import kotlinx.coroutines.flow.Flow

@Dao
abstract class StockMovementDao {

    @Query(
        """
        SELECT m.*, p.name AS product_name, p.base_unit_name AS base_unit_name
        FROM stock_movements m JOIN products p ON p.id = m.product_id
        WHERE (:productId IS NULL OR m.product_id = :productId)
        ORDER BY m.created_at DESC, m.id DESC
        LIMIT :limit
        """
    )
    abstract fun observe(productId: Long?, limit: Int): Flow<List<StockMovementWithProduct>>

    @Query("SELECT COALESCE(SUM(quantity_change), 0) FROM stock_movements WHERE product_id = :productId")
    abstract suspend fun sumQuantity(productId: Long): Int

    @Query("SELECT * FROM products WHERE id = :id")
    protected abstract suspend fun getProduct(id: Long): ProductEntity?

    @Query(
        "UPDATE products SET stock_quantity = :stock, cost_centavos = :cost, updated_at = :now WHERE id = :id"
    )
    protected abstract suspend fun updateStockAndCost(id: Long, stock: Int, cost: Long, now: Long)

    @Insert
    protected abstract suspend fun insert(movement: StockMovementEntity): Long

    /**
     * Records a receipt, then updates the cached stock and the cost. All in one transaction.
     * [newUnitCost] gets the stock and cost read inside the transaction.
     */
    @Transaction
    open suspend fun receive(
        productId: Long,
        baseQuantity: Int,
        totalCostCentavos: Long,
        reason: String?,
        now: Long,
        newUnitCost: (Int, Long) -> Long,
    ): Long {
        val product = requireNotNull(getProduct(productId)) { "Product $productId does not exist" }
        val cost = newUnitCost(product.stockQuantity, product.costCentavos)
        val movementId = insert(
            StockMovementEntity(
                productId = productId,
                type = "RECEIPT",
                quantityChange = baseQuantity,
                unitCostCentavos = (2 * totalCostCentavos + baseQuantity) / (2L * baseQuantity),
                totalCostCentavos = totalCostCentavos,
                reason = reason,
                createdAt = now,
                updatedAt = now,
            )
        )
        updateStockAndCost(productId, product.stockQuantity + baseQuantity, cost, now)
        return movementId
    }

    /** Records a signed stock change. Throws [InsufficientStockException] before writing anything if stock would go negative. */
    @Transaction
    open suspend fun adjust(productId: Long, type: String, quantityChange: Int, reason: String?, now: Long): Long {
        val product = requireNotNull(getProduct(productId)) { "Product $productId does not exist" }
        val newStock = product.stockQuantity + quantityChange
        if (newStock < 0) throw InsufficientStockException(product.stockQuantity, -quantityChange)
        val movementId = insert(
            StockMovementEntity(
                productId = productId,
                type = type,
                quantityChange = quantityChange,
                unitCostCentavos = null,
                totalCostCentavos = null,
                reason = reason,
                createdAt = now,
                updatedAt = now,
            )
        )
        updateStockAndCost(productId, newStock, product.costCentavos, now)
        return movementId
    }
}
