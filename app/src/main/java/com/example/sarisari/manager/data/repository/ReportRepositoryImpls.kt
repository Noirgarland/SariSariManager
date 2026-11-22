package com.example.sarisari.manager.data.repository

import com.example.sarisari.manager.data.local.dao.ExpenseDao
import com.example.sarisari.manager.data.local.dao.ReportDao
import com.example.sarisari.manager.data.local.dao.StoreSessionDao
import com.example.sarisari.manager.data.local.entity.ExpenseEntity
import com.example.sarisari.manager.domain.model.Expense
import com.example.sarisari.manager.domain.model.ExpenseCategory
import com.example.sarisari.manager.domain.model.ReportSummary
import com.example.sarisari.manager.domain.model.StoreSession
import com.example.sarisari.manager.domain.model.TopProduct
import com.example.sarisari.manager.domain.repository.ExpenseRepository
import com.example.sarisari.manager.domain.repository.ReportRepository
import com.example.sarisari.manager.domain.repository.StoreSessionRepository
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class ReportRepositoryImpl @Inject constructor(
    private val dao: ReportDao,
) : ReportRepository {
    override fun observeSummary(startMillis: Long, endMillis: Long): Flow<ReportSummary> =
        combine(
            dao.observeSalesTotals(startMillis, endMillis),
            dao.observeExpenseTotal(startMillis, endMillis),
        ) { sales, expenses ->
            ReportSummary(
                revenueCentavos = sales.revenue,
                costOfGoodsSoldCentavos = sales.cost,
                expensesCentavos = expenses,
                transactionCount = sales.transactions,
                creditSalesCentavos = sales.credit,
            )
        }

    override fun observeTopProducts(startMillis: Long, endMillis: Long, limit: Int): Flow<List<TopProduct>> =
        dao.observeTopProducts(startMillis, endMillis, limit).map { rows ->
            rows.map { TopProduct(it.productId, it.productName, it.baseUnitName, it.quantity, it.revenue) }
        }
}

class ExpenseRepositoryImpl @Inject constructor(
    private val dao: ExpenseDao,
    private val clock: Clock,
) : ExpenseRepository {
    override fun observeExpenses(startMillis: Long, endMillis: Long): Flow<List<Expense>> =
        dao.observeBetween(startMillis, endMillis).map { list ->
            list.map {
                Expense(it.id, ExpenseCategory.fromStorage(it.category), it.description, it.amountCentavos, it.spentAt)
            }
        }

    override suspend fun addExpense(
        category: ExpenseCategory,
        description: String?,
        amountCentavos: Long,
        spentAtMillis: Long,
    ): Long {
        val now = clock.millis()
        return dao.insert(ExpenseEntity(0, category.name, description, amountCentavos, spentAtMillis, now, now))
    }

    override suspend fun deleteExpense(id: Long) = dao.delete(id)
}

class StoreSessionRepositoryImpl @Inject constructor(
    private val dao: StoreSessionDao,
    private val clock: Clock,
) : StoreSessionRepository {
    override fun observeLatestSession(): Flow<StoreSession?> =
        dao.observeLatest().map { it?.let { s -> StoreSession(s.id, s.openedAt, s.closedAt) } }

    override suspend fun open(openedAtMillis: Long): Long = dao.open(openedAtMillis, clock.millis())

    override suspend fun close(id: Long, closedAtMillis: Long) = dao.close(id, closedAtMillis, clock.millis())
}
