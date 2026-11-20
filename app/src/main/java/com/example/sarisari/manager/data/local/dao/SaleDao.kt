package com.example.sarisari.manager.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.example.sarisari.manager.data.local.entity.ProductEntity
import com.example.sarisari.manager.data.local.entity.SaleEntity
import com.example.sarisari.manager.data.local.entity.SaleItemEntity
import com.example.sarisari.manager.data.local.entity.SaleWithItems
import com.example.sarisari.manager.data.local.entity.StockMovementEntity
import com.example.sarisari.manager.domain.model.CompletedSale
import com.example.sarisari.manager.domain.model.ProductUnavailableException
import com.example.sarisari.manager.domain.model.SaleAlreadyVoidedException
import com.example.sarisari.manager.domain.model.SaleLineInput
import com.example.sarisari.manager.domain.model.SaleNotFoundException
import com.example.sarisari.manager.domain.model.totalCentavos
import kotlinx.coroutines.flow.Flow

@Dao
abstract class SaleDao {

    @Transaction
    @Query("SELECT * FROM sales ORDER BY created_at DESC, id DESC LIMIT :limit")
    abstract fun observeAll(limit: Int): Flow<List<SaleWithItems>>

    @Transaction
    @Query("SELECT * FROM sales WHERE id = :id")
    abstract fun observeById(id: Long): Flow<SaleWithItems?>

    @Transaction
    @Query("SELECT * FROM sales WHERE id = :id")
    abstract suspend fun getById(id: Long): SaleWithItems?

    @Query("SELECT * FROM products WHERE id = :id")
    protected abstract suspend fun getProduct(id: Long): ProductEntity?

    @Query("UPDATE products SET stock_quantity = :stock, updated_at = :now WHERE id = :id")
    protected abstract suspend fun updateStock(id: Long, stock: Int, now: Long)

    @Insert
    protected abstract suspend fun insertSale(sale: SaleEntity): Long

    @Insert
    protected abstract suspend fun insertItem(item: SaleItemEntity): Long

    @Insert
    protected abstract suspend fun insertMovement(movement: StockMovementEntity): Long

    @Query("UPDATE sales SET voided_at = :now, void_reason = :reason, updated_at = :now WHERE id = :id")
    protected abstract suspend fun markVoided(id: Long, reason: String?, now: Long)

    /**
     * Saves the sale, one item per line with the product's current cost as the cost snapshot,
     * and a stock deduction per line. Any failure rolls back everything.
     * Stock may go below zero, because the shop may sell items it has not yet recorded as received.
     */
    @Transaction
    open suspend fun complete(lines: List<SaleLineInput>, amountReceived: Long, now: Long): CompletedSale {
        val total = lines.totalCentavos()
        require(lines.isNotEmpty()) { "A sale needs at least one line" }
        require(amountReceived >= total) { "Amount received is less than the total" }

        // Look up every product first so an unavailable product fails before anything is written.
        val unavailable = lines.firstOrNull { line -> getProduct(line.productId)?.isArchived != false }
        if (unavailable != null) throw ProductUnavailableException(unavailable.productId)

        val saleId = insertSale(
            SaleEntity(
                totalCentavos = total,
                amountReceivedCentavos = amountReceived,
                changeCentavos = amountReceived - total,
                totalCostCentavos = 0,
                createdAt = now,
                updatedAt = now,
            )
        )
        var totalCost = 0L
        for (line in lines) {
            val product = requireNotNull(getProduct(line.productId))
            val lineCost = line.baseQuantity.toLong() * product.costCentavos
            totalCost += lineCost
            insertItem(
                SaleItemEntity(
                    saleId = saleId,
                    productId = product.id,
                    productName = product.name,
                    unitName = line.unitName,
                    unitFactor = line.unitFactor,
                    quantity = line.quantity,
                    baseQuantity = line.baseQuantity,
                    unitPriceCentavos = line.unitPriceCentavos,
                    lineTotalCentavos = line.lineTotalCentavos,
                    unitCostCentavos = product.costCentavos,
                    lineCostCentavos = lineCost,
                    createdAt = now,
                    updatedAt = now,
                )
            )
            insertMovement(
                StockMovementEntity(
                    productId = product.id,
                    type = "SALE",
                    quantityChange = -line.baseQuantity,
                    unitCostCentavos = null,
                    totalCostCentavos = null,
                    reason = "Sale #$saleId",
                    createdAt = now,
                    updatedAt = now,
                )
            )
            updateStock(product.id, product.stockQuantity - line.baseQuantity, now)
        }
        // The cost total is only known after the lines are processed.
        updateTotalCost(saleId, totalCost)
        return CompletedSale(saleId, total, amountReceived, amountReceived - total, now)
    }

    @Query("UPDATE sales SET total_cost_centavos = :totalCost WHERE id = :id")
    protected abstract suspend fun updateTotalCost(id: Long, totalCost: Long)

    /** Marks the sale voided and puts each item's stock back with a compensating movement. */
    @Transaction
    open suspend fun voidSale(saleId: Long, reason: String?, now: Long) {
        val saleWithItems = getById(saleId) ?: throw SaleNotFoundException(saleId)
        if (saleWithItems.sale.voidedAt != null) throw SaleAlreadyVoidedException(saleId)
        for (item in saleWithItems.items) {
            val product = getProduct(item.productId) ?: throw ProductUnavailableException(item.productId)
            insertMovement(
                StockMovementEntity(
                    productId = product.id,
                    type = "SALE_VOID",
                    quantityChange = item.baseQuantity,
                    unitCostCentavos = null,
                    totalCostCentavos = null,
                    reason = "Void of sale #$saleId",
                    createdAt = now,
                    updatedAt = now,
                )
            )
            updateStock(product.id, product.stockQuantity + item.baseQuantity, now)
        }
        markVoided(saleId, reason, now)
    }
}
