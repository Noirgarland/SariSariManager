package com.example.sarisari.manager.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "expenses", indices = [Index("spent_at")])
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Long = 0,
    @ColumnInfo(name = "category") val category: String,
    @ColumnInfo(name = "description") val description: String?,
    @ColumnInfo(name = "amount_centavos") val amountCentavos: Long,
    @ColumnInfo(name = "spent_at") val spentAt: Long,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

@Entity(tableName = "store_sessions", indices = [Index("opened_at")])
data class StoreSessionEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Long = 0,
    @ColumnInfo(name = "opened_at") val openedAt: Long,
    @ColumnInfo(name = "closed_at") val closedAt: Long?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

data class SalesTotalsRow(
    @ColumnInfo(name = "transactions") val transactions: Int,
    @ColumnInfo(name = "revenue") val revenue: Long,
    @ColumnInfo(name = "cost") val cost: Long,
)

data class TopProductRow(
    @ColumnInfo(name = "product_id") val productId: Long,
    @ColumnInfo(name = "product_name") val productName: String,
    @ColumnInfo(name = "base_unit_name") val baseUnitName: String,
    @ColumnInfo(name = "quantity") val quantity: Int,
    @ColumnInfo(name = "revenue") val revenue: Long,
)
