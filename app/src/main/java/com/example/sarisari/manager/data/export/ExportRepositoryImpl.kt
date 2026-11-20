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
                    CsvWriter.money(maxOf(0, row.stockQuantity).toLong() * row.costCentavos),
                    CsvWriter.text(if (row.isArchived) "Archived" else "Active"),
                )
            }
            count += page.size
            after = page.last().id
        }
        return count
    }

    private companion object {
        const val PAGE_SIZE = 500
        val DATE_TIME: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.ENGLISH).withZone(StoreTime.ZONE)
    }
}
