package com.example.sarisari.manager.data.local.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.sarisari.manager.data.repository.ReportRepositoryImpl
import com.example.sarisari.manager.data.repository.SalesRepositoryImpl
import com.example.sarisari.manager.testing.FakeClock
import com.example.sarisari.manager.testing.SchemaFixtures
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.SQLiteMode

/**
 * Builds a version 5 database from the committed 5.json schema with real sales,
 * then opens it with [AppDatabase] and [ALL_MIGRATIONS]. Room checks the migrated schema against version 6 on open.
 *
 * LEGACY SQLite mode: the native runtime loader fails when the user home path contains spaces.
 */
@RunWith(RobolectricTestRunner::class)
@SQLiteMode(SQLiteMode.Mode.LEGACY)
class Migration5To6Test {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val name = "migration-5-6-test.db"

    @After
    fun tearDown() {
        context.deleteDatabase(name)
    }

    private fun openMigrated(): AppDatabase {
        SchemaFixtures.createDatabase(context.getDatabasePath(name).apply { parentFile?.mkdirs() }, 5) {
            SchemaFixtures.fillVersion5SalesData(it)
        }
        return Room.databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(*ALL_MIGRATIONS)
            .allowMainThreadQueries()
            .build()
    }

    private fun AppDatabase.queryStrings(sql: String): List<String> =
        openHelper.readableDatabase.query(sql).use { c -> buildList { while (c.moveToNext()) add(c.getString(0)) } }

    @Test
    fun existingSalesStayAccessibleWithTheirSnapshots() = runTest {
        val db = openMigrated()
        try {
            val sales = SalesRepositoryImpl(db.saleDao(), FakeClock())
            val all = sales.observeSales().first()
            assertEquals(listOf(2L, 1L), all.map { it.id })
            assertTrue(all.first { it.id == 2L }.isVoided)

            val items = sales.observeSale(1).first()!!.items
            assertEquals(listOf("Coke", "Coke", "Chips (old name)"), items.map { it.productName })
            assertEquals(listOf("bottle", "case", "pack"), items.map { it.unitName })
            assertEquals(listOf(2, 12, 1), items.map { it.baseQuantity })
            assertEquals(listOf(13_000L, 70_000L, 2_000L), items.map { it.lineTotalCentavos })
            assertEquals(listOf(9_000L, 54_000L, 1_200L), items.map { it.lineCostCentavos })
            // Factor-1 lines keep the unit they were sold in. The chips line keeps "pack" even though
            // the product's base unit is now "piece". The case line falls back to the current base unit.
            assertEquals(listOf("bottle", "bottle", "pack"), items.map { it.baseUnitName })

            assertEquals("Aling Nena Store", db.appSettingDao().getByKey("store_name")?.value)
            assertEquals(6, db.stockMovementDao().sumQuantity(1))
        } finally {
            db.close()
        }
    }

    @Test
    fun migratedDatabasePassesIntegrityAndForeignKeyChecks() = runTest {
        val db = openMigrated()
        try {
            assertEquals(listOf("ok"), db.queryStrings("PRAGMA integrity_check"))
            assertTrue(db.queryStrings("PRAGMA foreign_key_check").isEmpty())
            assertEquals(4, db.queryStrings("SELECT id FROM sale_items").size)
            assertTrue(db.queryStrings("SELECT id FROM sale_items WHERE base_unit_name = ''").isEmpty())
        } finally {
            db.close()
        }
    }

    @Test
    fun reportsUseTheHistoricalNameAndUnit() = runTest {
        val db = openMigrated()
        try {
            val top = ReportRepositoryImpl(db.reportDao()).observeTopProducts(0, Long.MAX_VALUE, 10).first()
            // The voided sale is excluded. Coke: 2 + 12 bottles. Chips keep the name and unit from the sale.
            assertEquals(listOf("Coke" to "bottle", "Chips (old name)" to "pack"), top.map { it.productName to it.baseUnitName })
            assertEquals(listOf(14, 1), top.map { it.baseQuantity })
        } finally {
            db.close()
        }
    }

    @Test
    fun runningTheMigrationAgainChangesNothing() = runTest {
        val db = openMigrated()
        try {
            val before = db.queryStrings("SELECT base_unit_name FROM sale_items ORDER BY id")
            MIGRATION_5_6.migrate(db.openHelper.writableDatabase)
            assertEquals(before, db.queryStrings("SELECT base_unit_name FROM sale_items ORDER BY id"))
        } finally {
            db.close()
        }
    }
}
