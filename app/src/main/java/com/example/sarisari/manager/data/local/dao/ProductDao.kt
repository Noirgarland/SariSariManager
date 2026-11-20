package com.example.sarisari.manager.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.sarisari.manager.data.local.entity.ProductEntity
import com.example.sarisari.manager.data.local.entity.ProductUnitEntity
import com.example.sarisari.manager.data.local.entity.ProductWithUnits
import kotlinx.coroutines.flow.Flow

@Dao
abstract class ProductDao {

    /** [query] must already have LIKE wildcards escaped with a backslash. */
    @Transaction
    @Query(
        """
        SELECT * FROM products
        WHERE (:includeArchived = 1 OR is_archived = 0)
          AND (:categoryId IS NULL OR category_id = :categoryId)
          AND (:query = '' OR name LIKE '%' || :query || '%' ESCAPE '\')
        ORDER BY name COLLATE NOCASE
        """
    )
    abstract fun observe(query: String, categoryId: Long?, includeArchived: Boolean): Flow<List<ProductWithUnits>>

    @Transaction
    @Query("SELECT * FROM products WHERE id = :id")
    abstract suspend fun getWithUnits(id: Long): ProductWithUnits?

    @Query("SELECT * FROM products WHERE id = :id")
    protected abstract suspend fun getById(id: Long): ProductEntity?

    @Insert
    protected abstract suspend fun insertProduct(product: ProductEntity): Long

    @Update
    protected abstract suspend fun updateProduct(product: ProductEntity)

    @Insert
    protected abstract suspend fun insertUnits(units: List<ProductUnitEntity>)

    @Query("DELETE FROM product_units WHERE product_id = :productId")
    protected abstract suspend fun deleteUnits(productId: Long)

    @Query("UPDATE products SET is_archived = :archived, updated_at = :now WHERE id = :id")
    abstract suspend fun setArchived(id: Long, archived: Boolean, now: Long)

    /**
     * Inserts a product with id 0, otherwise updates it while keeping its cached stock and creation time.
     * Units are replaced. All writes happen in one transaction. Returns the product id.
     */
    @Transaction
    open suspend fun save(product: ProductEntity, units: List<ProductUnitEntity>, now: Long): Long {
        val productId = if (product.id == 0L) {
            insertProduct(product.copy(createdAt = now, updatedAt = now))
        } else {
            val existing = requireNotNull(getById(product.id)) { "Product ${product.id} does not exist" }
            updateProduct(
                product.copy(
                    stockQuantity = existing.stockQuantity,
                    createdAt = existing.createdAt,
                    updatedAt = now,
                )
            )
            deleteUnits(product.id)
            product.id
        }
        insertUnits(units.map { it.copy(id = 0, productId = productId, createdAt = now, updatedAt = now) })
        return productId
    }
}
