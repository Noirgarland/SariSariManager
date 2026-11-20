package com.example.sarisari.manager.domain.repository

import com.example.sarisari.manager.domain.model.Expense
import com.example.sarisari.manager.domain.model.ExpenseCategory
import com.example.sarisari.manager.domain.model.ReportSummary
import com.example.sarisari.manager.domain.model.StoreSession
import com.example.sarisari.manager.domain.model.TopProduct
import kotlinx.coroutines.flow.Flow

/** Ranges are [startMillis, endMillis) in UTC epoch milliseconds. */
interface ReportRepository {
    fun observeSummary(startMillis: Long, endMillis: Long): Flow<ReportSummary>
    fun observeTopProducts(startMillis: Long, endMillis: Long, limit: Int): Flow<List<TopProduct>>
}

interface ExpenseRepository {
    /** Newest first. */
    fun observeExpenses(startMillis: Long, endMillis: Long): Flow<List<Expense>>
    suspend fun addExpense(category: ExpenseCategory, description: String?, amountCentavos: Long, spentAtMillis: Long): Long
    suspend fun deleteExpense(id: Long)
}

interface StoreSessionRepository {
    /** The open session, or the most recently closed one, or null if the store has never been opened. */
    fun observeLatestSession(): Flow<StoreSession?>

    /** @throws com.example.sarisari.manager.domain.model.StoreAlreadyOpenException */
    suspend fun open(openedAtMillis: Long): Long
    suspend fun close(id: Long, closedAtMillis: Long)
}
