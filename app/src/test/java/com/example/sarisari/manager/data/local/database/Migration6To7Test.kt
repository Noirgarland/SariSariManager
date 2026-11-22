package com.example.sarisari.manager.data.local.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.sarisari.manager.data.repository.CustomerRepositoryImpl
import com.example.sarisari.manager.data.repository.ReportRepositoryImpl
import com.example.sarisari.manager.data.repository.SalesRepositoryImpl
import com.example.sarisari.manager.domain.model.SaleLineInput
import com.example.sarisari.manager.testing.FakeClock
import com.example.sarisari.manager.testing.SchemaFixtures
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.SQLiteMode

/**
 * Builds a version 6 database from the committed 6.json schema with real sales, then opens it with
 * [ALL_MIGRATIONS]. Room checks the migrated schema against version 7 on open.
 *
 * LEGACY SQLite mode: the native runtime loader fails when the user home path contains spaces.
 */
@RunWith(RobolectricTestRunner::class)
@SQLiteMode(SQLiteMode.Mode.LEGACY)
class Migration6To7Test {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val name = "migration-6-7-test.db"
    private val clock = FakeClock().also { it.setMillis(10_000) }

    @After
    fun tearDown() {
        context.deleteDatabase(name)
    }

    private fun openMigrated(): AppDatabase {
        SchemaFixtures.createDatabase(context.getDatabasePath(name).apply { parentFile?.mkdirs() }, 6) { db ->
            db.execSQL("INSERT INTO products VALUES (1, 'Coke', NULL, 'bottle', 4500, 6500, 8, 5, 0, 1, 1)")
            db.execSQL("INSERT INTO stock_movements VALUES (1, 1, 'RECEIPT', 10, 4500, 45000, NULL, 1, 1)")
            db.execSQL("INSERT INTO stock_movements VALUES (2, 1, 'SALE', -2, NULL, NULL, 'Sale #1', 2, 2)")
            db.execSQL("INSERT INTO sales VALUES (1, 13000, 15000, 2000, 9000, 2, 2, NULL, NULL)")
            db.execSQL("INSERT INTO sale_items VALUES (1, 1, 1, 'Coke', 'bottle', 'bottle', 1, 2, 2, 6500, 13000, 4500, 9000, 2, 2)")
        }
        return Room.databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(*ALL_MIGRATIONS)
            .allowMainThreadQueries()
            .build()
    }

    private fun AppDatabase.queryStrings(sql: String): List<String> =
        openHelper.readableDatabase.query(sql).use { c -> buildList { while (c.moveToNext()) add(c.getString(0)) } }

    @Test
    fun existingSalesBecomeCashSalesAndStayIntact() = runTest {
        val db = openMigrated()
        try {
            val sale = SalesRepositoryImpl(db.saleDao(), clock).observeSale(1).first()!!
            assertEquals(13_000L, sale.totalCentavos)
            assertEquals(15_000L, sale.amountReceivedCentavos)
            assertEquals(2_000L, sale.changeCentavos)
            assertEquals(0L, sale.creditCentavos)
            assertNull(sale.customerId)
            assertEquals("Coke", sale.items.single().productName)

            val summary = ReportRepositoryImpl(db.reportDao()).observeSummary(0, Long.MAX_VALUE).first()
            assertEquals(13_000L, summary.revenueCentavos)
            assertEquals(13_000L, summary.cashSalesCentavos)
            assertEquals(0L, summary.creditSalesCentavos)
        } finally {
            db.close()
        }
    }

    @Test
    fun migratedDatabaseAcceptsCustomersAndUtangSales() = runTest {
        val db = openMigrated()
        try {
            assertEquals(listOf("ok"), db.queryStrings("PRAGMA integrity_check"))
            assertTrue(db.queryStrings("PRAGMA foreign_key_check").isEmpty())

            val customers = CustomerRepositoryImpl(db.customerDao(), clock)
            val juan = customers.addCustomer("Juan")
            SalesRepositoryImpl(db.saleDao(), clock).completeSale(listOf(SaleLineInput(1, "bottle", 1, 1, 6500)), 0, juan)
            assertEquals(6_500L, customers.observeCustomer(juan).first()!!.balanceCentavos)
        } finally {
            db.close()
        }
    }
}
