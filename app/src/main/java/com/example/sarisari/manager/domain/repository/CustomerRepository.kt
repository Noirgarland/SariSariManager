package com.example.sarisari.manager.domain.repository

import com.example.sarisari.manager.domain.model.CreditEntry
import com.example.sarisari.manager.domain.model.Customer
import kotlinx.coroutines.flow.Flow

/** Customers who buy on utang, and their ledgers. Money is in centavos. */
interface CustomerRepository {
    /** Highest balance first, then by name. A blank [query] returns everyone. */
    fun observeCustomers(query: String = ""): Flow<List<Customer>>

    fun observeCustomer(id: Long): Flow<Customer?>

    /** Newest first. */
    fun observeEntries(customerId: Long): Flow<List<CreditEntry>>

    /** What all customers owe right now. */
    fun observeOutstandingTotal(): Flow<Long>

    /** Payments received in [startMillis, endMillis), not counting voided ones. */
    fun observeCollected(startMillis: Long, endMillis: Long): Flow<Long>

    /** @throws com.example.sarisari.manager.domain.model.DuplicateNameException */
    suspend fun addCustomer(name: String): Long

    /** @throws com.example.sarisari.manager.domain.model.DuplicateNameException */
    suspend fun renameCustomer(id: Long, name: String)

    /**
     * @throws com.example.sarisari.manager.domain.model.CustomerNotFoundException
     * @throws com.example.sarisari.manager.domain.model.PaymentExceedsBalanceException
     */
    suspend fun recordPayment(customerId: Long, amountCentavos: Long, paidAtMillis: Long, note: String?): Long

    /** @throws com.example.sarisari.manager.domain.model.CreditEntryNotVoidableException */
    suspend fun voidPayment(entryId: Long)
}
