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
import com.example.sarisari.manager.domain.model.InsufficientCheckoutStockException
import com.example.sarisari.manager.domain.model.ProductUnavailableException
import com.example.sarisari.manager.domain.model.QuantityLimits
import com.example.sarisari.manager.domain.model.SaleAlreadyVoidedException
import com.example.sarisari.manager.domain.model.SaleLineInput
import com.example.sarisari.manager.domain.model.SaleNotFoundException
import com.example.sarisari.manager.domain.model.StockLimitExceededException
import com.example.sarisari.manager.domain.model.StockShortage
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

    /**
     * Deducts stock only if the product is still sellable and has enough stock.
     * Returns the number of rows changed: 1 on success, 0 when the guard failed.
     */
    @Query(
        """
        UPDATE products SET stock_quantity = stock_quantity - :quantity, updated_at = :now
        WHERE id = :id AND is_archived = 0 AND stock_quantity >= :quantity
        """
    )
    protected abstract suspend fun deductStockIfAvailable(id: Long, quantity: Int, now: Long): Int

    @Insert
    protected abstract suspend fun insertSale(sale: SaleEntity): Long

    @Insert
    protected abstract suspend fun insertItem(item: SaleItemEntity): Long

    @Insert
    protected abstract suspend fun insertMovement(movement: StockMovementEntity): Long

    @Query("UPDATE sales SET voided_at = :now, void_reason = :reason, updated_at = :now WHERE id = :id")
    protected abstract suspend fun markVoided(id: Long, reason: String?, now: Long)

    @Query("UPDATE sales SET total_cost_centavos = :totalCost WHERE id = :id")
    protected abstract suspend fun updateTotalCost(id: Long, totalCost: Long)

    /**
     * Saves the sale, one item per line with snapshots of the product name, base unit, and current cost,
     * and a stock deduction per line. Any failure rolls back everything.
     *
     * Negative stock is not allowed. Quantities are summed per product across all cart lines and
     * checked against the stock read inside this transaction. Each deduction is also guarded in SQL
     * (`stock_quantity >= :quantity`), so stock cannot go below zero even if it changed after the read.
     *
     * @throws ProductUnavailableException if a product is missing or archived. Nothing is written.
     * @throws InsufficientCheckoutStockException if stock cannot cover the cart. Nothing is written.
     */
    @Transaction
    open suspend fun complete(lines: List<SaleLineInput>, amountReceived: Long, now: Long): CompletedSale {
        require(lines.isNotEmpty()) { "A sale needs at least one line" }
        val total = lines.totalCentavos()
        require(amountReceived >= total) { "Amount received is less than the total" }

        // Base quantity per product over every cart line, in cart order.
        val requested = LinkedHashMap<Long, Long>()
        for (line in lines) {
            require(line.baseQuantity in 1..QuantityLimits.MAX_MOVEMENT_QUANTITY) { "Invalid base quantity" }
            requested[line.productId] = Math.addExact(requested[line.productId] ?: 0L, line.baseQuantity)
        }
        // Read and check every product before anything is written.
        val products = requested.keys.associateWith { id ->
            getProduct(id)?.takeIf { !it.isArchived } ?: throw ProductUnavailableException(id)
        }
        val shortages = requested.mapNotNull { (id, quantity) -> products.getValue(id).shortageFor(quantity) }
        if (shortages.isNotEmpty()) throw InsufficientCheckoutStockException(shortages)

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
            val product = products.getValue(line.productId)
            val baseQuantity = Math.toIntExact(line.baseQuantity)
            val lineCost = Math.multiplyExact(line.baseQuantity, product.costCentavos)
            totalCost = Math.addExact(totalCost, lineCost)
            insertItem(
                SaleItemEntity(
                    saleId = saleId,
                    productId = product.id,
                    productName = product.name,
                    unitName = line.unitName,
                    baseUnitName = product.baseUnitName,
                    unitFactor = line.unitFactor,
                    quantity = line.quantity,
                    baseQuantity = baseQuantity,
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
                    quantityChange = -baseQuantity,
                    unitCostCentavos = null,
                    totalCostCentavos = null,
                    reason = "Sale #$saleId",
                    createdAt = now,
                    updatedAt = now,
                )
            )
        }
        for ((id, quantity) in requested) {
            if (deductStockIfAvailable(id, Math.toIntExact(quantity), now) != 1) {
                // Stock changed after it was read. Throwing rolls back the sale, its items, and movements.
                val current = getProduct(id) ?: throw ProductUnavailableException(id)
                if (current.isArchived) throw ProductUnavailableException(id)
                throw InsufficientCheckoutStockException(listOfNotNull(current.shortageFor(quantity)))
            }
        }
        // The cost total is only known after the lines are processed.
        updateTotalCost(saleId, totalCost)
        return CompletedSale(saleId, total, amountReceived, amountReceived - total, now)
    }

    private fun ProductEntity.shortageFor(requested: Long): StockShortage? =
        if (requested <= stockQuantity) {
            null
        } else {
            StockShortage(id, name, baseUnitName, availableBaseQuantity = stockQuantity, requestedBaseQuantity = requested)
        }

    /**
     * Marks the sale voided and puts each item's stock back with a compensating movement.
     * @throws StockLimitExceededException if returning the stock would exceed the stock limit. Nothing is written.
     */
    @Transaction
    open suspend fun voidSale(saleId: Long, reason: String?, now: Long) {
        val saleWithItems = getById(saleId) ?: throw SaleNotFoundException(saleId)
        if (saleWithItems.sale.voidedAt != null) throw SaleAlreadyVoidedException(saleId)
        for (item in saleWithItems.items) {
            // Read again per item: an earlier item of the same sale may have changed this product's stock.
            val product = getProduct(item.productId) ?: throw ProductUnavailableException(item.productId)
            if (!QuantityLimits.isValidStock(product.stockQuantity, item.baseQuantity.toLong())) {
                throw StockLimitExceededException(product.id, product.stockQuantity, item.baseQuantity.toLong())
            }
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
