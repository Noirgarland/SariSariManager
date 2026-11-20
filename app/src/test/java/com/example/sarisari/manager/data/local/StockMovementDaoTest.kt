package com.example.sarisari.manager.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.sarisari.manager.data.local.database.AppDatabase
import com.example.sarisari.manager.data.local.entity.ProductEntity
import com.example.sarisari.manager.data.repository.InventoryRepositoryImpl
import com.example.sarisari.manager.domain.model.CostingMethod
import com.example.sarisari.manager.domain.model.InsufficientStockException
import com.example.sarisari.manager.domain.model.StockMovementType
import com.example.sarisari.manager.domain.usecase.AdjustStockUseCase
import com.example.sarisari.manager.domain.usecase.AdjustmentKind
import com.example.sarisari.manager.domain.usecase.ReceiveStockUseCase
import com.example.sarisari.manager.domain.usecase.StockEntryError
import com.example.sarisari.manager.domain.usecase.StockEntryResult
import com.example.sarisari.manager.testing.FakeClock
import com.example.sarisari.manager.testing.FakeSettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.SQLiteMode

/** LEGACY SQLite mode: the native runtime loader fails when the user home path contains spaces. */
@RunWith(RobolectricTestRunner::class)
@SQLiteMode(SQLiteMode.Mode.LEGACY)
class StockMovementDaoTest {
    private lateinit var db: AppDatabase
    private val clock = FakeClock().also { it.setMillis(1_000) }
    private var productId = 0L

    @Before
    fun setUp() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        productId = db.productDao().save(
            ProductEntity(
                name = "Coke", categoryId = null, baseUnitName = "bottle", costCentavos = 0,
                sellingPriceCentavos = 6500, stockQuantity = 0, lowStockThreshold = 3,
                isArchived = false, createdAt = 0, updatedAt = 0,
            ),
            emptyList(), now = 1,
        )
    }

    @After
    fun tearDown() = db.close()

    private fun repository() = InventoryRepositoryImpl(db.stockMovementDao(), clock)

    private fun receive(method: CostingMethod? = null) = ReceiveStockUseCase(
        repository(),
        FakeSettingsRepository(method?.let { mapOf(CostingMethod.SETTING_KEY to it.storageValue) } ?: emptyMap()),
    )

    private suspend fun product() = db.productDao().getWithUnits(productId)!!.product

    private suspend fun assertCachedStockEqualsMovementSum() {
        assertEquals(db.stockMovementDao().sumQuantity(productId), product().stockQuantity)
    }

    @Test
    fun receiptsUpdateStockAndWeightedAverageCostAtomically() = runTest {
        receive()(productId, 1, 10, 4000, "first")
        assertEquals(10, product().stockQuantity)
        assertEquals(4000L, product().costCentavos)
        receive()(productId, unitFactor = 6, unitQuantity = 1, costPerUnitCentavos = 30_000, note = null)
        assertEquals(16, product().stockQuantity)
        // (10 * 4000 + 30000) / 16 = 4375
        assertEquals(4375L, product().costCentavos)
        assertCachedStockEqualsMovementSum()
        val history = db.stockMovementDao().observe(productId, 50).first()
        assertEquals(listOf(6, 10), history.map { it.movement.quantityChange })
        assertEquals(5000L, history.first().movement.unitCostCentavos)
        assertEquals(30_000L, history.first().movement.totalCostCentavos)
        assertEquals("Coke", history.first().productName)
    }

    @Test
    fun latestPurchaseCostReplacesTheCost() = runTest {
        receive(CostingMethod.LATEST_PURCHASE)(productId, 1, 10, 4000, null)
        receive(CostingMethod.LATEST_PURCHASE)(productId, 1, 5, 4500, null)
        assertEquals(4500L, product().costCentavos)
        assertEquals(15, product().stockQuantity)
    }

    @Test
    fun adjustmentsKeepTheCachedStockEqualToTheMovementSum() = runTest {
        receive()(productId, 1, 10, 4000, null)
        val adjust = AdjustStockUseCase(repository())
        adjust(productId, AdjustmentKind.DAMAGED, 3, null)
        adjust(productId, AdjustmentKind.EXPIRED, 2, "old")
        adjust(productId, AdjustmentKind.MANUAL_INCREASE, 4, "found")
        adjust(productId, AdjustmentKind.MANUAL_DECREASE, 1, "recount")
        assertEquals(8, product().stockQuantity)
        assertEquals(4000L, product().costCentavos)
        assertCachedStockEqualsMovementSum()
    }

    @Test
    fun failedAdjustmentLeavesNoMovementAndNoStockChange() = runTest {
        receive()(productId, 1, 2, 4000, null)
        try {
            repository().adjustStock(productId, StockMovementType.DAMAGED, -5, null)
            fail("Expected InsufficientStockException")
        } catch (e: InsufficientStockException) {
            assertEquals(2, e.available)
        }
        assertEquals(1, db.stockMovementDao().observe(productId, 50).first().size)
        assertEquals(2, product().stockQuantity)
        assertCachedStockEqualsMovementSum()
        assertEquals(
            StockEntryResult.Invalid(StockEntryError.INSUFFICIENT_STOCK),
            AdjustStockUseCase(repository()).invoke(productId, AdjustmentKind.DAMAGED, 5, null),
        )
    }

    @Test
    fun failureInsideTheCostCalculationRollsBackTheWholeReceipt() = runTest {
        receive()(productId, 1, 2, 4000, null)
        try {
            repository().receiveStock(productId, 5, 1000, null) { _, _ -> error("boom") }
            fail("Expected failure")
        } catch (_: IllegalStateException) {
            // expected
        }
        assertEquals(2, product().stockQuantity)
        assertEquals(1, db.stockMovementDao().observe(productId, 50).first().size)
        assertCachedStockEqualsMovementSum()
    }

    @Test
    fun productEditsDoNotChangeStock() = runTest {
        receive()(productId, 1, 7, 4000, null)
        val before = db.productDao().getWithUnits(productId)!!.product
        db.productDao().save(before.copy(name = "Coke Zero", stockQuantity = 0), emptyList(), now = 99)
        assertEquals(7, product().stockQuantity)
        assertCachedStockEqualsMovementSum()
    }
}
