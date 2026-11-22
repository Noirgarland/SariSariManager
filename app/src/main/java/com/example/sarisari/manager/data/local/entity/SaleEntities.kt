package com.example.sarisari.manager.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

/**
 * A completed sale. Rows are never edited except to record a void.
 * [amountReceivedCentavos] is the cash handed over. For a sale on utang, [creditCentavos] is the part owed
 * by [customerId], so total = cash kept + credit. Both columns were added in schema version 7.
 */
@Entity(
    tableName = "sales",
    foreignKeys = [
        ForeignKey(
            entity = CustomerEntity::class,
            parentColumns = ["id"],
            childColumns = ["customer_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("created_at"), Index("customer_id")],
)
data class SaleEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Long = 0,
    @ColumnInfo(name = "total_centavos") val totalCentavos: Long,
    @ColumnInfo(name = "amount_received_centavos") val amountReceivedCentavos: Long,
    @ColumnInfo(name = "change_centavos") val changeCentavos: Long,
    @ColumnInfo(name = "total_cost_centavos") val totalCostCentavos: Long,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "voided_at") val voidedAt: Long? = null,
    @ColumnInfo(name = "void_reason") val voidReason: String? = null,
    @ColumnInfo(name = "customer_id") val customerId: Long? = null,
    @ColumnInfo(name = "credit_centavos", defaultValue = "0") val creditCentavos: Long = 0,
)

/** Name, units, price, and cost are copies taken at sale time, so later product edits never change history. */
@Entity(
    tableName = "sale_items",
    foreignKeys = [
        ForeignKey(
            entity = SaleEntity::class,
            parentColumns = ["id"],
            childColumns = ["sale_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["product_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("sale_id"), Index("product_id")],
)
data class SaleItemEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Long = 0,
    @ColumnInfo(name = "sale_id") val saleId: Long,
    @ColumnInfo(name = "product_id") val productId: Long,
    @ColumnInfo(name = "product_name") val productName: String,
    @ColumnInfo(name = "unit_name") val unitName: String,
    /** The product's base unit at sale time, the unit of [baseQuantity]. Added in schema version 6. */
    @ColumnInfo(name = "base_unit_name") val baseUnitName: String,
    @ColumnInfo(name = "unit_factor") val unitFactor: Int,
    @ColumnInfo(name = "quantity") val quantity: Int,
    @ColumnInfo(name = "base_quantity") val baseQuantity: Int,
    @ColumnInfo(name = "unit_price_centavos") val unitPriceCentavos: Long,
    @ColumnInfo(name = "line_total_centavos") val lineTotalCentavos: Long,
    @ColumnInfo(name = "unit_cost_centavos") val unitCostCentavos: Long,
    @ColumnInfo(name = "line_cost_centavos") val lineCostCentavos: Long,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

data class SaleWithItems(
    @Embedded val sale: SaleEntity,
    @Relation(parentColumn = "id", entityColumn = "sale_id") val items: List<SaleItemEntity>,
    @Relation(parentColumn = "customer_id", entityColumn = "id") val customer: CustomerEntity? = null,
)
