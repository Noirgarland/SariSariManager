package com.example.sarisari.manager.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.sarisari.manager.data.local.database.AppDatabase
import com.example.sarisari.manager.data.local.entity.ProductEntity
import com.example.sarisari.manager.data.repository.ExpenseRepositoryImpl
import com.example.sarisari.manager.data.repository.InventoryRepositoryImpl
import com.example.sarisari.manager.data.repository.ReportRepositoryImpl
import com.example.sarisari.manager.data.repository.SalesRepositoryImpl
import com.example.sarisari.manager.data.repository.StoreSessionRepositoryImpl
import com.example.sarisari.manager.domain.model.ExpenseCategory
import com.example.sarisari.manager.domain.model.SaleLineInput
import com.example.sarisari.manager.domain.model.StoreSession
import com.example.sarisari.manager.domain.usecase.AddExpenseResult
import com.example.sarisari.manager.domain.usecase.AddExpenseUseCase
import com.example.sarisari.manager.domain.usecase.ExpenseError
import com.example.sarisari.manager.domain.usecase.ReceiveStockUseCase
import com.example.sarisari.manager.domain.usecase.ReportPeriod
import com.example.sarisari.manager.domain.usecase.StoreSessionResult
import com.example.sarisari.manager.domain.usecase.StoreSessionUseCase
import com.example.sarisari.manager.testing.FakeClock
import com.example.sarisari.manager.testing.FakeSettingsRepository
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.SQLiteMode

/** LEGACY SQLite mode: the native runtime loader fails when the user home path contains spaces. */
@RunWith(RobolectricTestRunner::class)
@SQLiteMode(SQLiteMode.Mode.LEGACY)
class ReportDaoTest {
    private lateinit var db: AppDatabase
    private val clock = FakeClock(Instant.parse("2025-11-18T02:00:00Z")) // 10:00 AM Nov 18 in Manila
    private var coke = 0L
    private var chips = 0L
    private val today = ReportPeriod.Day(LocalDate.of(2025, 11, 18))

    private val reports get() = ReportRepositoryImpl(db.reportDao())
    private val sales get() = SalesRepositoryImpl(db.saleDao(), clock)

