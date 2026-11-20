package com.example.sarisari.manager.testing

import com.example.sarisari.manager.domain.model.Expense
import com.example.sarisari.manager.domain.model.ExpenseCategory
import com.example.sarisari.manager.domain.model.ReportSummary
import com.example.sarisari.manager.domain.model.StoreAlreadyOpenException
import com.example.sarisari.manager.domain.model.StoreSession
import com.example.sarisari.manager.domain.model.TopProduct
import com.example.sarisari.manager.domain.repository.ExpenseRepository
import com.example.sarisari.manager.domain.repository.ReportRepository
import com.example.sarisari.manager.domain.repository.StoreSessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** Returns the summary registered for a start time, or an empty one. */
class FakeReportRepository : ReportRepository {
    val summaries = MutableStateFlow<Map<Long, ReportSummary>>(emptyMap())
    val requestedRanges = mutableListOf<Pair<Long, Long>>()

    override fun observeSummary(startMillis: Long, endMillis: Long): Flow<ReportSummary> {
        requestedRanges += startMillis to endMillis
        return summaries.map { it[startMillis] ?: ReportSummary() }
    }

    override fun observeTopProducts(startMillis: Long, endMillis: Long, limit: Int): Flow<List<TopProduct>> =
        MutableStateFlow(emptyList())
}

class FakeExpenseRepository : ExpenseRepository {
    val expenses = MutableStateFlow<List<Expense>>(emptyList())

    override fun observeExpenses(startMillis: Long, endMillis: Long): Flow<List<Expense>> =
        expenses.map { list -> list.filter { it.spentAtMillis in startMillis until endMillis } }

    override suspend fun addExpense(category: ExpenseCategory, description: String?, amountCentavos: Long, spentAtMillis: Long): Long {
        val id = expenses.value.size + 1L
        expenses.value = expenses.value + Expense(id, category, description, amountCentavos, spentAtMillis)
        return id
    }

    override suspend fun deleteExpense(id: Long) {
        expenses.value = expenses.value.filterNot { it.id == id }
    }
}

class FakeStoreSessionRepository : StoreSessionRepository {
    val latest = MutableStateFlow<StoreSession?>(null)

    override fun observeLatestSession(): Flow<StoreSession?> = latest

    override suspend fun open(openedAtMillis: Long): Long {
        if (latest.value?.isOpen == true) throw StoreAlreadyOpenException()
        val id = (latest.value?.id ?: 0) + 1
        latest.value = StoreSession(id, openedAtMillis, null)
        return id
    }

    override suspend fun close(id: Long, closedAtMillis: Long) {
        latest.value = latest.value?.takeIf { it.id == id }?.copy(closedAtMillis = closedAtMillis) ?: latest.value
    }
}
