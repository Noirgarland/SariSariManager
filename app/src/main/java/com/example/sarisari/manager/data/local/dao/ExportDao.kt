package com.example.sarisari.manager.data.local.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Query

data class SaleExportRow(
    @ColumnInfo(name = "item_id") val itemId: Long,
    @ColumnInfo(name = "sale_id") val saleId: Long,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "voided_at") val voidedAt: Long?,
    @ColumnInfo(name = "product_name") val productName: String,
    @ColumnInfo(name = "unit_name") val unitName: String,
    @ColumnInfo(name = "quantity") val quantity: Int,
    @ColumnInfo(name = "base_quantity") val baseQuantity: Int,
    @ColumnInfo(name = "unit_price_centavos") val unitPriceCentavos: Long,
    @ColumnInfo(name = "line_total_centavos") val lineTotalCentavos: Long,
    @ColumnInfo(name = "line_cost_centavos") val lineCostCentavos: Long,
)

data class ProductExportRow(
    @ColumnInfo(name = "id") val id: Long,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "category_name") val categoryName: String?,
    @ColumnInfo(name = "base_unit_name") val baseUnitName: String,
    @ColumnInfo(name = "cost_centavos") val costCentavos: Long,
    @ColumnInfo(name = "selling_price_centavos") val sellingPriceCentavos: Long,
    @ColumnInfo(name = "stock_quantity") val stockQuantity: Int,
    @ColumnInfo(name = "low_stock_threshold") val lowStockThreshold: Int,
    @ColumnInfo(name = "is_archived") val isArchived: Boolean,
)

/** Paged reads for CSV export, so large exports are streamed instead of loaded at once. */
@Dao
interface ExportDao {
    @Query(
        """
        SELECT i.id AS item_id, s.id AS sale_id, s.created_at, s.voided_at, i.product_name, i.unit_name,
               i.quantity, i.base_quantity, i.unit_price_centavos, i.line_total_centavos, i.line_cost_centavos
        FROM sale_items i JOIN sales s ON s.id = i.sale_id
        WHERE s.created_at >= :start AND s.created_at < :end AND i.id > :afterItemId
        ORDER BY i.id
        LIMIT :limit
        """
    )
    suspend fun saleItemsPage(start: Long, end: Long, afterItemId: Long, limit: Int): List<SaleExportRow>

    @Query(
        """
        SELECT p.id, p.name, c.name AS category_name, p.base_unit_name, p.cost_centavos, p.selling_price_centavos,
               p.stock_quantity, p.low_stock_threshold, p.is_archived
        FROM products p LEFT JOIN categories c ON c.id = p.category_id
        WHERE p.id > :afterId
        ORDER BY p.id
        LIMIT :limit
        """
    )
    suspend fun productsPage(afterId: Long, limit: Int): List<ProductExportRow>
}
