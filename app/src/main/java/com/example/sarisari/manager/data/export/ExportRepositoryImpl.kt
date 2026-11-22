package com.example.sarisari.manager.data.export

import android.content.Context
import android.net.Uri
import com.example.sarisari.manager.data.local.dao.ExportDao
import com.example.sarisari.manager.di.IoDispatcher
import com.example.sarisari.manager.domain.model.ExportOutcome
import com.example.sarisari.manager.domain.model.StoreTime
import com.example.sarisari.manager.domain.repository.ExportRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import java.io.Writer
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

class ExportRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val dao: ExportDao,
    @param:IoDispatcher private val io: CoroutineDispatcher,
) : ExportRepository {

    override suspend fun exportSales(destinationUri: String, startMillis: Long, endMillis: Long): ExportOutcome =
        writeTo(destinationUri) { writeSales(it, startMillis, endMillis) }

    override suspend fun exportProducts(destinationUri: String): ExportOutcome =
        writeTo(destinationUri) { writeProducts(it) }

    override suspend fun exportUtangBalances(destinationUri: String): ExportOutcome =
        writeTo(destinationUri) { writeUtangBalances(it) }

    override suspend fun exportUtangHistory(destinationUri: String): ExportOutcome =
        writeTo(destinationUri) { writeUtangHistory(it) }

    private suspend fun writeTo(destinationUri: String, write: suspend (Writer) -> Int): ExportOutcome = withContext(io) {
        try {
            val stream = context.contentResolver.openOutputStream(Uri.parse(destinationUri), "wt")
                ?: return@withContext ExportOutcome.Failure
            val rows = stream.bufferedWriter(Charsets.UTF_8).use { write(it) }
            ExportOutcome.Success(rows)
        } catch (_: IOException) {
            ExportOutcome.Failure
        } catch (_: SecurityException) {
            ExportOutcome.Failure
        }
    }

    /** Writes one row per sale item. Returns the number of data rows. Visible for tests. */
    internal suspend fun writeSales(writer: Writer, startMillis: Long, endMillis: Long): Int {
        val csv = CsvWriter(writer)
        csv.writeByteOrderMark()
        csv.header(
            "Sale number", "Date and time", "Status", "Product", "Unit", "Quantity", "Base quantity",
            "Unit price", "Line total", "Line cost", "Gross profit",
        )
        var after = 0L
        var count = 0
        while (true) {
            val page = dao.saleItemsPage(startMillis, endMillis, after, PAGE_SIZE)
            if (page.isEmpty()) break
            page.forEach { row ->
                csv.row(
                    CsvWriter.number(row.saleId),
                    CsvWriter.text(DATE_TIME.format(Instant.ofEpochMilli(row.createdAt))),
                    CsvWriter.text(if (row.voidedAt == null) "Completed" else "Voided"),
                    CsvWriter.text(row.productName),
                    CsvWriter.text(row.unitName),
                    CsvWriter.number(row.quantity),
                    CsvWriter.number(row.baseQuantity),
                    CsvWriter.money(row.unitPriceCentavos),
                    CsvWriter.money(row.lineTotalCentavos),
                    CsvWriter.money(row.lineCostCentavos),
                    CsvWriter.money(row.lineTotalCentavos - row.lineCostCentavos),
                )
            }
            count += page.size
            after = page.last().itemId
        }
        return count
    }

    /** Writes every product, including archived ones. Returns the number of data rows. Visible for tests. */
    internal suspend fun writeProducts(writer: Writer): Int {
        val csv = CsvWriter(writer)
        csv.writeByteOrderMark()
        csv.header(
            "Product", "Category", "Unit", "Cost per unit", "Selling price", "Stock", "Low-stock level",
            "Stock value at cost", "Status",
        )
        var after = 0L
        var count = 0
        while (true) {
            val page = dao.productsPage(after, PAGE_SIZE)
            if (page.isEmpty()) break
            page.forEach { row ->
                csv.row(
                    CsvWriter.text(row.name),
                    CsvWriter.text(row.categoryName),
                    CsvWriter.text(row.baseUnitName),
                    CsvWriter.money(row.costCentavos),
                    CsvWriter.money(row.sellingPriceCentavos),
                    CsvWriter.number(row.stockQuantity),
                    CsvWriter.number(row.lowStockThreshold),
                    CsvWriter.moneyTimes(maxOf(0, row.stockQuantity).toLong(), row.costCentavos),
                    CsvWriter.text(if (row.isArchived) "Archived" else "Active"),
                )
            }
            count += page.size
            after = page.last().id
        }
        return count
    }

    /** Writes every customer with what they owe. Returns the number of data rows. Visible for tests. */
    internal suspend fun writeUtangBalances(writer: Writer): Int {
        val csv = CsvWriter(writer)
        csv.writeByteOrderMark()
        csv.header("Customer", "Balance", "Status", "Last utang date", "Last payment date")
        var after = 0L
        var count = 0
        while (true) {
            val page = dao.customersPage(after, PAGE_SIZE)
            if (page.isEmpty()) break
            page.forEach { row ->
                csv.row(
                    CsvWriter.text(row.name),
                    CsvWriter.money(row.balanceCentavos),
                    CsvWriter.text(if (row.balanceCentavos > 0) "Unpaid" else "Paid"),
                    CsvWriter.text(row.lastCreditAt?.let { DATE.format(Instant.ofEpochMilli(it)) }),
                    CsvWriter.text(row.lastPaymentAt?.let { DATE.format(Instant.ofEpochMilli(it)) }),
                )
            }
            count += page.size
            after = page.last().id
        }
        return count
    }

    /** Writes every utang ledger entry, oldest first. Returns the number of data rows. Visible for tests. */
    internal suspend fun writeUtangHistory(writer: Writer): Int {
        val csv = CsvWriter(writer)
        csv.writeByteOrderMark()
        csv.header("Date and time", "Customer", "Type", "Amount", "Balance after", "Sale number", "Note", "Status")
        var after = 0L
        var count = 0
        while (true) {
            val page = dao.creditEntriesPage(after, PAGE_SIZE)
            if (page.isEmpty()) break
            page.forEach { row ->
                csv.row(
                    CsvWriter.text(DATE_TIME.format(Instant.ofEpochMilli(row.occurredAt))),
                    CsvWriter.text(row.customerName),
                    CsvWriter.text(ENTRY_LABELS[row.type] ?: row.type),
                    CsvWriter.money(row.amountCentavos),
                    CsvWriter.money(row.balanceAfter),
                    CsvWriter.text(row.saleId?.toString()),
                    CsvWriter.text(row.note),
                    CsvWriter.text(if (row.voidedAt == null) "" else "Voided"),
                )
            }
            count += page.size
            after = page.last().id
        }
        return count
    }

    private companion object {
        val DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ENGLISH).withZone(StoreTime.ZONE)
        val ENTRY_LABELS = mapOf(
            "CREDIT_SALE" to "Utang sale",
            "PAYMENT" to "Payment",
            "SALE_VOID" to "Sale voided",
            "PAYMENT_VOID" to "Payment voided",
        )
        const val PAGE_SIZE = 500
        val DATE_TIME: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.ENGLISH).withZone(StoreTime.ZONE)
    }
}
