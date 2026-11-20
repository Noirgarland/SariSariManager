package com.example.sarisari.manager.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One change to a product's stock, in the base unit. [quantityChange] is signed.
 * [unitCostCentavos] and [totalCostCentavos] are set for receipts only.
 */
@Entity(
    tableName = "stock_movements",
    foreignKeys = [
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["product_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("product_id"), Index("created_at")],
)
data class StockMovementEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Long = 0,
    @ColumnInfo(name = "product_id") val productId: Long,
    @ColumnInfo(name = "type") val type: String,
    @ColumnInfo(name = "quantity_change") val quantityChange: Int,
    @ColumnInfo(name = "unit_cost_centavos") val unitCostCentavos: Long?,
    @ColumnInfo(name = "total_cost_centavos") val totalCostCentavos: Long?,
    @ColumnInfo(name = "reason") val reason: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

data class StockMovementWithProduct(
    @Embedded val movement: StockMovementEntity,
    @ColumnInfo(name = "product_name") val productName: String,
    @ColumnInfo(name = "base_unit_name") val baseUnitName: String,
)