    @Before
    fun setUp() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        coke = addProduct("Coke", 6500)
        chips = addProduct("Piattos", 2000)
        val receive = ReceiveStockUseCase(InventoryRepositoryImpl(db.stockMovementDao(), clock), FakeSettingsRepository())
        receive(coke, 1, 50, 4500, null)
        receive(chips, 1, 50, 1200, null)
    }

    @After
    fun tearDown() = db.close()

    private suspend fun addProduct(name: String, price: Long) = db.productDao().save(
        ProductEntity(
            name = name, categoryId = null, baseUnitName = "piece", costCentavos = 0, sellingPriceCentavos = price,
            stockQuantity = 0, lowStockThreshold = 3, isArchived = false, createdAt = 0, updatedAt = 0,
        ),
        emptyList(), now = 1,
    )

    private suspend fun sell(at: String, vararg lines: SaleLineInput): Long {
        clock.set(Instant.parse(at))
        val total = lines.sumOf { it.lineTotalCentavos }
        return sales.completeSale(lines.toList(), total).id
    }

    @Test
    fun summaryCountsOnlyNonVoidedSalesInsideTheManilaDay() = runTest {
        sell("2025-11-17T15:59:59Z", SaleLineInput(coke, "piece", 1, 1, 6500)) // 11:59 PM Nov 17 Manila
        sell("2025-11-17T16:00:00Z", SaleLineInput(coke, "piece", 1, 2, 6500)) // midnight Nov 18 Manila
        val voided = sell("2025-11-18T05:00:00Z", SaleLineInput(chips, "piece", 1, 5, 2000))
        sell("2025-11-18T06:00:00Z", SaleLineInput(chips, "piece", 1, 3, 2000), SaleLineInput(coke, "piece", 1, 1, 6000))
        sales.voidSale(voided, null)

        val summary = reports.observeSummary(today.startMillis, today.endMillis).first()
        assertEquals(2, summary.transactionCount)
        assertEquals(2 * 6500L + 3 * 2000L + 6000L, summary.revenueCentavos)
        assertEquals(3 * 4500L + 3 * 1200L, summary.costOfGoodsSoldCentavos)
        assertEquals(25_000L - 17_100L, summary.grossProfitCentavos)
    }

    @Test
    fun expensesReduceNetProfitButNotGrossProfit() = runTest {
        sell("2025-11-18T03:00:00Z", SaleLineInput(coke, "piece", 1, 10, 6500))
        val expenses = ExpenseRepositoryImpl(db.expenseDao(), clock)
        val add = AddExpenseUseCase(expenses, clock)
        clock.set(Instant.parse("2025-11-18T08:00:00Z"))
        assertEquals(AddExpenseResult.Saved, add(ExpenseCategory.ELECTRICITY, " Meralco ", 15_000, clock.millis()))
        add(ExpenseCategory.RENT, null, 99_999, Instant.parse("2025-11-19T01:00:00Z").toEpochMilli().let { clock.millis() - 86_400_000L * 2 })

        val summary = reports.observeSummary(today.startMillis, today.endMillis).first()
        assertEquals(65_000L - 45_000L, summary.grossProfitCentavos)
        assertEquals(15_000L, summary.expensesCentavos)
        assertEquals(20_000L - 15_000L, summary.netProfitCentavos)
        val listed = expenses.observeExpenses(today.startMillis, today.endMillis).first().single()
        assertEquals("Meralco", listed.description)
        expenses.deleteExpense(listed.id)
        assertEquals(0L, reports.observeSummary(today.startMillis, today.endMillis).first().expensesCentavos)
    }

    @Test
    fun stockPurchasesAreNotCountedAsExpenses() = runTest {
        val month = ReportPeriod.Month(java.time.YearMonth.of(2025, 11))
        assertEquals(0L, reports.observeSummary(month.startMillis, month.endMillis).first().expensesCentavos)
    }

    @Test
    fun expenseValidation() = runTest {
        val add = AddExpenseUseCase(ExpenseRepositoryImpl(db.expenseDao(), clock), clock)
        assertEquals(AddExpenseResult.Invalid(ExpenseError.AMOUNT_INVALID), add(ExpenseCategory.OTHER, null, null, clock.millis()))
        assertEquals(AddExpenseResult.Invalid(ExpenseError.AMOUNT_INVALID), add(ExpenseCategory.OTHER, null, 0, clock.millis()))
        assertEquals(AddExpenseResult.Invalid(ExpenseError.DESCRIPTION_TOO_LONG), add(ExpenseCategory.OTHER, "x".repeat(121), 100, clock.millis()))
        assertEquals(AddExpenseResult.Invalid(ExpenseError.DATE_IN_FUTURE), add(ExpenseCategory.OTHER, null, 100, clock.millis() + 1))
    }

    @Test
    fun topProductsKeepHistoricalNamesAndUnitsAfterProductEdits() = runTest {
        sell("2025-11-18T03:00:00Z", SaleLineInput(coke, "piece", 1, 3, 6500))
        val product = db.productDao().getWithUnits(coke)!!.product
        db.productDao().save(product.copy(name = "Coca-Cola 1.5L", baseUnitName = "bottle"), emptyList(), now = 9)
        val before = reports.observeTopProducts(today.startMillis, today.endMillis, 10).first().single()
        assertEquals("Coke", before.productName)
        assertEquals("piece", before.baseUnitName)
        assertEquals(3, before.baseQuantity)

        // A later sale under the new name and unit is listed separately, so quantities in different units are not summed.
        sell("2025-11-18T04:00:00Z", SaleLineInput(coke, "bottle", 1, 1, 6500))
        val after = reports.observeTopProducts(today.startMillis, today.endMillis, 10).first()
        assertEquals(listOf(Triple("Coke", "piece", 3), Triple("Coca-Cola 1.5L", "bottle", 1)), after.map { Triple(it.productName, it.baseUnitName, it.baseQuantity) })
    }

    @Test
    fun topProductsRankByQuantityAndSkipVoidedSales() = runTest {
        sell("2025-11-18T03:00:00Z", SaleLineInput(chips, "piece", 1, 4, 2000))
        sell("2025-11-18T04:00:00Z", SaleLineInput(coke, "piece", 1, 2, 6500), SaleLineInput(chips, "piece", 1, 1, 2000))
        val voided = sell("2025-11-18T05:00:00Z", SaleLineInput(coke, "pack", 6, 1, 36_000))
        sales.voidSale(voided, null)
        val top = reports.observeTopProducts(today.startMillis, today.endMillis, 5).first()
        assertEquals(listOf("Piattos", "Coke"), top.map { it.productName })
        assertEquals(listOf(5, 2), top.map { it.baseQuantity })
        assertEquals(listOf(10_000L, 13_000L), top.map { it.revenueCentavos })
    }

    @Test
    fun storeSessionsOpenOnceAndCloseWithValidTimes() = runTest {
        val repo = StoreSessionRepositoryImpl(db.storeSessionDao(), clock)
        val useCase = StoreSessionUseCase(repo, clock)
        assertNull(repo.observeLatestSession().first())
        assertEquals(StoreSessionResult.DONE, useCase.open())
        assertEquals(StoreSessionResult.ALREADY_OPEN, useCase.open())
        val open = repo.observeLatestSession().first()!!
        assertTrue(open.isOpen)
        assertEquals(StoreSessionResult.BEFORE_OPENING, useCase.close(open, open.openedAtMillis - 1))
        assertEquals(StoreSessionResult.TIME_IN_FUTURE, useCase.close(open, clock.millis() + 1))
        clock.set(Instant.parse("2025-11-18T13:00:00Z"))
        val forgottenClose = Instant.parse("2025-11-18T11:00:00Z").toEpochMilli()
        assertEquals(StoreSessionResult.DONE, useCase.close(open, forgottenClose))
        val closed = repo.observeLatestSession().first()!!
        assertEquals(forgottenClose, closed.closedAtMillis)
        assertEquals(StoreSessionResult.NOT_OPEN, useCase.close(closed))
        assertEquals(StoreSessionResult.DONE, useCase.open())
        assertEquals(StoreSession::class, repo.observeLatestSession().first()!!::class)
        assertTrue(repo.observeLatestSession().first()!!.isOpen)
    }
}
