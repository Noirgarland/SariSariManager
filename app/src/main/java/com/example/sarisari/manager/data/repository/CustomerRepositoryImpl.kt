package com.example.sarisari.manager.data.repository

import android.database.sqlite.SQLiteConstraintException
import com.example.sarisari.manager.data.local.dao.CustomerDao
import com.example.sarisari.manager.data.local.entity.CreditEntryEntity
import com.example.sarisari.manager.data.local.entity.CustomerEntity
import com.example.sarisari.manager.data.local.entity.CustomerSummaryRow
import com.example.sarisari.manager.domain.model.CreditEntry
import com.example.sarisari.manager.domain.model.CreditEntryType
import com.example.sarisari.manager.domain.model.Customer
import com.example.sarisari.manager.domain.model.DuplicateNameException
import com.example.sarisari.manager.domain.repository.CustomerRepository
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CustomerRepositoryImpl @Inject constructor(
    private val dao: CustomerDao,
    private val clock: Clock,
) : CustomerRepository {

    override fun observeCustomers(query: String): Flow<List<Customer>> =
        dao.observeSummaries(escapeLike(query.trim())).map { rows -> rows.map { it.toDomain() } }

    override fun observeCustomer(id: Long): Flow<Customer?> = dao.observeSummary(id).map { it?.toDomain() }

    override fun observeEntries(customerId: Long): Flow<List<CreditEntry>> =
        dao.observeEntries(customerId).map { rows -> rows.map { it.toDomain() } }

    override fun observeOutstandingTotal(): Flow<Long> = dao.observeOutstandingTotal()

    override fun observeCollected(startMillis: Long, endMillis: Long): Flow<Long> =
        dao.observeCollected(startMillis, endMillis)

    override suspend fun addCustomer(name: String): Long {
        val now = clock.millis()
        return try {
            dao.insertCustomer(CustomerEntity(name = name, createdAt = now, updatedAt = now))
        } catch (_: SQLiteConstraintException) {
            throw DuplicateNameException(name)
        }
    }

    override suspend fun renameCustomer(id: Long, name: String) {
        try {
            dao.renameCustomer(id, name, clock.millis())
        } catch (_: SQLiteConstraintException) {
            throw DuplicateNameException(name)
        }
    }

    override suspend fun recordPayment(customerId: Long, amountCentavos: Long, paidAtMillis: Long, note: String?): Long =
        dao.recordPayment(customerId, amountCentavos, paidAtMillis, note, clock.millis())

    override suspend fun voidPayment(entryId: Long) = dao.voidPayment(entryId, clock.millis())

    private fun CustomerSummaryRow.toDomain() = Customer(id, name, balanceCentavos, lastCreditAt, lastPaymentAt)

    private fun CreditEntryEntity.toDomain() = CreditEntry(
        id = id,
        customerId = customerId,
        type = CreditEntryType.valueOf(type),
        amountCentavos = amountCentavos,
        saleId = saleId,
        note = note,
        occurredAtMillis = occurredAt,
        voidedAtMillis = voidedAt,
    )

    private fun escapeLike(text: String): String =
        text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
}
