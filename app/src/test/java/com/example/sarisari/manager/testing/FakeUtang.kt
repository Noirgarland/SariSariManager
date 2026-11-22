package com.example.sarisari.manager.testing

import com.example.sarisari.manager.domain.model.CreditEntry
import com.example.sarisari.manager.domain.model.CreditEntryNotVoidableException
import com.example.sarisari.manager.domain.model.CreditEntryType
import com.example.sarisari.manager.domain.model.Customer
import com.example.sarisari.manager.domain.model.CustomerNotFoundException
import com.example.sarisari.manager.domain.model.DuplicateNameException
import com.example.sarisari.manager.domain.model.PaymentExceedsBalanceException
import com.example.sarisari.manager.domain.repository.CustomerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/** In-memory customers and ledger, following the same balance rules as the Room implementation. */
class FakeCustomerRepository : CustomerRepository {
    private val names = MutableStateFlow<Map<Long, String>>(emptyMap())
    val entries = MutableStateFlow<List<CreditEntry>>(emptyList())
    private var nextId = 1L

    private fun summary(id: Long, name: String, all: List<CreditEntry>): Customer {
        val mine = all.filter { it.customerId == id }
        return Customer(
            id = id,
            name = name,
            balanceCentavos = mine.sumOf { it.amountCentavos },
            lastCreditAtMillis = mine.filter { it.type == CreditEntryType.CREDIT_SALE }.maxOfOrNull { it.occurredAtMillis },
            lastPaymentAtMillis = mine.filter { it.type == CreditEntryType.PAYMENT && !it.isVoided }.maxOfOrNull { it.occurredAtMillis },
        )
    }

    override fun observeCustomers(query: String): Flow<List<Customer>> = combine(names, entries) { n, all ->
        n.filter { it.value.contains(query.trim(), ignoreCase = true) }
            .map { summary(it.key, it.value, all) }
            .sortedWith(compareByDescending<Customer> { it.balanceCentavos }.thenBy { it.name.lowercase() })
    }

    override fun observeCustomer(id: Long): Flow<Customer?> =
        combine(names, entries) { n, all -> n[id]?.let { summary(id, it, all) } }

    override fun observeEntries(customerId: Long): Flow<List<CreditEntry>> =
        entries.map { all -> all.filter { it.customerId == customerId }.sortedByDescending { it.id } }

    override fun observeOutstandingTotal(): Flow<Long> = entries.map { all ->
        all.groupBy { it.customerId }.values.sumOf { list -> maxOf(0L, list.sumOf { it.amountCentavos }) }
    }

    override fun observeCollected(startMillis: Long, endMillis: Long): Flow<Long> = entries.map { all ->
        all.filter { it.type == CreditEntryType.PAYMENT && !it.isVoided && it.occurredAtMillis in startMillis until endMillis }
            .sumOf { -it.amountCentavos }
    }

    override suspend fun addCustomer(name: String): Long {
        if (names.value.values.any { it.equals(name, ignoreCase = true) }) throw DuplicateNameException(name)
        val id = nextId++
        names.value = names.value + (id to name)
        return id
    }

    override suspend fun renameCustomer(id: Long, name: String) {
        if (names.value.any { it.key != id && it.value.equals(name, ignoreCase = true) }) throw DuplicateNameException(name)
        names.value = names.value + (id to name)
    }

    /** Adds an utang sale entry, as checkout would. */
    fun addCredit(customerId: Long, amountCentavos: Long, atMillis: Long) {
        entries.value = entries.value + CreditEntry(
            entries.value.size + 1L, customerId, CreditEntryType.CREDIT_SALE, amountCentavos, null, null, atMillis, null,
        )
    }

    override suspend fun recordPayment(customerId: Long, amountCentavos: Long, paidAtMillis: Long, note: String?): Long {
        if (customerId !in names.value) throw CustomerNotFoundException(customerId)
        val balance = entries.value.filter { it.customerId == customerId }.sumOf { it.amountCentavos }
        if (amountCentavos > balance) throw PaymentExceedsBalanceException(balance, amountCentavos)
        val id = entries.value.size + 1L
        entries.value = entries.value + CreditEntry(id, customerId, CreditEntryType.PAYMENT, -amountCentavos, null, note, paidAtMillis, null)
        return id
    }

    override suspend fun voidPayment(entryId: Long) {
        val entry = entries.value.firstOrNull { it.id == entryId }
        if (entry == null || entry.type != CreditEntryType.PAYMENT || entry.isVoided) throw CreditEntryNotVoidableException(entryId)
        entries.value = entries.value.map { if (it.id == entryId) it.copy(voidedAtMillis = 1) else it } +
            CreditEntry(entries.value.size + 1L, entry.customerId, CreditEntryType.PAYMENT_VOID, -entry.amountCentavos, null, null, 1, null)
    }
}
