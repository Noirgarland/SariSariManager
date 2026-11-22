package com.example.sarisari.manager.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** A customer who may buy on utang (credit). Customers are never deleted. Added in schema version 7. */
@Entity(tableName = "customers", indices = [Index(value = ["name"], unique = true)])
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Long = 0,
    @ColumnInfo(name = "name", collate = ColumnInfo.NOCASE) val name: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

/**
 * One change to a customer's utang balance. [amountCentavos] is signed: positive adds to what the customer
 * owes (a sale on utang, a voided payment), negative reduces it (a payment, a voided sale).
 * The balance is the sum of all entries. Rows are never edited, except to mark a payment voided.
 */
@Entity(
    tableName = "credit_entries",
    foreignKeys = [
        ForeignKey(
            entity = CustomerEntity::class,
            parentColumns = ["id"],
            childColumns = ["customer_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = SaleEntity::class,
            parentColumns = ["id"],
            childColumns = ["sale_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("customer_id"), Index("sale_id"), Index("occurred_at")],
)
data class CreditEntryEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Long = 0,
    @ColumnInfo(name = "customer_id") val customerId: Long,
    @ColumnInfo(name = "type") val type: String,
    @ColumnInfo(name = "amount_centavos") val amountCentavos: Long,
    @ColumnInfo(name = "sale_id") val saleId: Long?,
    @ColumnInfo(name = "note") val note: String?,
    /** When the credit was given or the payment was made, as chosen by the owner. */
    @ColumnInfo(name = "occurred_at") val occurredAt: Long,
    @ColumnInfo(name = "voided_at") val voidedAt: Long? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

/** A customer with their balance and latest credit and payment dates. */
data class CustomerSummaryRow(
    @ColumnInfo(name = "id") val id: Long,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "balance") val balanceCentavos: Long,
    @ColumnInfo(name = "last_credit_at") val lastCreditAt: Long?,
    @ColumnInfo(name = "last_payment_at") val lastPaymentAt: Long?,
)
