package com.example.sarisari.manager.data.export

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.sarisari.manager.data.local.database.AppDatabase
import com.example.sarisari.manager.data.local.entity.CategoryEntity
import com.example.sarisari.manager.data.local.entity.ProductEntity
import com.example.sarisari.manager.data.repository.SalesRepositoryImpl
import com.example.sarisari.manager.domain.model.SaleLineInput
import com.example.sarisari.manager.testing.FakeClock
import java.io.StringWriter
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.SQLiteMode

class CsvWriterTest {
    private fun line(vararg cells: CsvWriter.Cell) = StringWriter().also { CsvWriter(it).row(*cells) }.toString()

    @Test
    fun escapesCommasQuotesAndLineBreaks() {
        assertEquals("plain", CsvWriter.escape("plain"))
        assertEquals("\"a,b\"", CsvWriter.escape("a,b"))
        assertEquals("\"say \"\"hi\"\"\"", CsvWriter.escape("say \"hi\""))
        assertEquals("\"two\nlines\"", CsvWriter.escape("two\nlines"))
        assertEquals("\"cr\rhere\"", CsvWriter.escape("cr\rhere"))
        assertEquals("", CsvWriter.escape(""))
    }

    @Test
    fun protectsTextThatLooksLikeAFormula() {
        assertEquals("'=SUM(A1:A9)", CsvWriter.protectFormula("=SUM(A1:A9)"))
        assertEquals("'+63 917", CsvWriter.protectFormula("+63 917"))
        assertEquals("'-cheap", CsvWriter.protectFormula("-cheap"))
        assertEquals("'@cmd", CsvWriter.protectFormula("@cmd"))
        assertEquals("'\tx", CsvWriter.protectFormula("\tx"))
        assertEquals("Coke = good", CsvWriter.protectFormula("Coke = good"))
        // A protected value with a comma is also quoted.
        assertEquals("\"'=1,2\"\r\n", line(CsvWriter.text("=1,2")))
    }

    @Test
    fun numbersAndMoneyAreWrittenAsIsWithIntegerFormatting() {
        assertEquals("-2,1234.50,0.05,-12.30\r\n", line(CsvWriter.number(-2), CsvWriter.money(123_450), CsvWriter.money(5), CsvWriter.money(-1230)))
        assertEquals("0.00", CsvWriter.formatMoney(0))
    }

    @Test
    fun rowsUseCrlfAndTheFileStartsWithAByteOrderMark() {
        val out = StringWriter()
        CsvWriter(out).apply {
            writeByteOrderMark()
            header("Product", "Price")
            row(CsvWriter.text("Piattos \u20B1"), CsvWriter.money(2000))
        }
        assertEquals("\uFEFFProduct,Price\r\nPiattos \u20B1,20.00\r\n", out.toString())
    }
}

/** LEGACY SQLite mode: the native runtime loader fails when the user home path contains spaces. */
@RunWith(RobolectricTestRunner::class)
@SQLiteMode(SQLiteMode.Mode.LEGACY)
class ExportRepositoryTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun salesAndProductsAreExportedWithSnapshotsAndAllPages() = runTest {
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        try {
            val drinks = db.categoryDao().insert(CategoryEntity(name = "Drinks", createdAt = 1, updatedAt = 1))
            val cokeId = db.productDao().save(
                ProductEntity(
                    name = "=Coke, 1.5L", categoryId = drinks, baseUnitName = "bottle", costCentavos = 4500,
                    sellingPriceCentavos = 6500, stockQuantity = 0, lowStockThreshold = 5, isArchived = false,
                    createdAt = 0, updatedAt = 0,
                ),
                emptyList(), now = 1,
            )
            val clock = FakeClock(Instant.parse("2025-11-18T02:30:00Z"))
            val sales = SalesRepositoryImpl(db.saleDao(), clock)
            repeat(600) { sales.completeSale(listOf(SaleLineInput(cokeId, "bottle", 1, 1, 6000)), 6000) }
            sales.voidSale(1, null)

            val exporter = ExportRepositoryImpl(context, db.exportDao(), Dispatchers.Unconfined)
            val out = StringWriter()
            val rows = exporter.writeSales(out, 0, Long.MAX_VALUE)
            assertEquals(600, rows)
            val lines = out.toString().removePrefix("\uFEFF").split("\r\n").filter { it.isNotEmpty() }
            assertEquals(601, lines.size)
            assertEquals("1,2025-11-18 10:30,Voided,\"'=Coke, 1.5L\",bottle,1,1,60.00,60.00,45.00,15.00", lines[1])
            assertTrue(lines[2].contains(",Completed,"))

            val productsOut = StringWriter()
            assertEquals(1, exporter.writeProducts(productsOut))
            val productLine = productsOut.toString().split("\r\n")[1]
            // 600 sold from 0 stock and one sale voided: stock is -599, so stock value is 0.
            assertEquals("\"'=Coke, 1.5L\",Drinks,bottle,45.00,65.00,-599,5,0.00,Active", productLine)
        } finally {
            db.close()
        }
    }
}
