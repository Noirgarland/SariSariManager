package com.example.sarisari.manager.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.example.sarisari.manager.data.local.entity.CreditEntryEntity
import com.example.sarisari.manager.data.local.entity.CustomerEntity
import com.example.sarisari.manager.data.local.entity.CustomerSummaryRow
import com.example.sarisari.manager.domain.model.CreditEntryNotVoidableException
import com.example.sarisari.manager.domain.model.CustomerNotFoundException
import com.example.sarisari.manager.domain.model.PaymentExceedsBalanceException
import kotlinx.coroutines.flow.Flow

/** Customers and their utang ledger. A balance is always the sum of the customer's entries. */
@Dao
abstract class CustomerDao {

    /** [query] must already have LIKE wildcards escaped with a backslash. Highest balance first, then by name. */
    @Query(
        """
        SELECT c.id, c.name,
               COALESCE((SELECT SUM(e.amount_centavos) FROM credit_entries e WHERE e.customer_id = c.id), 0) AS balance,
               (SELECT MAX(e.occurred_at) FROM credit_entries e
                 WHERE e.customer_id = c.id AND e.type = 'CREDIT_SALE') AS last_credit_at,
               (SELECT MAX(e.occurred_at) FROM credit_entries e
                 WHERE e.customer_id = c.id AND e.type = 'PAYMENT' AND e.voided_at IS NULL) AS last_payment_at
        FROM customers c
        WHERE (:query = '' OR c.name LIKE '%' || :query || '%' ESCAPE '\')
        ORDER BY balance DESC, c.name COLLATE NOCASE
        """
    )
    abstract fun observeSummaries(query: String): Flow<List<CustomerSummaryRow>>

    @Query(
        """
        SELECT c.id, c.name,
               COALESCE((SELECT SUM(e.amount_centavos) FROM credit_entries e WHERE e.customer_id = c.id), 0) AS balance,
               (SELECT MAX(e.occurred_at) FROM credit_entries e
                 WHERE e.customer_id = c.id AND e.type = 'CREDIT_SALE') AS last_credit_at,
               (SELECT MAX(e.occurred_at) FROM credit_entries e
                 WHERE e.customer_id = c.id AND e.type = 'PAYMENT' AND e.voided_at IS NULL) AS last_payment_at
        FROM customers c WHERE c.id = :id
        """
    )
    abstract fun observeSummary(id: Long): Flow<CustomerSummaryRow?>

    @Query("SELECT * FROM credit_entries WHERE customer_id = :customerId ORDER BY occurred_at DESC, id DESC")
    abstract fun observeEntries(customerId: Long): Flow<List<CreditEntryEntity>>

    /** What all customers owe right now. Customers with a zero or negative balance add nothing. */
    @Query(
        """
        SELECT COALESCE(SUM(balance), 0) FROM (
            SELECT SUM(amount_centavos) AS balance FROM credit_entries GROUP BY customer_id HAVING balance > 0
        )
        """
    )
    abstract fun observeOutstandingTotal(): Flow<Long>

    /** Payments received in [start, end), excluding voided ones. Payments are stored negative. */
    @Query(
        """
        SELECT COALESCE(-SUM(amount_centavos), 0) FROM credit_entries
        WHERE type = 'PAYMENT' AND voided_at IS NULL AND occurred_at >= :start AND occurred_at < :end
        """
    )
    abstract fun observeCollected(start: Long, end: Long): Flow<Long>

    @Query("SELECT * FROM customers WHERE id = :id")
    abstract suspend fun getCustomer(id: Long): CustomerEntity?

    @Query("SELECT COALESCE(SUM(amount_centavos), 0) FROM credit_entries WHERE customer_id = :customerId")
    abstract suspend fun balance(customerId: Long): Long

    @Insert
    abstract suspend fun insertCustomer(customer: CustomerEntity): Long

    @Query("UPDATE customers SET name = :name, updated_at = :now WHERE id = :id")
    abstract suspend fun renameCustomer(id: Long, name: String, now: Long)

    @Insert
    protected abstract suspend fun insertEntry(entry: CreditEntryEntity): Long

    @Query("SELECT * FROM credit_entries WHERE id = :id")
    protected abstract suspend fun getEntry(id: Long): CreditEntryEntity?

    @Query("UPDATE credit_entries SET voided_at = :now, updated_at = :now WHERE id = :id")
    protected abstract suspend fun markEntryVoided(id: Long, now: Long)

    /**
     * Records a payment of [amountCentavos] (positive) made at [paidAt].
     * @throws CustomerNotFoundException
     * @throws PaymentExceedsBalanceException if the payment is more than the balance read in this transaction.
     */
    @Transaction
    open suspend fun recordPayment(customerId: Long, amountCentavos: Long, paidAt: Long, note: String?, now: Long): Long {
        require(amountCentavos > 0) { "Payment must be positive" }
        getCustomer(customerId) ?: throw CustomerNotFoundException(customerId)
        val balance = balance(customerId)
        if (amountCentavos > balance) throw PaymentExceedsBalanceException(balance, amountCentavos)
        return insertEntry(
            CreditEntryEntity(
                customerId = customerId,
                type = TYPE_PAYMENT,
                amountCentavos = -amountCentavos,
                saleId = null,
                note = note,
                occurredAt = paidAt,
                createdAt = now,
                updatedAt = now,
            )
        )
    }

    /**
     * Voids a payment recorded by mistake: marks it voided and adds a compensating entry,
     * so the amount goes back on the balance.
     * @throws CreditEntryNotVoidableException if [entryId] is not an unvoided payment.
     */
    @Transaction
    open suspend fun voidPayment(entryId: Long, now: Long) {
        val entry = getEntry(entryId)
        if (entry == null || entry.type != TYPE_PAYMENT || entry.voidedAt != null) {
            throw CreditEntryNotVoidableException(entryId)
        }
        insertEntry(
            CreditEntryEntity(
                customerId = entry.customerId,
                type = TYPE_PAYMENT_VOID,
                amountCentavos = -entry.amountCentavos,
                saleId = null,
                note = "Void of payment #${entry.id}",
                occurredAt = now,
                createdAt = now,
                updatedAt = now,
            )
        )
        markEntryVoided(entryId, now)
    }

    companion object {
        const val TYPE_CREDIT_SALE = "CREDIT_SALE"
        const val TYPE_PAYMENT = "PAYMENT"
        const val TYPE_SALE_VOID = "SALE_VOID"
        const val TYPE_PAYMENT_VOID = "PAYMENT_VOID"
    }
}
