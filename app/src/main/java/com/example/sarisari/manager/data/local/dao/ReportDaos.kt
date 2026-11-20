package com.example.sarisari.manager.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.example.sarisari.manager.data.local.entity.ExpenseEntity
import com.example.sarisari.manager.data.local.entity.SalesTotalsRow
import com.example.sarisari.manager.data.local.entity.StoreSessionEntity
import com.example.sarisari.manager.data.local.entity.TopProductRow
import com.example.sarisari.manager.domain.model.StoreAlreadyOpenException
import kotlinx.coroutines.flow.Flow

/** Read-only report queries. Voided sales are always excluded. */
@Dao
interface ReportDao {
    @Query(
        """
        SELECT COUNT(*) AS transactions,
               COALESCE(SUM(total_centavos), 0) AS revenue,
               COALESCE(SUM(total_cost_centavos), 0) AS cost
        FROM sales
        WHERE voided_at IS NULL AND created_at >= :start AND created_at < :end
        """
    )
    fun observeSalesTotals(start: Long, end: Long): Flow<SalesTotalsRow>

    @Query("SELECT COALESCE(SUM(amount_centavos), 0) FROM expenses WHERE spent_at >= :start AND spent_at < :end")
    fun observeExpenseTotal(start: Long, end: Long): Flow<Long>

    @Query(
        """
        SELECT i.product_id AS product_id, p.name AS product_name, p.base_unit_name AS base_unit_name,
               SUM(i.base_quantity) AS quantity, SUM(i.line_total_centavos) AS revenue
        FROM sale_items i
        JOIN sales s ON s.id = i.sale_id
        JOIN products p ON p.id = i.product_id
        WHERE s.voided_at IS NULL AND s.created_at >= :start AND s.created_at < :end
        GROUP BY i.product_id
        ORDER BY quantity DESC, revenue DESC
        LIMIT :limit
        """
    )
    fun observeTopProducts(start: Long, end: Long, limit: Int): Flow<List<TopProductRow>>
}

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses WHERE spent_at >= :start AND spent_at < :end ORDER BY spent_at DESC, id DESC")
    fun observeBetween(start: Long, end: Long): Flow<List<ExpenseEntity>>

    @Insert
    suspend fun insert(expense: ExpenseEntity): Long

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
abstract class StoreSessionDao {
    @Query("SELECT * FROM store_sessions ORDER BY (closed_at IS NULL) DESC, opened_at DESC, id DESC LIMIT 1")
    abstract fun observeLatest(): Flow<StoreSessionEntity?>

    @Query("SELECT COUNT(*) FROM store_sessions WHERE closed_at IS NULL")
    protected abstract suspend fun countOpen(): Int

    @Insert
    protected abstract suspend fun insert(session: StoreSessionEntity): Long

    @Query("UPDATE store_sessions SET closed_at = :closedAt, updated_at = :now WHERE id = :id AND closed_at IS NULL")
    abstract suspend fun close(id: Long, closedAt: Long, now: Long)

    /** Opens a new session unless one is already open. Only one session can be open at a time. */
    @Transaction
    open suspend fun open(openedAt: Long, now: Long): Long {
        if (countOpen() > 0) throw StoreAlreadyOpenException()
        return insert(StoreSessionEntity(openedAt = openedAt, closedAt = null, createdAt = now, updatedAt = now))
    }
}
