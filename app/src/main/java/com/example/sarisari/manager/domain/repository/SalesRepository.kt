package com.example.sarisari.manager.domain.repository

import com.example.sarisari.manager.domain.model.CompletedSale
import com.example.sarisari.manager.domain.model.Sale
import com.example.sarisari.manager.domain.model.SaleLineInput
import kotlinx.coroutines.flow.Flow

interface SalesRepository {
    /**
     * Saves the sale, its items with cost snapshots, and the stock deductions in one transaction.
     * Stock is checked inside the transaction and may never go below zero.
     * @throws com.example.sarisari.manager.domain.model.ProductUnavailableException for a missing or archived product.
     * @throws com.example.sarisari.manager.domain.model.InsufficientCheckoutStockException when stock cannot cover the cart.
     */
    suspend fun completeSale(lines: List<SaleLineInput>, amountReceivedCentavos: Long): CompletedSale

    /** Newest first. Voided sales are included. */
    fun observeSales(limit: Int = 200): Flow<List<Sale>>

    fun observeSale(id: Long): Flow<Sale?>

    /**
     * Marks the sale voided and returns its stock through compensating movements, in one transaction.
     * @throws com.example.sarisari.manager.domain.model.SaleNotFoundException
     * @throws com.example.sarisari.manager.domain.model.SaleAlreadyVoidedException
     * @throws com.example.sarisari.manager.domain.model.StockLimitExceededException
     */
    suspend fun voidSale(id: Long, reason: String?)
}
